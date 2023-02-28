package technology.rocketjump.saul.entities.ai.goap.actions.trading;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.saul.entities.ai.goap.actions.Action;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.saul.entities.behaviour.creature.TraderCreatureGroup;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.vehicle.VehicleEntityAttributes;
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
		if (parent.parentEntity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour &&
				creatureBehaviour.getCreatureGroup() instanceof TraderCreatureGroup traderCreatureGroup) {
			for (Long memberId : traderCreatureGroup.getMemberIds()) {
				Entity entity = gameContext.getEntity(memberId);
				if (entity != null && entity.getPhysicalEntityComponent().getAttributes() instanceof VehicleEntityAttributes vehicleEntityAttributes) {
					if (vehicleEntityAttributes.getAssignedToEntityId() == null) {
						vehicleEntityAttributes.setAssignedToEntityId(parent.parentEntity.getId());
						parent.setAssignedVehicleId(entity.getId());
						parent.setTargetLocation(entity.getLocationComponent().getWorldOrParentPosition());
						completionType = CompletionType.SUCCESS;
						return;
					}
				}
			}
		}
		completionType = CompletionType.FAILURE;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
	}
}
