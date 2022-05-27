package technology.rocketjump.saul.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.components.humanoid.ProfessionsComponent;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.JobCompletedMessage;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.SUCCESS;

public class JobCompletedAction extends Action {

	public JobCompletedAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public boolean isInterruptible() {
		return false;
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (parent.getAssignedJob() != null) {
			parent.messageDispatcher.dispatchMessage(MessageType.JOB_COMPLETED,
					new JobCompletedMessage(parent.getAssignedJob(), parent.parentEntity.getComponent(ProfessionsComponent.class), parent.parentEntity));
			parent.setInterrupted(false); // Kind of a hack to ignore that the above marks the goal as interrupted
		}
		completionType = SUCCESS;
	}

	@Override
	public boolean isApplicable(GameContext gameContext) {
		return parent.getAssignedJob() != null;
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
