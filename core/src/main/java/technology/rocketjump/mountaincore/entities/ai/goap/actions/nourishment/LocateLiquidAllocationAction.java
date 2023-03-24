package technology.rocketjump.mountaincore.entities.ai.goap.actions.nourishment;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.components.LiquidAllocation;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.RequestLiquidAllocationMessage;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;

import java.util.Optional;

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
			parent.getAssignedHaulingAllocation().setTargetPositionType(HaulingAllocation.AllocationPositionType.ZONE);
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
