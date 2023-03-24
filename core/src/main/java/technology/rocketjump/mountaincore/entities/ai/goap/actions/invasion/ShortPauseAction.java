package technology.rocketjump.mountaincore.entities.ai.goap.actions.invasion;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.combat.CombatTracker;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

public class ShortPauseAction extends Action {

	private static final float TIME_TO_PAUSE = 1.3f * CombatTracker.COMBAT_ROUND_DURATION;

	private float timeElapsed;

	public ShortPauseAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		timeElapsed += deltaTime;

		if (timeElapsed > TIME_TO_PAUSE) {
			completionType = CompletionType.SUCCESS;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("elapsed", timeElapsed);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.timeElapsed = asJson.getFloatValue("elapsed");
	}
}
