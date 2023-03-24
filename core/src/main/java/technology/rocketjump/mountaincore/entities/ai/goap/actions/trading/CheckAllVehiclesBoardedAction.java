package technology.rocketjump.mountaincore.entities.ai.goap.actions.trading;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureGroup;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemHoldPosition;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

public class CheckAllVehiclesBoardedAction extends Action {

	public CheckAllVehiclesBoardedAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		if (parent.parentEntity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour) {
			CreatureGroup creatureGroup = creatureBehaviour.getCreatureGroup();
			if (creatureGroup != null) {
				completionType = creatureGroup.getMemberIds().stream()
						.map(gameContext::getEntity)
						.filter(e -> e != null && e.getType().equals(EntityType.VEHICLE))
						.allMatch(e -> e.getAttachedEntities().stream().anyMatch(a -> a.holdPosition.equals(ItemHoldPosition.VEHICLE_DRIVER)))
				? CompletionType.SUCCESS : CompletionType.FAILURE;
			}
		}

		if (completionType == null) {
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
