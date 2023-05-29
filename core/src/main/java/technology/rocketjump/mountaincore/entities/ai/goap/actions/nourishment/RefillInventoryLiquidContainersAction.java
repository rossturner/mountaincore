package technology.rocketjump.mountaincore.entities.ai.goap.actions.nourishment;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.components.LiquidAllocation;
import technology.rocketjump.mountaincore.entities.components.LiquidContainerComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import java.util.Objects;
import java.util.Optional;

public class RefillInventoryLiquidContainersAction extends Action {
    public RefillInventoryLiquidContainersAction(AssignedGoal parent) {
        super(parent);
    }

    @Override
    public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
        LiquidAllocation liquidAllocation = parent.getLiquidAllocation();
        completionType = CompletionType.FAILURE;

        InventoryComponent inventory = parent.parentEntity.getComponent(InventoryComponent.class);
        if (liquidAllocation == null || inventory == null) {
            return;
        } else {
            //TODO: this is copied from InspectWaterSkins action
            Optional<LiquidContainerComponent> needingRefill = inventory.getInventoryEntries().stream()
                    .map(item -> item.entity.getComponent(LiquidContainerComponent.class))
                    .filter(Objects::nonNull)
                    .filter(container -> container.getMaxLiquidCapacity() > container.getLiquidQuantity())
                    .filter(container -> container.getLiquidQuantity() < LocateDrinkAction.LIQUID_AMOUNT_FOR_DRINK_CONSUMPTION)
                    .findFirst();

            if (needingRefill.isPresent()) {
                MapTile targetZoneTile = gameContext.getAreaMap().getTile(liquidAllocation.getTargetZoneTile().getTargetTile());
                Entity targetFurniture = ConsumeLiquidFromContainerAction.getFirstFurnitureEntity(targetZoneTile);
                if (targetZoneTile.getFloor().isRiverTile()) {
                    refill(targetZoneTile.getFloor().getMaterial(), needingRefill.get());
                    completionType = CompletionType.SUCCESS;
                } else if (targetFurniture != null) {
                    LiquidContainerComponent targetLiquidContainer = targetFurniture.getComponent(LiquidContainerComponent.class);
                    if (targetLiquidContainer != null) {
                        LiquidAllocation success = targetLiquidContainer.cancelAllocationAndDecrementQuantity(liquidAllocation);
                        if (success != null) {
                            refill(targetLiquidContainer.getTargetLiquidMaterial(), needingRefill.get());
                            completionType = CompletionType.SUCCESS;
                        }
                    }
                }
            }
        }
        parent.setLiquidAllocation(null);
    }

    private void refill(GameMaterial material, LiquidContainerComponent liquidContainer) {
        liquidContainer.setTargetLiquidMaterial(material);
        liquidContainer.setLiquidQuantity(parent.getLiquidAllocation().getAllocationAmount());
    }

    @Override
    public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
    }

    @Override
    public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
    }
}
