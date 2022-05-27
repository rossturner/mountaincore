package technology.rocketjump.saul.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.saul.assets.entities.tags.BedSleepingPositionTag;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.FurnitureAssignmentRequest;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.SUCCESS;

public class LocateBedAction extends Action implements FurnitureAssignmentCallback {

	public LocateBedAction(AssignedGoal parent) {
		super(parent);
	}

	boolean willingToSleepOnFloor = false;

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (parent.parentEntity.getBehaviourComponent().isJobAssignable()) {
			parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_FURNITURE_ASSIGNMENT,
					new FurnitureAssignmentRequest(BedSleepingPositionTag.class, parent.parentEntity, this, willingToSleepOnFloor));
		} else {
			// For animals / non-settlers
			completionType = FAILURE;
		}
	}

	@Override
	public void furnitureAssigned(Entity furnitureEntity) {
		if (furnitureEntity == null) {
			if (!willingToSleepOnFloor) {
				willingToSleepOnFloor = true;
				parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_FURNITURE_ASSIGNMENT,
						new FurnitureAssignmentRequest(BedSleepingPositionTag.class, parent.parentEntity, this, willingToSleepOnFloor));
			} else {
				completionType = FAILURE;
			}
		} else {
			parent.setAssignedFurnitureId(furnitureEntity.getId());
			completionType = SUCCESS;
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
