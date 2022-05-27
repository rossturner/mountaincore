package technology.rocketjump.saul.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.SUCCESS;

public class UnassignFurnitureAction extends Action {
	public UnassignFurnitureAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public boolean isInterruptible() {
		return false;
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		// This null check seems to be needed in spite of the isApplicable() method
		if (parent.getAssignedFurnitureId() != null) {
			Entity assignedFurniture = gameContext.getEntities().get(parent.getAssignedFurnitureId());
			if (assignedFurniture == null) {
				Logger.error("Could not find assigned furniture by ID " + parent.getAssignedFurnitureId());
				completionType = FAILURE;
			} else {
				FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) assignedFurniture.getPhysicalEntityComponent().getAttributes();
				attributes.setAssignedToEntityId(null);
				parent.setAssignedFurnitureId(null);
				completionType = SUCCESS;
			}
		} else {
			completionType = FAILURE;
		}
	}

	@Override
	public boolean isApplicable(GameContext gameContext) {
		return parent.getAssignedFurnitureId() != null;
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
