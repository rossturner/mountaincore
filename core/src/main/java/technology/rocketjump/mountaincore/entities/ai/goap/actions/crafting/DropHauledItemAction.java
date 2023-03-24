package technology.rocketjump.mountaincore.entities.ai.goap.actions.crafting;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.PlaceEntityAction;
import technology.rocketjump.mountaincore.entities.model.physical.creature.HaulingComponent;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.misc.VectorUtils;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;

public class DropHauledItemAction extends Action {

	public DropHauledItemAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		HaulingAllocation haulingAllocation = parent.getAssignedHaulingAllocation();
		haulingAllocation.setTargetId(parent.parentEntity.getComponent(HaulingComponent.class).getHauledEntity().getId());
		haulingAllocation.setTargetPositionType(HaulingAllocation.AllocationPositionType.FLOOR);
		haulingAllocation.setTargetPosition(VectorUtils.toGridPoint(parent.parentEntity.getLocationComponent().getWorldOrParentPosition()));

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
