package technology.rocketjump.saul.production;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.mapping.model.TiledMap;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.misc.VectorUtils;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.ChildPersistable;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.rooms.HaulingAllocation;
import technology.rocketjump.saul.rooms.HaulingAllocationBuilder;

public class FurnitureStockpile extends AbstractStockpile implements ChildPersistable {
    private Entity parentEntity;
    private int maxQuantity;
    private int currentAllocationCount = 0;

    public void setMaxQuantity(int maxQuantity) {
        this.maxQuantity = maxQuantity;
    }

    public void setParentEntity(Entity parentEntity) {
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

    @Override
    public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
        asJson.put("currentAllocationCount", currentAllocationCount);
        asJson.put("maxQuantity", maxQuantity);
    }

    @Override
    public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
        this.currentAllocationCount = asJson.getIntValue("currentAllocationCount");
        this.maxQuantity = asJson.getIntValue("maxQuantity");
    }

    public FurnitureStockpile clone() {
        FurnitureStockpile cloned = new FurnitureStockpile();
        cloned.maxQuantity = this.maxQuantity;
        cloned.currentAllocationCount = this.currentAllocationCount;
        cloned.parentEntity = this.parentEntity;
        return cloned;
    }

}