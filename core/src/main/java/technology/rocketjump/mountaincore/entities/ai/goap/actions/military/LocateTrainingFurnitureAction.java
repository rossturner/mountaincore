package technology.rocketjump.mountaincore.entities.ai.goap.actions.military;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.assets.entities.tags.TrainingEquipmentTag;
import technology.rocketjump.mountaincore.entities.ai.combat.CreatureCombat;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.model.physical.combat.WeaponInfo;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.FurnitureAssignmentRequest;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

public class LocateTrainingFurnitureAction extends Action {

	public LocateTrainingFurnitureAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (parent.parentEntity.getBehaviourComponent().isJobAssignable()) {
			CreatureCombat creatureCombat = new CreatureCombat(parent.parentEntity);
			WeaponInfo equippedWeapon = creatureCombat.getEquippedWeapon();

			parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_FURNITURE_ASSIGNMENT,
					new FurnitureAssignmentRequest(TrainingEquipmentTag.class, parent.parentEntity,
							potentialFurniture -> {
								TrainingEquipmentTag trainingEquipmentTag = potentialFurniture.getTag(TrainingEquipmentTag.class);
								if (trainingEquipmentTag == null) {
									return false;
								} else {
									return trainingEquipmentTag.getTrainingEquipmentType().equals(equippedWeapon.isRanged() ? TrainingEquipmentTag.TrainingEquipmentType.RANGED : TrainingEquipmentTag.TrainingEquipmentType.MELEE);
								}
							}, assignedFurniture -> {
								if (assignedFurniture == null) {
									completionType = CompletionType.FAILURE;
								} else {
									parent.setAssignedFurnitureId(assignedFurniture.getId());
									completionType = CompletionType.SUCCESS;
								}
					}));
		} else {
			// For animals / non-settlers
			completionType = CompletionType.FAILURE;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		// No state to write
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		// No state to read
	}
}
