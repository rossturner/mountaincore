package technology.rocketjump.mountaincore.entities.ai.goap.actions.crafting;

import com.alibaba.fastjson.JSONObject;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SpecialGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.InitialisableAction;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.CraftingStationBehaviour;
import technology.rocketjump.mountaincore.entities.components.LiquidAllocation;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.RequestHaulingAllocationMessage;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MoveLiquidInputToCraftingStationAction extends Action implements InitialisableAction {

	public static final int MAX_ATTEMPTS_PER_LIQUID_ALLOCATION = 3;
	private AssignedGoal subGoal;
	private final Map<Integer, Integer> transferAttempts = new HashMap<>();

	public MoveLiquidInputToCraftingStationAction(AssignedGoal parent) {
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
		if (subGoal != null) {
			if (subGoal.isComplete()) {
				initialiseLiquidTransferInput(gameContext);
			} else {
				subGoal.update(deltaTime, gameContext);
			}
		} else {
			initialiseLiquidTransferInput(gameContext);
		}
	}

	private void initialiseLiquidTransferInput(GameContext gameContext) {
		if (parent.getAssignedJob() == null) {
			completionType = CompletionType.FAILURE;
			return;
		}
		Entity craftingStation = gameContext.getEntities().get(parent.getAssignedJob().getTargetId());
		if (craftingStation != null && craftingStation.getBehaviourComponent() instanceof CraftingStationBehaviour craftingStationBehaviour
				&& craftingStationBehaviour.getCurrentCraftingAssignment() != null) {

			List<LiquidAllocation> liquidAllocations = craftingStationBehaviour.getCurrentCraftingAssignment().getInputLiquidAllocations();
			if (liquidAllocations.isEmpty()) {
				completionType = CompletionType.SUCCESS;
			} else {

				if (craftingStationBehaviour.getRelatedItemTypes().isEmpty() || craftingStationBehaviour.getRelatedItemTypes().get(0) == null) {
					completionType = CompletionType.FAILURE;
					Logger.error("Expecting related item type on crafting station {}", craftingStation);
					return;
				}
				ItemType itemType = craftingStationBehaviour.getRelatedItemTypes().get(0);

				LiquidAllocation liquidAllocation = liquidAllocations.remove(0);
				Integer attempt = transferAttempts.getOrDefault(liquidAllocations.size(), 0);
				if (attempt > MAX_ATTEMPTS_PER_LIQUID_ALLOCATION) {
					liquidAllocations.add(liquidAllocation);
					completionType = CompletionType.FAILURE;
					return;
				} else {
					transferAttempts.put(liquidAllocations.size(), attempt + 1);
				}

				subGoal = new AssignedGoal(SpecialGoal.TRANSFER_LIQUID_FOR_CRAFTING.getInstance(), parent.parentEntity, parent.messageDispatcher, gameContext);
				subGoal.setAssignedJob(parent.getAssignedJob());

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
