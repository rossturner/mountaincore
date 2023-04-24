package technology.rocketjump.mountaincore.entities.ai.goap.actions.crafting;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SpecialGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.InitialisableAction;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.CraftingStationBehaviour;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;

import java.util.List;

public class MoveInputToCraftingStationAction extends Action implements InitialisableAction {

	private AssignedGoal subGoal;

	public MoveInputToCraftingStationAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void init(GameContext gameContext) {
		if (subGoal != null) {
			subGoal.setParentGoal(this.parent);
			subGoal.init(parent.parentEntity, parent.messageDispatcher, gameContext);
		}
	}

	@Override
	public void actionInterrupted(GameContext gameContext) {
		super.actionInterrupted(gameContext);
		if (subGoal != null) {
			subGoal.setInterrupted(true);
			subGoal.destroy(null, parent.messageDispatcher, gameContext);
		}
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		if (subGoal == null) {
			initialiseHaulingOfInput(gameContext);
		} else {
			if (subGoal.isComplete()) {
				completionType = CompletionType.SUCCESS;
			} else {
				subGoal.update(deltaTime, gameContext);
			}
		}
	}

	private void initialiseHaulingOfInput(GameContext gameContext) {
		Entity craftingStation = gameContext.getEntities().get(parent.getAssignedJob().getTargetId());
		if (craftingStation != null && craftingStation.getBehaviourComponent() instanceof CraftingStationBehaviour craftingStationBehaviour
				&& craftingStationBehaviour.getCurrentCraftingAssignment() != null) {

			List<HaulingAllocation> inputAllocations = craftingStationBehaviour.getCurrentCraftingAssignment().getInputAllocations();
			if (!inputAllocations.isEmpty()) {
				HaulingAllocation haulingAllocation = inputAllocations.remove(0);

				subGoal = new AssignedGoal(SpecialGoal.HAUL_ITEM.getInstance(), parent.parentEntity, parent.messageDispatcher, gameContext);
				subGoal.setAssignedHaulingAllocation(haulingAllocation);
				subGoal.setParentGoal(this.parent);
			} else {
				completionType = CompletionType.FAILURE;
			}
		} else {
			completionType = CompletionType.FAILURE;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (subGoal != null) {
			JSONObject subGoalJson = new JSONObject(true);
			subGoal.writeTo(subGoalJson, savedGameStateHolder);
			asJson.put("subGoal", subGoalJson);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		JSONObject subGoalJson = asJson.getJSONObject("subGoal");
		if (subGoalJson != null) {
			subGoal = new AssignedGoal();
			subGoal.readFrom(subGoalJson, savedGameStateHolder, relatedStores);
		}
	}
}
