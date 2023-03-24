package technology.rocketjump.mountaincore.entities.ai.goap.actions.crafting;

import com.alibaba.fastjson.JSONObject;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.CraftingStationBehaviour;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

public class CheckForPendingLiquidInputAction extends Action {

	public CheckForPendingLiquidInputAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		if (parent.getAssignedJob() != null && parent.getAssignedJob().getCraftingRecipe() != null) {
			Entity craftingStation = gameContext.getEntities().get(parent.getAssignedJob().getTargetId());
			if (craftingStation != null && craftingStation.getBehaviourComponent() instanceof CraftingStationBehaviour craftingStationBehaviour) {
				if (craftingStationBehaviour.getCurrentCraftingAssignment() != null) {
					if (!craftingStationBehaviour.getCurrentCraftingAssignment().getInputLiquidAllocations().isEmpty()) {
						completionType = CompletionType.SUCCESS;
						return;
					}
				}
			}
		} else {
			Logger.error(getClass().getSimpleName() + " is in use with a job that does not have a crafting recipe attached");
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
