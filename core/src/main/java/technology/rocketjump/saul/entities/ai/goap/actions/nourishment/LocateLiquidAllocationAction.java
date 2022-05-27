package technology.rocketjump.saul.entities.ai.goap.actions.nourishment;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.actions.Action;
import technology.rocketjump.saul.entities.components.LiquidAllocation;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.RequestLiquidAllocationMessage;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.Optional;

import static technology.rocketjump.saul.rooms.HaulingAllocation.AllocationPositionType.ZONE;

public class LocateLiquidAllocationAction extends Action implements RequestLiquidAllocationMessage.LiquidAllocationCallback {

	public LocateLiquidAllocationAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_LIQUID_ALLOCATION, new RequestLiquidAllocationMessage(
				parent.parentEntity, 1, false, true, this));
	}

	@Override
	public void allocationFound(Optional<LiquidAllocation> optionalAllocation) {
		if (optionalAllocation.isPresent()) {
			LiquidAllocation liquidAllocation = optionalAllocation.get();
			parent.setLiquidAllocation(liquidAllocation);
			parent.getAssignedHaulingAllocation().setTargetPositionType(ZONE);
			parent.getAssignedHaulingAllocation().setTargetPosition(liquidAllocation.getTargetZoneTile().getAccessLocation());
			parent.getAssignedHaulingAllocation().setTargetId(liquidAllocation.getTargetContainerId());
			completionType = CompletionType.SUCCESS;
		} else {
			completionType = CompletionType.FAILURE;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {

	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {

	}
}
