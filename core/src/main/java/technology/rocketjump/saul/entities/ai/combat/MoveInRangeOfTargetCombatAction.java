package technology.rocketjump.saul.entities.ai.combat;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

public class MoveInRangeOfTargetCombatAction extends CombatAction {

	public MoveInRangeOfTargetCombatAction(Entity parentEntity) {
		super(parentEntity);
	}

	@Override
	public boolean completesInOneRound() {
		return false;
	}

	@Override
	public void onRoundCompletion() {
		super.onRoundCompletion();

		// TODO update pathfinding in case target has moved position
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {

	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {

	}
}
