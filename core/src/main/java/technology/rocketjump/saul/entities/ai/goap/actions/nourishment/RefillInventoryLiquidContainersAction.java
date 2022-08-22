package technology.rocketjump.saul.entities.ai.goap.actions.nourishment;

import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.saul.entities.ai.goap.actions.Action;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.components.LiquidAllocation;
import technology.rocketjump.saul.entities.components.LiquidContainerComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.materials.model.GameMaterial;

import java.util.Objects;
import java.util.Optional;

import static technology.rocketjump.saul.entities.ai.goap.actions.nourishment.ConsumeLiquidFromContainerAction.getFirstFurnitureEntity;

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
                Entity targetFurniture = getFirstFurnitureEntity(targetZoneTile);
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
}
