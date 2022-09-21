package technology.rocketjump.saul.production;

import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.mapping.model.TiledMap;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.misc.VectorUtils;
import technology.rocketjump.saul.rooms.HaulingAllocation;
import technology.rocketjump.saul.rooms.HaulingAllocationBuilder;

public class FurnitureStockpile extends AbstractStockpile {
    private final Entity parentEntity;
    private int currentAllocationCount = 0;
    private final int maxQuantity = 6; //TODO: expose in the json

    public FurnitureStockpile(Entity parentEntity) {
        this.parentEntity = parentEntity;
    }

    @Override
    protected StockpileAllocation findExistingAllocation(Entity entity, TiledMap map, int maxStackSize, int quantityToAllocate) {
        return null;
    }

    @Override
    protected StockpileAllocation createAllocation(TiledMap map, ItemType itemType, GameMaterial itemMaterial, Race corpseRace) {
        InventoryComponent inventoryComponent = parentEntity.getComponent(InventoryComponent.class);
        if (inventoryComponent != null && (inventoryComponent.getInventoryEntries().size() + currentAllocationCount) < maxQuantity) {
            //if enough space then return one else return null
            StockpileAllocation allocationToUse = new StockpileAllocation(VectorUtils.toGridPoint(parentEntity.getLocationComponent().getWorldPosition()));
            allocationToUse.setItemType(itemType);
            allocationToUse.setGameMaterial(itemMaterial);
            allocationToUse.setRaceCorpse(corpseRace);

            currentAllocationCount++;
            return allocationToUse;
        }
        return null;
    }

    @Override
    protected HaulingAllocation createHaulingAllocation(Entity entity, Entity requestingEntity, int quantityToAllocate, StockpileAllocation allocationToUse) {
        return HaulingAllocationBuilder.createWithItemAllocation(quantityToAllocate, entity, requestingEntity)
                .toEntity(parentEntity);
    }

    public void cancelAllocation() {
        currentAllocationCount--;
    }
}