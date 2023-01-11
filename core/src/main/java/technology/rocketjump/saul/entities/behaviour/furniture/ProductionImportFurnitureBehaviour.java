package technology.rocketjump.saul.entities.behaviour.furniture;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.components.ItemAllocation;
import technology.rocketjump.saul.entities.components.ItemAllocationComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.model.Job;
import technology.rocketjump.saul.jobs.model.JobPriority;
import technology.rocketjump.saul.jobs.model.JobState;
import technology.rocketjump.saul.jobs.model.JobType;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.RequestHaulingAllocationMessage;
import technology.rocketjump.saul.messaging.types.RequestHaulingMessage;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.rendering.utils.HexColors;
import technology.rocketjump.saul.rooms.HaulingAllocation;

import java.util.ArrayList;
import java.util.List;

public class ProductionImportFurnitureBehaviour extends FurnitureBehaviour implements Prioritisable, DisplayGhostItemWhenInventoryEmpty {

	private int maxNumItemStacks = 0;
	private ItemType selectedItemType;
	private GameMaterial selectedMaterial; // null == ANY

	private final List<Job> incomingHaulingJobs = new ArrayList<>();
	private JobType haulingJobType;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		super.init(parentEntity, messageDispatcher, gameContext);
		parentEntity.getOrCreateComponent(InventoryComponent.class).setAddAsAllocationPurpose(ItemAllocation.Purpose.PRODUCTION_IMPORT);
	}

	@Override
	public FurnitureBehaviour clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		ProductionImportFurnitureBehaviour cloned = new ProductionImportFurnitureBehaviour();
		cloned.maxNumItemStacks = this.maxNumItemStacks;
		cloned.selectedItemType = this.selectedItemType;
		cloned.selectedMaterial = this.selectedMaterial;

		cloned.incomingHaulingJobs.addAll(this.incomingHaulingJobs);
		cloned.haulingJobType = this.haulingJobType;

		return cloned;
	}

	@Override
	public void setPriority(JobPriority jobPriority) {
		super.setPriority(jobPriority);
		for (Job incomingHaulingJob : incomingHaulingJobs) {
			incomingHaulingJob.setJobPriority(jobPriority);
		}
	}

	public void setMaxNumItemStacks(int maxNumItemStacks) {
		this.maxNumItemStacks = maxNumItemStacks;
	}

	public void setSelectedItemType(ItemType selectedItemType) {
		this.selectedItemType = selectedItemType;
		cancelIncomingHaulingJobs();
	}

	public void setSelectedMaterial(GameMaterial selectedMaterial) {
		this.selectedMaterial = selectedMaterial;
		if (selectedMaterial != null) {
			cancelIncomingHaulingJobs();
		}
	}

	@Override
	public ItemType getSelectedItemType() {
		return selectedItemType;
	}

	@Override
	public Color getOverrideColor() {
		if (incomingHaulingJobs.isEmpty()) {
			return HexColors.GHOST_NEGATIVE_COLOR_MORE_OPAQUE;
		} else {
			return HexColors.GHOST_PLAIN_COLOR;
		}
	}

	public GameMaterial getSelectedMaterial() {
		return selectedMaterial;
	}

	public void setHaulingJobType(JobType haulingJobType) {
		this.haulingJobType = haulingJobType;
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		super.infrequentUpdate(gameContext);
		incomingHaulingJobs.removeIf(job -> job.getJobState().equals(JobState.REMOVED));

		if (parentEntity.isOnFire()) {
			cancelIncomingHaulingJobs();
			return;
		}

		InventoryComponent inventoryComponent = parentEntity.getOrCreateComponent(InventoryComponent.class);

		List<Entity> unwantedInventoryItems = getInventoryItemsNotMatchingSelection(inventoryComponent);
		if (unwantedInventoryItems.size() > 0) {
			for (Entity inventoryItem : unwantedInventoryItems) {
				ItemAllocationComponent allocationComponent = inventoryItem.getComponent(ItemAllocationComponent.class);
				allocationComponent.cancelAll(ItemAllocation.Purpose.PRODUCTION_IMPORT);

				if (allocationComponent.getNumUnallocated() > 0) {
					messageDispatcher.dispatchMessage(MessageType.REQUEST_ENTITY_HAULING, new RequestHaulingMessage(
							inventoryItem, parentEntity, true, priority, null)
					);
				}
			}
		} else if (selectedItemType != null) {
			List<Entity> inventoryItems = inventoryComponent.getInventoryEntries().stream().map(e -> e.entity).toList();

			for (int stackCursor = 0; stackCursor < maxNumItemStacks; stackCursor++) {
				Entity stackInInventory = null;
				if (inventoryItems.size() > stackCursor) {
					stackInInventory = inventoryItems.get(stackCursor);
				}

				int amountToRequest = 0;
				// reduce potential stack size by incoming hauling
				GameMaterial materialToRequest = selectedMaterial;
				for (Job incomingHaulingJob : incomingHaulingJobs) {
					amountToRequest -= incomingHaulingJob.getHaulingAllocation().getItemAllocation().getAllocationAmount();
					if (selectedMaterial == null) {
						Entity targetItem = gameContext.getEntity(incomingHaulingJob.getHaulingAllocation().getItemAllocation().getTargetItemEntityId());
						ItemEntityAttributes attributes = (ItemEntityAttributes) targetItem.getPhysicalEntityComponent().getAttributes();
						materialToRequest = attributes.getPrimaryMaterial();
					}
				}
				if (stackInInventory == null) {
					// request remaining amount
					amountToRequest += selectedItemType.getMaxStackSize();
				} else {
					ItemEntityAttributes attributes = (ItemEntityAttributes) stackInInventory.getPhysicalEntityComponent().getAttributes();
					if (attributes.getQuantity() < attributes.getItemType().getMaxStackSize() && incomingHaulingJobs.isEmpty()) {
						// try to request remainder of stack to come in
						amountToRequest += attributes.getItemType().getMaxStackSize() - attributes.getQuantity();
						materialToRequest = attributes.getPrimaryMaterial();
					}
				}

				if (amountToRequest > 0) {
					messageDispatcher.dispatchMessage(MessageType.REQUEST_HAULING_ALLOCATION, new RequestHaulingAllocationMessage(
							parentEntity, parentEntity.getLocationComponent().getWorldOrParentPosition(), selectedItemType, materialToRequest,
							true, amountToRequest, null, this::createHaulingJobForAllocation));
				}
			}
		}

	}

	private List<Entity> getInventoryItemsNotMatchingSelection(InventoryComponent inventoryComponent) {
		if (selectedItemType == null) {
			return inventoryComponent.getInventoryEntries().stream().map(e -> e.entity).toList();
		}
		return inventoryComponent.getInventoryEntries().stream()
				.map(e -> e.entity)
				.filter(e -> e.getType().equals(EntityType.ITEM))
				.filter(e -> !matchesCurrentSelection((ItemEntityAttributes) e.getPhysicalEntityComponent().getAttributes()))
				.toList();
	}

	private boolean matchesCurrentSelection(ItemEntityAttributes attributes) {
		return attributes.getItemType().equals(selectedItemType) && (selectedMaterial == null || attributes.getPrimaryMaterial().equals(selectedMaterial));
	}

	private void createHaulingJobForAllocation(HaulingAllocation allocation) {
		if (allocation != null) {
			Job haulingJob = new Job(haulingJobType);
			haulingJob.setJobPriority(priority);
			haulingJob.setTargetId(allocation.getHauledEntityId());
			haulingJob.setHaulingAllocation(allocation);
			haulingJob.setJobLocation(allocation.getSourcePosition());

			incomingHaulingJobs.add(haulingJob);
			messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, haulingJob);
		}
	}
	private void cancelIncomingHaulingJobs() {
		incomingHaulingJobs.forEach(job -> messageDispatcher.dispatchMessage(MessageType.JOB_REMOVED, job));
	}

	public Entity getParentEntity() {
		return parentEntity;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);

		asJson.put("maxNumItemStacks", maxNumItemStacks);
		asJson.put("selectedItemType", selectedItemType != null ? selectedItemType.getItemTypeName() : null);
		asJson.put("selectedMaterial", selectedMaterial != null ? selectedMaterial.getMaterialName() : null);

		if (!incomingHaulingJobs.isEmpty()) {
			JSONArray incomingHaulingJobsJson = new JSONArray();
			for (Job haulingJob : incomingHaulingJobs) {
				haulingJob.writeTo(savedGameStateHolder);
				incomingHaulingJobsJson.add(haulingJob.getJobId());
			}
			asJson.put("jobs", incomingHaulingJobsJson);
		}

		asJson.put("haulingJobType", haulingJobType.getName());
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		this.maxNumItemStacks = asJson.getIntValue("maxNumItemStacks");
		String itemTypeName = asJson.getString("selectedItemType");
		if (itemTypeName != null) {
			selectedItemType = relatedStores.itemTypeDictionary.getByName(itemTypeName);
			if (selectedItemType == null) {
				throw new InvalidSaveException("Could not find item type " + itemTypeName);
			}
		}
		String materialName = asJson.getString("selectedMaterial");
		if (materialName != null) {
			selectedMaterial = relatedStores.gameMaterialDictionary.getByName(materialName);
			if (selectedMaterial == null) {
				throw new InvalidSaveException("Could not find material " + materialName);
			}
		}

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

		this.haulingJobType = relatedStores.jobTypeDictionary.getByName(asJson.getString("haulingJobType"));
		if (haulingJobType == null) {
			throw new InvalidSaveException("Could not find job type with name " + asJson.getString("haulingJobType"));
		}
	}
}
