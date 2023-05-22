package technology.rocketjump.mountaincore.entities.behaviour.furniture;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.components.ItemAllocationComponent;
import technology.rocketjump.mountaincore.entities.components.furniture.DecorationInventoryComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.jobs.model.*;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.RequestCorpseMessage;
import technology.rocketjump.mountaincore.messaging.types.RequestHaulingMessage;
import technology.rocketjump.mountaincore.misc.Destructible;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;
import technology.rocketjump.mountaincore.rooms.HaulingAllocationBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ButcheryStationBehaviour extends FurnitureBehaviour implements Prioritisable, Destructible {

	private List<Job> haulingJobs = new ArrayList<>();
	private Job butcheryJob;

	private Skill requiredProfession = null;
	private JobType haulingJobType;
	private JobType butcheryJobType;

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		for (Job haulingJob : haulingJobs) {
			HaulingAllocation haulingAllocation = haulingJob.getHaulingAllocation();
			messageDispatcher.dispatchMessage(MessageType.HAULING_ALLOCATION_CANCELLED, haulingAllocation);

			messageDispatcher.dispatchMessage(MessageType.JOB_REMOVED, haulingJob);
		}

		if (butcheryJob != null) {
			messageDispatcher.dispatchMessage(MessageType.JOB_REMOVED, butcheryJob);
		}


	}

	@Override
	public FurnitureBehaviour clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		Logger.error(this.getClass().getSimpleName() + ".clone() not yet implemented");
		return new ButcheryStationBehaviour();
	}

	@Override
	public void setPriority(JobPriority jobPriority) {
		super.setPriority(jobPriority);
		for (Job incomingHaulingJob : haulingJobs) {
			incomingHaulingJob.setJobPriority(jobPriority);
		}
		if (butcheryJob != null) {
			butcheryJob.setJobPriority(jobPriority);
		}
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		super.infrequentUpdate(gameContext);
		haulingJobs.removeIf(job -> job.getJobState().equals(JobState.REMOVED));
		if (butcheryJob != null && butcheryJob.getJobState().equals(JobState.REMOVED)) {
			butcheryJob = null;
		}

		if (parentEntity.isOnFire()) {
			haulingJobs.forEach(job -> messageDispatcher.dispatchMessage(MessageType.JOB_REMOVED, job));
			if (butcheryJob != null) {
				messageDispatcher.dispatchMessage(MessageType.JOB_REMOVED, butcheryJob);
			}
			return;
		}

		InventoryComponent inventoryComponent = parentEntity.getOrCreateComponent(InventoryComponent.class);

		if (haulingJobs.isEmpty() && inventoryComponent.isEmpty()) {
			// Try to create new incoming hauling assignment
			messageDispatcher.dispatchMessage(MessageType.FIND_BUTCHERABLE_UNALLOCATED_CORPSE, new RequestCorpseMessage(
					parentEntity, parentEntity.getLocationComponent().getWorldOrParentPosition(), entity -> {
				if (entity != null) {
					createIncomingHaulingJob(entity);
				}
			}));
		}

		// empty out item-type entities
		for (InventoryComponent.InventoryEntry inventoryEntry : inventoryComponent.getInventoryEntries()) {
			if (inventoryEntry.entity.getType().equals(EntityType.ITEM)) {
				ItemAllocationComponent itemAllocationComponent = inventoryEntry.entity.getComponent(ItemAllocationComponent.class);
				if (itemAllocationComponent.getNumUnallocated() > 0) {
					RequestHaulingMessage requestHaulingMessage = new RequestHaulingMessage(
							inventoryEntry.entity, parentEntity, true, priority, job -> haulingJobs.add(job)
					);
					requestHaulingMessage.setSpecificProfessionRequired(requiredProfession);
					messageDispatcher.dispatchMessage(MessageType.REQUEST_ENTITY_HAULING, requestHaulingMessage);
				}
			}
		}

		if (!inventoryComponent.isEmpty() && butcheryJob == null && haulingJobs.isEmpty()) {
			createButcheryJob(gameContext);
		}

		if (butcheryJob != null) {
			boolean butcheryJobIsNavigable = gameContext.getAreaMap().getTile(butcheryJob.getJobLocation()).isNavigable(null);
			if (!butcheryJobIsNavigable) {
				FurnitureLayout.Workspace navigableWorkspace = FurnitureLayout.getAnyNavigableWorkspace(parentEntity, gameContext.getAreaMap());
				if (navigableWorkspace != null) {
					butcheryJob.setJobLocation(navigableWorkspace.getAccessedFrom());
					butcheryJob.setSecondaryLocation(navigableWorkspace.getLocation());
				}
			}
		}
	}

	private void createIncomingHaulingJob(Entity corpseEntity) {
		HaulingAllocation haulingAllocation = HaulingAllocationBuilder.createWithItemAllocation(1, corpseEntity, parentEntity)
						.toEntity(parentEntity);

		Job haulingJob = new Job(haulingJobType);
		haulingJob.setJobPriority(priority);
		haulingJob.setTargetId(haulingAllocation.getHauledEntityId());
		haulingJob.setHaulingAllocation(haulingAllocation);
		haulingJob.setJobLocation(haulingAllocation.getSourcePosition());

		if (requiredProfession != null) {
			haulingJob.setRequiredProfession(requiredProfession);
		}

		haulingJobs.add(haulingJob);
		messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, haulingJob);
	}

	private void createButcheryJob(GameContext gameContext) {
		FurnitureLayout.Workspace navigableWorkspace = FurnitureLayout.getAnyNavigableWorkspace(parentEntity, gameContext.getAreaMap());
		Collection<Entity> decorationEntities = parentEntity.getComponent(DecorationInventoryComponent.class).getDecorationEntities();
		if (navigableWorkspace == null) {
			Logger.warn("Could not access workstation at " + parentEntity.getLocationComponent().getWorldPosition());
			return;
		}

		butcheryJob = new Job(butcheryJobType);
		butcheryJob.setJobPriority(priority);
		butcheryJob.setTargetId(parentEntity.getId());
		butcheryJob.setJobLocation(navigableWorkspace.getAccessedFrom());
		butcheryJob.setSecondaryLocation(navigableWorkspace.getLocation());
		if (!decorationEntities.isEmpty()) {
			butcheryJob.setRequiredItemType(((ItemEntityAttributes) decorationEntities.iterator().next().getPhysicalEntityComponent().getAttributes()).getItemType());
		}

		messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, butcheryJob);
	}


	public void setRequiredProfession(Skill requiredProfession) {
		this.requiredProfession = requiredProfession;
	}

	public void setHaulingJobType(JobType haulingJobType) {
		this.haulingJobType = haulingJobType;
	}

	public void setButcheryJobType(JobType butcheryJobType) {
		this.butcheryJobType = butcheryJobType;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);

		if (!haulingJobs.isEmpty()) {
			JSONArray incomingHaulingJobsJson = new JSONArray();
			for (Job haulingJob : haulingJobs) {
				haulingJob.writeTo(savedGameStateHolder);
				incomingHaulingJobsJson.add(haulingJob.getJobId());
			}
			asJson.put("jobs", incomingHaulingJobsJson);
		}

		if (butcheryJob != null) {
			butcheryJob.writeTo(savedGameStateHolder);
			asJson.put("butcheryJob", butcheryJob.getJobId());
		}

		if (requiredProfession != null) {
			asJson.put("requiredProfession", requiredProfession.getName());
		}
		asJson.put("haulingJobType", haulingJobType.getName());
		asJson.put("butcheryJobType", butcheryJobType.getName());
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		JSONArray incomingHaulingJobsJson = asJson.getJSONArray("jobs");
		if (incomingHaulingJobsJson != null) {
			for (int cursor = 0; cursor < incomingHaulingJobsJson.size(); cursor++) {
				long jobId = incomingHaulingJobsJson.getLongValue(cursor);
				Job job = savedGameStateHolder.jobs.get(jobId);
				if (job == null) {
					throw new InvalidSaveException("Could not find job by ID " + jobId);
				} else {
					haulingJobs.add(job);
				}
			}
		}
		Long butcheryJobId = asJson.getLong("butcheryJob");
		if (butcheryJobId != null) {
			this.butcheryJob = savedGameStateHolder.jobs.get(butcheryJobId);
			if (butcheryJob == null) {
				throw new InvalidSaveException("Could not find butchery job by ID " + butcheryJobId);
			}
		}

		String requiredProfessionName = asJson.getString("requiredProfession");
		if (requiredProfessionName != null) {
			this.requiredProfession = relatedStores.skillDictionary.getByName(requiredProfessionName);
			if (this.requiredProfession == null) {
				throw new InvalidSaveException("Could not find profession by name " + requiredProfessionName);
			}
		}

		this.haulingJobType = relatedStores.jobTypeDictionary.getByName(asJson.getString("haulingJobType"));
		if (haulingJobType == null) {
			throw new InvalidSaveException("Could not find job type with name " + asJson.getString("haulingJobType"));
		}

		this.butcheryJobType = relatedStores.jobTypeDictionary.getByName(asJson.getString("butcheryJobType"));
		if (butcheryJobType == null) {
			throw new InvalidSaveException("Could not find job type with name " + asJson.getString("butcheryJobType"));
		}
	}

}
