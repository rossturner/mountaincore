package technology.rocketjump.saul.entities.ai.goap.condition;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import technology.rocketjump.saul.entities.ai.goap.GoalSelectionCondition;
import technology.rocketjump.saul.entities.ai.goap.Operator;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.components.creature.MilitaryComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.item.AmmoType;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.gamecontext.GameContext;

public class GoalSelectionByWeaponAmmo implements GoalSelectionCondition {

    public final Operator operator;
    public final Integer value;
    public final Integer targetQuantity;

    @JsonCreator
    public GoalSelectionByWeaponAmmo(
            @JsonProperty("operator") Operator operator,
            @JsonProperty("value") Integer value,
            @JsonProperty("targetQuantity") Integer targetQuantity) {
        this.operator = operator;
        this.value = value;
        this.targetQuantity = targetQuantity;
    }

    @JsonIgnore
    @Override
    public boolean apply(Entity parentEntity, GameContext gameContext) {
        MilitaryComponent militaryComponent = parentEntity.getComponent(MilitaryComponent.class);
        if (militaryComponent != null) {
            Long assignedWeaponId = militaryComponent.getAssignedWeaponId();
            if (assignedWeaponId != null) {
                Entity assignedWeapon = gameContext.getEntities().get(assignedWeaponId);
                if (assignedWeapon != null && assignedWeapon.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes weaponAttributes) {
                    if (weaponAttributes.getItemType().getWeaponInfo() != null) {
                        AmmoType requiredAmmoType = weaponAttributes.getItemType().getWeaponInfo().getRequiresAmmoType();
                        if (requiredAmmoType != null) {
                            return operator.apply(amountInInventory(parentEntity, requiredAmmoType), value);
                        }
                    }
                }
            }
        }
        return false;
    }

    public int amountInInventory(Entity entity, AmmoType requiredAmmoType) {
        int amountInInventory = 0;
        InventoryComponent inventory = entity.getOrCreateComponent(InventoryComponent.class);
        for (InventoryComponent.InventoryEntry entry : inventory.getInventoryEntries()) {
            if (entry.entity.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes inventoryItemAttributes) {
                if (requiredAmmoType.equals(inventoryItemAttributes.getItemType().getIsAmmoType())) {
                    amountInInventory += inventoryItemAttributes.getQuantity();
                }
            }
        }
        return amountInInventory;
    }

}
