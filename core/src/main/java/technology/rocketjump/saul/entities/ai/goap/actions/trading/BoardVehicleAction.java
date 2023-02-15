package technology.rocketjump.saul.entities.ai.goap.actions.trading;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.lang3.NotImplementedException;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.saul.entities.ai.goap.actions.Action;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

public class BoardVehicleAction extends Action {

	public BoardVehicleAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		throw new NotImplementedException("BoardVehicleAction.update() not implemented");
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
	}
}
