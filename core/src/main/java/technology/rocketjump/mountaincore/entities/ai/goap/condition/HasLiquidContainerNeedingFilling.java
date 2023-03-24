package technology.rocketjump.mountaincore.entities.ai.goap.condition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public class HasLiquidContainerNeedingFilling implements GoalSelectionCondition {
    @JsonIgnore
    @Override
    public boolean apply(Entity parentEntity, GameContext gameContext) {
        InventoryComponent inventory = parentEntity.getComponent(InventoryComponent.class);
        if (inventory != null) {
            return inventory.getLiquidContainerNeedingFilling().isPresent();
        }
        return false;
    }
}
