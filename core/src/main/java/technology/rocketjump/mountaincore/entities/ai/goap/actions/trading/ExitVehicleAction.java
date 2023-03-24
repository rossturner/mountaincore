package technology.rocketjump.mountaincore.entities.ai.goap.actions.trading;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.components.AttachedEntitiesComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.vehicle.VehicleEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

public class ExitVehicleAction extends Action {

	public ExitVehicleAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		Entity vehicle = parent.parentEntity.getContainingVehicle();
		if (vehicle != null) {
			AttachedEntitiesComponent vehicleAttachedEntities = vehicle.getComponent(AttachedEntitiesComponent.class);
			VehicleEntityAttributes vehicleEntityAttributes = (VehicleEntityAttributes) vehicle.getPhysicalEntityComponent().getAttributes();
			if (vehicleEntityAttributes.getAssignedToEntityId() != null && vehicleEntityAttributes.getAssignedToEntityId() == parent.parentEntity.getId()) {
				vehicleEntityAttributes.setAssignedToEntityId(null);
			}
			vehicleAttachedEntities.remove(parent.parentEntity);

			parent.parentEntity.getLocationComponent().setWorldPosition(vehicle.getLocationComponent().getWorldPosition(), false);

			completionType = CompletionType.SUCCESS;
		} else {
			completionType = CompletionType.FAILURE;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
	}

}
