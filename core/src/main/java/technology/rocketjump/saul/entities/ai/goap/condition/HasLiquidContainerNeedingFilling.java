package technology.rocketjump.saul.entities.ai.goap.condition;

import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.saul.entities.ai.goap.GoalSelectionCondition;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.environment.GameClock;

public class HasLiquidContainerNeedingFilling implements GoalSelectionCondition {
    @JsonIgnore
    @Override
    public boolean apply(GameClock gameClock, Entity parentEntity) {
        InventoryComponent inventory = parentEntity.getComponent(InventoryComponent.class);
        if (inventory != null) {
            return inventory.getLiquidContainerNeedingFilling().isPresent();
        }
        return false;
    }
}
