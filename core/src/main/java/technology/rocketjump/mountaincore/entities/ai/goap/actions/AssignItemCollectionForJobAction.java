package technology.rocketjump.mountaincore.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.jobs.model.Job;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.RequestHaulingAllocationMessage;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

public class AssignItemCollectionForJobAction extends Action {

	public AssignItemCollectionForJobAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		Job assignedJob = parent.getAssignedJob();
		if (assignedJob != null) {
			parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_HAULING_ALLOCATION,
					new RequestHaulingAllocationMessage(parent.parentEntity, parent.parentEntity.getLocationComponent().getWorldOrParentPosition(),
							assignedJob.getRequiredItemType(), assignedJob.getRequiredItemMaterial(), true, 1, null,
							haulingAllocation -> {
								if (haulingAllocation != null) {
									parent.setAssignedHaulingAllocation(haulingAllocation);
									completionType = CompletionType.SUCCESS;
								}
							}));
		}

		if (completionType == null) {
			completionType = CompletionType.FAILURE;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
	}
}
