package technology.rocketjump.mountaincore.entities.ai.goap.actions.trading;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.components.AttachedEntitiesComponent;
import technology.rocketjump.mountaincore.entities.components.FactionComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemHoldPosition;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

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
