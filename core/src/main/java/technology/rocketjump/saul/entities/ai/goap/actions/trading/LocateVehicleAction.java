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

public class LocateVehicleAction extends Action {

	public LocateVehicleAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		throw new NotImplementedException("LocateVehicleAction.update() not implemented");

		// TODO check vehicle belongs to parent faction and is not allocated to anyone and does not have a driver
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
	}
}
