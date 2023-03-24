package technology.rocketjump.mountaincore.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.mountaincore.entities.ai.goap.actions.Action.CompletionType.SUCCESS;

public class PauseAction extends Action {

	private static final double MAX_HOURS_TO_PAUSE = 0.4;

	public PauseAction(AssignedGoal parent) {
		super(parent);
	}

	private double elapsedTime;

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		elapsedTime += deltaTime;
		if (elapsedTime > gameContext.getGameClock().gameHoursToRealTimeSeconds(MAX_HOURS_TO_PAUSE)) {
			completionType = SUCCESS;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (elapsedTime > 0) {
			asJson.put("elapsedTime", elapsedTime);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.elapsedTime = asJson.getDoubleValue("elapsedTime");
	}
}
