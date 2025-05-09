package technology.rocketjump.mountaincore.entities.behaviour.furniture;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeWithMaterial;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.jobs.model.*;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.RequestHaulingAllocationMessage;
import technology.rocketjump.mountaincore.misc.VectorUtils;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class CollectItemFurnitureBehaviour extends FurnitureBehaviour implements Prioritisable {

	private List<ItemTypeWithMaterial> itemsToCollect = new ArrayList<>();
	private List<ItemTypeWithMaterial> inventoryAssignments = new ArrayList<>();
	private int maxNumItemStacks = 0;
	private List<Job> incomingHaulingJobs = new ArrayList<>();
	private Skill requiredProfession = null;
	private boolean allowDuplicates = false;
	private JobType haulingJobType;
	private boolean includeFromFurniture = false;
	private boolean haulingJobUninterruptible = false;

	@Override
	public FurnitureBehaviour clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		Logger.error(this.getClass().getSimpleName() + ".clone() not yet implemented");
		return new CollectItemFurnitureBehaviour();
	}

	@Override
	public void setPriority(JobPriority jobPriority) {
		super.setPriority(jobPriority);
		for (Job incomingHaulingJob : incomingHaulingJobs) {
			incomingHaulingJob.setJobPriority(jobPriority);
		}
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		super.infrequentUpdate(gameContext);
		incomingHaulingJobs.removeIf(job -> job.getJobState().equals(JobState.REMOVED));

		if (parentEntity.isOnFire()) {
			incomingHaulingJobs.forEach(job -> messageDispatcher.dispatchMessage(MessageType.JOB_REMOVED, job));
			return;
		}

		InventoryComponent inventoryComponent = parentEntity.getOrCreateComponent(InventoryComponent.class);

		// Only when no hauling jobs incoming, clear empty inventory assignments
		if (incomingHaulingJobs.isEmpty() && !inventoryAssignments.isEmpty()) {
			removeEmptyInventoryAssignments(gameContext, inventoryComponent);
		}

		// Try to create new assignments up to maxNumItemStacks
		if (inventoryAssignments.size() < maxNumItemStacks) {
			ArrayList<ItemTypeWithMaterial> potentialList = new ArrayList<>(itemsToCollect);
			Collections.shuffle(potentialList, gameContext.getRandom());

			potentialList.stream()
					.filter(itemTypeWithMaterial -> allowDuplicates || !inventoryAssignments.contains(itemTypeWithMaterial))
					.filter(this::isAvailable)
					.findFirst()
					.ifPresent(itemTypeWithMaterial -> {
						Vector2 location = parentEntity.getLocationComponent().getWorldOrParentPosition();
						FurnitureLayout.Workspace workspace = FurnitureLayout.getAnyNavigableWorkspace(parentEntity, gameContext.getAreaMap());
						if (workspace != null) {
							location = VectorUtils.toVector(workspace.getAccessedFrom());
						}

						messageDispatcher.dispatchMessage(MessageType.REQUEST_HAULING_ALLOCATION, new RequestHaulingAllocationMessage(
								parentEntity, location, itemTypeWithMaterial.getItemType(), itemTypeWithMaterial.getMaterial(),
								includeFromFurniture, null, null, allocation -> {
							if (allocation != null) {
								createHaulingJobForAllocation(itemTypeWithMaterial, allocation);
							}
						}));
					});

		}
	}

	private boolean isAvailable(ItemTypeWithMaterial itemTypeWithMaterial) {
		AtomicInteger availableAmount = new AtomicInteger();
		messageDispatcher.dispatchMessage(MessageType.CHECK_ITEM_AVAILABILITY, new MessageType.CheckItemAvailabilityMessage(itemTypeWithMaterial, availableAmount::set));
		return availableAmount.get() > 0;
	}

	public boolean canAccept(Entity itemEntity) {
		if (inventoryAssignments.size() < maxNumItemStacks) {
			ItemEntityAttributes attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();
			ItemTypeWithMaterial matching = getMatch(attributes);
			if (matching != null) {
				if (allowDuplicates || !inventoryAssignments.contains(matching)) {
					return true;
				}
			}
		}
		return false;
	}

	public void createHaulingJobForAllocation(ItemTypeWithMaterial potentialItemTypeWithMaterial, HaulingAllocation allocation) {
		inventoryAssignments.add(potentialItemTypeWithMaterial);
		// Create hauling job to haul assignment into inventory

		Job haulingJob = new Job(haulingJobType);
		haulingJob.setJobPriority(priority);
		haulingJob.setTargetId(allocation.getHauledEntityId());
		haulingJob.setHaulingAllocation(allocation);
		haulingJob.setJobLocation(allocation.getSourcePosition());
		haulingJob.setUninterruptible(haulingJobUninterruptible);

		if (allocation.getSourcePositionType() == HaulingAllocation.AllocationPositionType.FURNITURE) {
			Entity containerEntity = gameContext.getEntities().get(allocation.getSourceContainerId());
			if (containerEntity != null && containerEntity.getPhysicalEntityComponent().getAttributes() instanceof FurnitureEntityAttributes) {
				FurnitureLayout.Workspace workspace = FurnitureLayout.getAnyNavigableWorkspace(containerEntity, gameContext.getAreaMap());
				if (workspace != null) {
					haulingJob.setJobLocation(workspace.getAccessedFrom());
				}
			}
		}

		if (requiredProfession != null) {
			haulingJob.setRequiredProfession(requiredProfession);
		}

		incomingHaulingJobs.add(haulingJob);
		messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, haulingJob);
	}

	public ItemTypeWithMaterial getMatch(ItemEntityAttributes attributes) {
		for (ItemTypeWithMaterial itemTypeWithMaterial : itemsToCollect) {
			if (attributes.getItemType().equals(itemTypeWithMaterial.getItemType()) &&
					attributes.getMaterial(attributes.getItemType().getPrimaryMaterialType()).equals(itemTypeWithMaterial.getMaterial())) {
				return itemTypeWithMaterial;
			}
		}
		return null;
	}

	public void setItemsToCollect(List<ItemTypeWithMaterial> itemsToCollect) {
		this.itemsToCollect = itemsToCollect;
	}

	public void setMaxNumItemStacks(int maxNumItemStacks) {
		this.maxNumItemStacks = maxNumItemStacks;
	}

	private void removeEmptyInventoryAssignments(GameContext gameContext, InventoryComponent inventoryComponent) {
		List<ItemTypeWithMaterial> refreshedInventoryAssignments = new ArrayList<>();

		for (InventoryComponent.InventoryEntry inventoryEntry : inventoryComponent.getInventoryEntries()) {
			ItemTypeWithMaterial match = getMatch((ItemEntityAttributes) inventoryEntry.entity.getPhysicalEntityComponent().getAttributes());
			if (match != null) {
				refreshedInventoryAssignments.add(match);
			}
		}
		this.inventoryAssignments = refreshedInventoryAssignments;
	}

	public void setRequiredProfession(Skill requiredProfession) {
		this.requiredProfession = requiredProfession;
	}

	public void setAllowDuplicates(boolean allowDuplicates) {
		this.allowDuplicates = allowDuplicates;
	}

	public boolean getAllowDuplicates() {
		return allowDuplicates;
	}

	public List<ItemTypeWithMaterial> getItemsToCollect() {
		return itemsToCollect;
	}

	public void setHaulingJobType(JobType haulingJobType) {
		this.haulingJobType = haulingJobType;
	}

	public void setIncludeFromFurniture(boolean includeFromFurniture) {
		this.includeFromFurniture = includeFromFurniture;
	}

	public void setHaulingJobUninterruptible(boolean haulingJobUninterruptible) {
		this.haulingJobUninterruptible = haulingJobUninterruptible;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);

		JSONArray itemsToCollectJson = new JSONArray();
		for (ItemTypeWithMaterial itemTypeWithMaterial : itemsToCollect) {
			JSONObject itemTypeWithMaterialJson = new JSONObject(true);
			itemTypeWithMaterial.writeTo(itemTypeWithMaterialJson, savedGameStateHolder);
			itemsToCollectJson.add(itemTypeWithMaterialJson);
		}
		asJson.put("itemsToCollect", itemsToCollectJson);

		JSONArray inventoryAssignmentsJson = new JSONArray();
		for (ItemTypeWithMaterial inventoryAssignment : inventoryAssignments) {
			JSONObject inventoryAssignmentJson = new JSONObject(true);
			inventoryAssignment.writeTo(inventoryAssignmentJson, savedGameStateHolder);
			inventoryAssignmentsJson.add(inventoryAssignmentJson);
		}
		asJson.put("inventoryAssignments", inventoryAssignmentsJson);

		asJson.put("maxNumItemStacks", maxNumItemStacks);

		if (!incomingHaulingJobs.isEmpty()) {
			JSONArray incomingHaulingJobsJson = new JSONArray();
			for (Job haulingJob : incomingHaulingJobs) {
				haulingJob.writeTo(savedGameStateHolder);
				incomingHaulingJobsJson.add(haulingJob.getJobId());
			}
			asJson.put("jobs", incomingHaulingJobsJson);
		}

		if (requiredProfession != null) {
			asJson.put("requiredProfession", requiredProfession.getName());
		}
		if (allowDuplicates) {
			asJson.put("allowDuplicates", true);
		}

		asJson.put("haulingJobType", haulingJobType.getName());
		asJson.put("includeFromFurniture", includeFromFurniture);

		if (haulingJobUninterruptible) {
			asJson.put("haulingJobUninterruptible", true);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		JSONArray itemsToCollectJson = asJson.getJSONArray("itemsToCollect");
		for (int cursor = 0; cursor < itemsToCollectJson.size(); cursor++) {
			ItemTypeWithMaterial itemTypeWithMaterial = new ItemTypeWithMaterial();
			itemTypeWithMaterial.readFrom(itemsToCollectJson.getJSONObject(cursor), savedGameStateHolder, relatedStores);
			this.itemsToCollect.add(itemTypeWithMaterial);
		}

		JSONArray inventoryAssignmentsJson = asJson.getJSONArray("inventoryAssignments");
		for (int cursor = 0; cursor < inventoryAssignmentsJson.size(); cursor++) {
			ItemTypeWithMaterial itemTypeWithMaterial = new ItemTypeWithMaterial();
			itemTypeWithMaterial.readFrom(inventoryAssignmentsJson.getJSONObject(cursor), savedGameStateHolder, relatedStores);
			this.inventoryAssignments.add(itemTypeWithMaterial);
		}

		this.maxNumItemStacks = asJson.getIntValue("maxNumItemStacks");

		JSONArray incomingHaulingJobsJson = asJson.getJSONArray("jobs");
		if (incomingHaulingJobsJson != null) {
			for (int cursor = 0; cursor < incomingHaulingJobsJson.size(); cursor++) {
				long jobId = incomingHaulingJobsJson.getLongValue(cursor);
				Job job = savedGameStateHolder.jobs.get(jobId);
				if (job == null) {
					throw new InvalidSaveException("Could not find job by ID " + jobId);
				} else {
					incomingHaulingJobs.add(job);
				}
			}
		}

		String requiredProfessionName = asJson.getString("requiredProfession");
		if (requiredProfessionName != null) {
			this.requiredProfession = relatedStores.skillDictionary.getByName(requiredProfessionName);
			if (this.requiredProfession == null) {
				throw new InvalidSaveException("Could not find profession by name " + requiredProfessionName);
			}
		}

		this.allowDuplicates = asJson.getBooleanValue("allowDuplicates");

		this.haulingJobType = relatedStores.jobTypeDictionary.getByName(asJson.getString("haulingJobType"));
		if (haulingJobType == null) {
			throw new InvalidSaveException("Could not find job type with name " + asJson.getString("haulingJobType"));
		}
		this.includeFromFurniture = asJson.getBooleanValue("includeFromFurniture");
		this.haulingJobUninterruptible = asJson.getBooleanValue("haulingJobUninterruptible");
	}

}
