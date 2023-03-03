package technology.rocketjump.saul.entities.ai.goap.actions.trading;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.saul.entities.ai.goap.actions.Action;
import technology.rocketjump.saul.entities.components.AttachedEntitiesComponent;
import technology.rocketjump.saul.entities.components.FactionComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.item.ItemHoldPosition;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

public class BoardVehicleAction extends Action {

	public BoardVehicleAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		if (parent.parentEntity.getContainingVehicle() != null) {
			completionType = CompletionType.FAILURE;
			return;
		}

		Long assignedVehicleId = parent.getAssignedVehicleId();
		if (assignedVehicleId != null) {
			MapTile currentTile = gameContext.getAreaMap().getTile(parent.parentEntity.getLocationComponent().getWorldOrParentPosition());
			if (currentTile != null) {
				Entity vehicle = gameContext.getEntity(assignedVehicleId);
				if (vehicle != null && nextTo(vehicle) && vehicle.getComponent(FactionComponent.class).getFaction().equals(parent.parentEntity.getComponent(FactionComponent.class).getFaction())) {
					AttachedEntitiesComponent attachedEntitiesComponent = vehicle.getComponent(AttachedEntitiesComponent.class);
					if (attachedEntitiesComponent.getAttachedEntities().stream().noneMatch(a -> a.holdPosition.equals(ItemHoldPosition.VEHICLE_DRIVER))) {
						attachedEntitiesComponent.addAttachedEntity(parent.parentEntity, ItemHoldPosition.VEHICLE_DRIVER);
						completionType = CompletionType.SUCCESS;
						return;
					}
				}
			}
		}
		completionType = CompletionType.FAILURE;
	}

	private boolean nextTo(Entity vehicle) {
		return vehicle.getLocationComponent().getWorldPosition().cpy().dst(
				parent.parentEntity.getLocationComponent().getWorldPosition()) < 1.5f;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
	}
}
