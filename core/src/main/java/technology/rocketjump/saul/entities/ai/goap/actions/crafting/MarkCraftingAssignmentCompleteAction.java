package technology.rocketjump.saul.entities.ai.goap.actions.crafting;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.saul.entities.ai.goap.actions.Action;
import technology.rocketjump.saul.entities.behaviour.furniture.CraftingStationBehaviour;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

public class MarkCraftingAssignmentCompleteAction extends Action {

	public MarkCraftingAssignmentCompleteAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		Entity craftingStation = gameContext.getEntities().get(parent.getAssignedJob().getTargetId());
		if (craftingStation != null && craftingStation.getBehaviourComponent() instanceof CraftingStationBehaviour craftingStationBehaviour) {
			craftingStationBehaviour.assignmentCompleted();
		}
		completionType = CompletionType.SUCCESS;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
	}
}
