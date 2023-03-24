package technology.rocketjump.mountaincore.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.GoalSelector;
import technology.rocketjump.mountaincore.entities.ai.goap.QueuedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.condition.GoalSelectionCondition;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

public class RequeueThisGoalAction extends Action {
	public RequeueThisGoalAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public boolean isInterruptible() {
		return false;
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		CreatureBehaviour parentBehaviour = (CreatureBehaviour) parent.parentEntity.getBehaviourComponent();


		for (GoalSelector selector : parent.goal.getSelectors()) {
			boolean allConditionsApply = true;
			for (GoalSelectionCondition condition : selector.conditions) {
				if (!condition.apply(parent.parentEntity, gameContext)) {
					allConditionsApply = false;
					break;
				}
			}
			if (allConditionsApply) {
				parentBehaviour.getGoalQueue().add(new QueuedGoal(parent.goal, selector.scheduleCategory, selector.priority, gameContext.getGameClock()));
				completionType = CompletionType.SUCCESS;
				return;
			}
		}

		completionType = CompletionType.FAILURE;
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
