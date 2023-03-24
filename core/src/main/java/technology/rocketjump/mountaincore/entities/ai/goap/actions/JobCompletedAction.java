package technology.rocketjump.mountaincore.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.components.creature.SkillsComponent;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.JobCompletedMessage;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

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
					new JobCompletedMessage(parent.getAssignedJob(), parent.parentEntity.getComponent(SkillsComponent.class), parent.parentEntity));
			parent.setInterrupted(false); // Kind of a hack to ignore that the above marks the goal as interrupted
		}
		completionType = CompletionType.SUCCESS;
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
