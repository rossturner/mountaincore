package technology.rocketjump.saul.entities.ai.goap.actions.crafting;

import com.alibaba.fastjson.JSONObject;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.SpecialGoal;
import technology.rocketjump.saul.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.saul.entities.ai.goap.actions.Action;
import technology.rocketjump.saul.entities.ai.goap.actions.InitialisableAction;
import technology.rocketjump.saul.entities.behaviour.furniture.CraftingStationBehaviour;
import technology.rocketjump.saul.entities.components.LiquidAllocation;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.RequestHaulingAllocationMessage;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.rooms.HaulingAllocation;

import java.util.List;

public class MoveLiquidInputToCraftingStation extends Action implements InitialisableAction {

	private AssignedGoal subGoal;

	public MoveLiquidInputToCraftingStation(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void init() {
		if (subGoal != null) {
			subGoal.setParentGoal(this.parent);
			subGoal.init(parent.parentEntity, parent.messageDispatcher);
		}
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		if (subGoal == null) {
			initialiseLiquidTransferInput(gameContext);
		} else {
			if (subGoal.isComplete()) {
				completionType = CompletionType.SUCCESS;
			} else {
				subGoal.update(deltaTime, gameContext);
			}
		}
	}

	private void initialiseLiquidTransferInput(GameContext gameContext) {
		Entity craftingStation = gameContext.getEntities().get(parent.getAssignedJob().getTargetId());
		if (craftingStation != null && craftingStation.getBehaviourComponent() instanceof CraftingStationBehaviour craftingStationBehaviour
				&& craftingStationBehaviour.getCurrentCraftingAssignment() != null) {

			ItemType itemType = craftingStationBehaviour.getRelatedItemTypes().get(0);
			if (itemType == null) {
				Logger.error("Expecting related item type on crafting station {}", craftingStation);
				completionType = CompletionType.FAILURE;
				return;
			}

			List<LiquidAllocation> liquidAllocations = craftingStationBehaviour.getCurrentCraftingAssignment().getInputLiquidAllocations();
			LiquidAllocation liquidAllocation = liquidAllocations.remove(0);

			subGoal = new AssignedGoal(SpecialGoal.TRANSFER_LIQUID_FOR_CRAFTING.getInstance(), parent.parentEntity, parent.messageDispatcher);

			// need a hauling allocation of an item to carry the liquid in
			parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_HAULING_ALLOCATION, new RequestHaulingAllocationMessage(
					parent.parentEntity, parent.parentEntity.getLocationComponent().getWorldOrParentPosition(), itemType,
					null, // any material
					true, null, null, (allocation) -> {
				if (allocation != null) {
					allocation.setTargetPositionType(HaulingAllocation.AllocationPositionType.ZONE);
					allocation.setTargetId(liquidAllocation.getTargetContainerId());
					allocation.setTargetPosition(liquidAllocation.getTargetZoneTile().getAccessLocation());

					subGoal.setAssignedHaulingAllocation(allocation);
				}
			}));

			if (subGoal.getAssignedHaulingAllocation() == null) {
				completionType = CompletionType.FAILURE;
				liquidAllocations.add(liquidAllocation);
			} else {
				subGoal.setLiquidAllocation(liquidAllocation);
				subGoal.setParentGoal(this.parent);
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
