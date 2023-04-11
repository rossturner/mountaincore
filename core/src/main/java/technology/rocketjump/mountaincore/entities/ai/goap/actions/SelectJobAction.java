package technology.rocketjump.mountaincore.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.entities.ai.memory.Memory;
import technology.rocketjump.mountaincore.entities.ai.memory.MemoryType;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.components.creature.MemoryComponent;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.entities.planning.JobAssignmentCallback;
import technology.rocketjump.mountaincore.environment.GameClock;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.jobs.model.Job;
import technology.rocketjump.mountaincore.jobs.model.JobState;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.JobRequestMessage;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import java.util.List;

public class SelectJobAction extends Action implements JobAssignmentCallback, InitialisableAction {

	private static final float MAX_TIME_TO_WAIT_SECONDS = 5f;

	private JobRequestMessage jobRequest;
	private float timeWaitingForJob = 0f;

	public SelectJobAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void init(GameContext gameContext) {
		if (jobRequest != null) {
			jobRequest.setRequestingEntity(parent.parentEntity);
		}
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (jobRequest == null) {
			jobRequest = new JobRequestMessage(parent.parentEntity, gameContext.getGameClock(), this);
			parent.messageDispatcher.dispatchMessage(MessageType.JOB_REQUESTED, jobRequest);
		}

		timeWaitingForJob += deltaTime;
		if (timeWaitingForJob > MAX_TIME_TO_WAIT_SECONDS) {
			completionType = CompletionType.FAILURE;
			jobRequest.setCancelled(true);
			Logger.info("Timed out while waiting for job callback");
		}
	}

	@Override
	public CompletionType isCompleted(GameContext gameContext) throws SwitchGoalException {
		if (parent.getAssignedJob() != null) {
			if (parent.getAssignedJob().getType().getSwitchToSpecialGoal() != null) {
				parent.setAssignedHaulingAllocation(parent.getAssignedJob().getHaulingAllocation());
				parent.setLiquidAllocation(parent.getAssignedJob().getLiquidAllocation());
				throw new SwitchGoalException(parent.getAssignedJob().getType().getSwitchToSpecialGoal());
			}
		}
		return completionType;
	}

	@Override
	public void jobCallback(List<Job> potentialJobs, GameContext gameContext) {
		if (CompletionType.FAILURE.equals(completionType)) {
			return; // Don't accept any jobs when already failed
		}
		Job selectedJob = null;
		for (Job potentialJob : potentialJobs) {
			if (potentialJob.getJobState().equals(JobState.ASSIGNABLE) && potentialJob.getAssignedToEntityId() == null) {

				if (potentialJob.getRequiredItemType() != null && !jobUsesWorkstationTool(potentialJob)) {
					if (!haveInventoryItem(potentialJob.getRequiredItemType(), potentialJob.getRequiredItemMaterial(), gameContext.getGameClock())) {
						Memory itemRequiredMemory = new Memory(MemoryType.LACKING_REQUIRED_ITEM, gameContext.getGameClock());
						itemRequiredMemory.setRelatedItemType(potentialJob.getRequiredItemType());
						itemRequiredMemory.setRelatedMaterial(potentialJob.getRequiredItemMaterial()); // Might be null
						parent.parentEntity.getComponent(MemoryComponent.class).addShortTerm(itemRequiredMemory, gameContext.getGameClock());
						continue;
					}
				}

				selectedJob = potentialJob;
				selectedJob.setAssignedToEntityId(parent.parentEntity.getId());
				parent.setAssignedJob(selectedJob);
				parent.messageDispatcher.dispatchMessage(MessageType.JOB_ASSIGNMENT_ACCEPTED, selectedJob);
				completionType = CompletionType.SUCCESS;
				break;
			}
		}
		if (selectedJob == null) {
			// No jobs found
			completionType = CompletionType.FAILURE;
		}
	}

	private boolean haveInventoryItem(ItemType itemTypeRequired, GameMaterial requiredItemMaterial, GameClock gameClock) {
		InventoryComponent inventoryComponent = parent.parentEntity.getComponent(InventoryComponent.class);
		if (requiredItemMaterial != null) {
			return inventoryComponent.findByItemTypeAndMaterial(itemTypeRequired, requiredItemMaterial, gameClock) != null;
		} else {
			return inventoryComponent.findByItemType(itemTypeRequired, gameClock) != null;
		}
	}

	private boolean jobUsesWorkstationTool(Job job) {
		if (job.getCraftingRecipe() != null) {
			return job.getCraftingRecipe().getCraftingType().isUsesWorkstationTool();
		} else {
			return job.getType().isUsesWorkstationTool();
		}
	}

	@Override
	public String getDescriptionOverrideI18nKey() {
		return "ACTION.SELECT_JOB.DESCRIPTION";
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (jobRequest != null) {
			jobRequest.writeTo(savedGameStateHolder);
			asJson.put("jobRequest", jobRequest.getRequestId());
		}

		if (timeWaitingForJob > 0f) {
			asJson.put("timeWaitingForJob", timeWaitingForJob);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		Long jobRequestId = asJson.getLong("jobRequest");
		if (jobRequestId != null) {
			this.jobRequest = savedGameStateHolder.jobRequests.get(jobRequestId);
			if (this.jobRequest == null) {
				throw new InvalidSaveException("Could not find job request by ID " + jobRequestId);
			}
			this.jobRequest.setCallback(this);
			// jobRequest requesterId set in init()
		}

		this.timeWaitingForJob = asJson.getFloatValue("timeWaitingForJob");
	}
}
