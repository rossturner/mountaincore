package technology.rocketjump.saul.entities.ai.goap.actions.crafting;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.saul.entities.ai.goap.actions.Action;
import technology.rocketjump.saul.entities.ai.goap.actions.PlaceEntityAction;
import technology.rocketjump.saul.entities.model.physical.creature.HaulingComponent;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.rooms.HaulingAllocation;

import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;

public class DropHauledItemAction extends Action {

	public DropHauledItemAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		HaulingAllocation haulingAllocation = parent.getAssignedHaulingAllocation();
		haulingAllocation.setTargetId(parent.parentEntity.getComponent(HaulingComponent.class).getHauledEntity().getId());
		haulingAllocation.setTargetPositionType(HaulingAllocation.AllocationPositionType.FLOOR);
		haulingAllocation.setTargetPosition(toGridPoint(parent.parentEntity.getLocationComponent(true).getWorldOrParentPosition()));

		PlaceEntityAction subAction = new PlaceEntityAction(parent);
		subAction.update(deltaTime, gameContext);
		completionType = subAction.isCompleted(gameContext);
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
	}
}
