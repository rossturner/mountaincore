package technology.rocketjump.mountaincore.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

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
				completionType = CompletionType.FAILURE;
			} else {
				FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) assignedFurniture.getPhysicalEntityComponent().getAttributes();
				attributes.setAssignedToEntityId(null);
				parent.setAssignedFurnitureId(null);
				completionType = CompletionType.SUCCESS;
			}
		} else {
			completionType = CompletionType.FAILURE;
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
