package technology.rocketjump.saul.entities.ai.goap.actions.trading;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.saul.entities.ai.goap.actions.Action;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.saul.entities.behaviour.creature.TraderCreatureGroup;
import technology.rocketjump.saul.entities.behaviour.creature.TraderGroupStage;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

public class ProgressTraderStageToArrivedAction extends Action {

	public ProgressTraderStageToArrivedAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		if (parent.parentEntity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour &&
			creatureBehaviour.getCreatureGroup() instanceof TraderCreatureGroup traderCreatureGroup) {
			if (traderCreatureGroup.getStage().equals(TraderGroupStage.MOVING_TO_TRADE_DEPOT)) {
				traderCreatureGroup.progressToNextStage();
			}
		}

		completionType = CompletionType.SUCCESS;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {

	}
}
