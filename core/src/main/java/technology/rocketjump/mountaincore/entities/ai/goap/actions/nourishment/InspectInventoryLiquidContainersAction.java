package technology.rocketjump.mountaincore.entities.ai.goap.actions.nourishment;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.components.LiquidAllocation;
import technology.rocketjump.mountaincore.entities.components.LiquidContainerComponent;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.RequestLiquidAllocationMessage;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import java.util.Optional;

import static technology.rocketjump.mountaincore.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.mountaincore.misc.VectorUtils.toVector;

public class InspectInventoryLiquidContainersAction extends Action implements RequestLiquidAllocationMessage.LiquidAllocationCallback {
    public InspectInventoryLiquidContainersAction(AssignedGoal parent) {
        super(parent);
    }

    @Override
    public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
        if (completionType == null) {
            InventoryComponent inventory = parent.parentEntity.getComponent(InventoryComponent.class);
            if (inventory == null) {
                completionType = FAILURE;
            } else {
                Optional<LiquidContainerComponent> needingRefill = inventory.getLiquidContainerNeedingFilling();
                needingRefill.ifPresentOrElse(container -> {
                    parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_LIQUID_ALLOCATION, new RequestLiquidAllocationMessage(
                            parent.parentEntity, container.getMaxLiquidCapacity(),
                            false, true, this));
                }, () -> completionType = FAILURE);
            }
        }
    }

    @Override
    public void allocationFound(Optional<LiquidAllocation> optionalLiquidAllocation) {
        if (optionalLiquidAllocation.isEmpty()) {
            completionType = FAILURE;
        } else {
            LiquidAllocation liquidAllocation = optionalLiquidAllocation.get();
            parent.setLiquidAllocation(liquidAllocation);
            parent.setTargetLocation(toVector(liquidAllocation.getTargetZoneTile().getTargetTile()));
            completionType = CompletionType.SUCCESS;
        }
    }

    @Override
    public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
        // resolves instantly
    }

    @Override
    public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
        // resolves instantly
    }
}
