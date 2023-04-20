package technology.rocketjump.mountaincore.settlement.production;

import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;

public class CraftingQuota {
    public static final CraftingQuota UNLIMITED = new Unlimited();
    private static final int MIN_QUANTITY = 0;
    private static final int MAX_QUANTITY = 999;
    private ItemType itemType;
    private GameMaterial gameMaterial;
    private int quantity;

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = Math.min(Math.max(quantity, MIN_QUANTITY), MAX_QUANTITY);
    }

    public ItemType getItemType() {
        return itemType;
    }

    public void setItemType(ItemType itemType) {
        this.itemType = itemType;
    }

    public GameMaterial getGameMaterial() {
        return gameMaterial;
    }

    public void setGameMaterial(GameMaterial gameMaterial) {
        this.gameMaterial = gameMaterial;
    }

    public boolean isLimited() {
        return true;
    }

    private static class Unlimited extends CraftingQuota {
        @Override
        public boolean isLimited() {
            return false;
        }
    }
}
