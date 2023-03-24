package technology.rocketjump.mountaincore.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

public class UnassignJobAction extends Action {
	public UnassignJobAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public boolean isInterruptible() {
		return false;
	}

	@Override
	public boolean isApplicable(GameContext gameContext) {
		return parent.getAssignedJob() != null;
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (parent.getAssignedJob() == null) {
			completionType = CompletionType.FAILURE;
		} else {
			parent.messageDispatcher.dispatchMessage(MessageType.JOB_ASSIGNMENT_CANCELLED, parent.getAssignedJob());
			parent.setAssignedJob(null);
			completionType = CompletionType.SUCCESS;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		// No state to write
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		// No state to read
	}

}
