package technology.rocketjump.saul.entities.ai.goap.condition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import technology.rocketjump.saul.entities.ai.goap.GoalSelectionCondition;
import technology.rocketjump.saul.entities.ai.goap.Operator;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.environment.GameClock;

public class GoalSelectionByInventory implements GoalSelectionCondition {

    public final String itemTypeName;
    public final Operator operator;
    public final Integer value;
    public final Integer targetQuantity;

    @JsonCreator
    public GoalSelectionByInventory(
            @JsonProperty("itemType") String itemTypeName,
            @JsonProperty("operator") Operator operator,
            @JsonProperty("value") Integer value,
            @JsonProperty("targetQuantity") Integer targetQuantity) {
        this.itemTypeName = itemTypeName;
        this.operator = operator;
        this.value = value;
        this.targetQuantity = targetQuantity;
    }

    @JsonIgnore
    @Override
    public boolean apply(GameClock gameClock, Entity parentEntity) {
        InventoryComponent inventory = parentEntity.getComponent(InventoryComponent.class);

        if (inventory != null) {
            double currentQuantity = getCurrentQuantity(gameClock, inventory);
            return operator.apply(currentQuantity, value);
        }
        return false;
    }

    public int getCurrentQuantity(GameClock gameClock, InventoryComponent inventory) {
        int currentQuantity = 0;
        InventoryComponent.InventoryEntry found = inventory.findByItemType(itemTypeName, gameClock);
        if (found != null && found.entity.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes attributes) {
            currentQuantity = attributes.getQuantity();
        }
        return currentQuantity;
    }
}
