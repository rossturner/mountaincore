package technology.rocketjump.saul.entities.ai.goap.actions.military;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.saul.assets.entities.tags.TrainingEquipmentTag;
import technology.rocketjump.saul.entities.ai.combat.CreatureCombat;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.actions.Action;
import technology.rocketjump.saul.entities.model.physical.combat.WeaponInfo;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.FurnitureAssignmentRequest;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.saul.assets.entities.tags.TrainingEquipmentTag.TrainingEquipmentType.MELEE;
import static technology.rocketjump.saul.assets.entities.tags.TrainingEquipmentTag.TrainingEquipmentType.RANGED;
import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.SUCCESS;

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
								TrainingEquipmentTag.TrainingEquipmentComponent trainingEquipmentComponent = potentialFurniture.getComponent(TrainingEquipmentTag.TrainingEquipmentComponent.class);
								if (trainingEquipmentComponent == null) {
									return false;
								} else {
									return trainingEquipmentComponent.getEquipmentType().equals(equippedWeapon.isRanged() ? RANGED : MELEE);
								}
							}, assignedFurniture -> {
								if (assignedFurniture == null) {
									completionType = FAILURE;
								} else {
									parent.setAssignedFurnitureId(assignedFurniture.getId());
									completionType = SUCCESS;
								}
					}));
		} else {
			// For animals / non-settlers
			completionType = FAILURE;
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
