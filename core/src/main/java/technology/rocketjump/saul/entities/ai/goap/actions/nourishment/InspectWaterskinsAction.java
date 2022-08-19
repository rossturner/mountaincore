package technology.rocketjump.saul.entities.ai.goap.actions.nourishment;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.saul.entities.ai.goap.actions.Action;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.components.LiquidAllocation;
import technology.rocketjump.saul.entities.components.LiquidContainerComponent;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.RequestLiquidAllocationMessage;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.Objects;
import java.util.Optional;

import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.saul.misc.VectorUtils.toVector;

public class InspectWaterskinsAction extends Action implements RequestLiquidAllocationMessage.LiquidAllocationCallback {
    public InspectWaterskinsAction(AssignedGoal parent) {
        super(parent);
    }

    @Override
    public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {

        if (completionType == null) {
            InventoryComponent inventory = parent.parentEntity.getComponent(InventoryComponent.class);
            if (inventory == null) {
                completionType = FAILURE;
            } else {
                Optional<LiquidContainerComponent> needingRefill = inventory.getInventoryEntries().stream()
                        .map(item -> item.entity.getComponent(LiquidContainerComponent.class))
                        .filter(Objects::nonNull)
                        .filter(container -> container.getMaxLiquidCapacity() > container.getLiquidQuantity())
                        .filter(container -> container.getLiquidQuantity() < LocateDrinkAction.LIQUID_AMOUNT_FOR_DRINK_CONSUMPTION)
                        .findFirst();
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
