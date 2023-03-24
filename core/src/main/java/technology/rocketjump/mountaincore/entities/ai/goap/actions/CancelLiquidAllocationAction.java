package technology.rocketjump.mountaincore.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.components.ItemAllocation;
import technology.rocketjump.mountaincore.entities.components.LiquidAllocation;
import technology.rocketjump.mountaincore.entities.components.LiquidContainerComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

public class CancelLiquidAllocationAction extends Action {

	public CancelLiquidAllocationAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public boolean isInterruptible() {
		return false;
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (parent.getLiquidAllocation() != null) {
			completionType = cancelLiquidAllocation(parent.getLiquidAllocation(), gameContext);
			parent.setLiquidAllocation(null);
		} else {
			completionType = CompletionType.FAILURE;
		}
	}

	public static CompletionType cancelLiquidAllocation(LiquidAllocation liquidAllocation, GameContext gameContext) {
		switch (liquidAllocation.getType()) {
			case FROM_RIVER: {
				return CompletionType.SUCCESS;
			}
			case FROM_LIQUID_CONTAINER: {
				Entity targetEntity = gameContext.getEntities().get(liquidAllocation.getTargetContainerId());
				if (targetEntity == null) {
					Logger.warn("Target entity for " + CancelLiquidAllocationAction.class.getSimpleName() + " is null, probably removed furniture");
					return CompletionType.FAILURE;
				}

				LiquidContainerComponent liquidContainerComponent1 = targetEntity.getComponent(LiquidContainerComponent.class);
				if (liquidContainerComponent1 == null) {
					Logger.error("Target entity does not have LiquidContainerComponent");
					return CompletionType.FAILURE;
				}

				liquidContainerComponent1.cancelAllocation(liquidAllocation);
				if (liquidAllocation.getState().equals(ItemAllocation.AllocationState.CANCELLED)) {
					return CompletionType.SUCCESS;
				} else {
					Logger.error("LiquidAllocation was not cancelled successfully");
					return CompletionType.FAILURE;
				}
			}
			default:
				Logger.error("Not yet implemented cancelLiquidAllocation() for type " + liquidAllocation.getType());
		}
		return CompletionType.FAILURE;
	}


	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {

	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {

	}
}
