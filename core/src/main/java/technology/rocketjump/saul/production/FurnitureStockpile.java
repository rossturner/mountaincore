package technology.rocketjump.saul.production;

import com.alibaba.fastjson.JSONArray;
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

import java.util.HashMap;
import java.util.Map;

public class FurnitureStockpile extends AbstractStockpile implements ChildPersistable {

    private Entity parentEntity;
    private int maxQuantity;
    private final Map<Long, StockpileAllocation> allocationsByHaulingAllocationId = new HashMap<>();

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
        if (inventoryComponent != null && (inventoryComponent.getInventoryEntries().size() + allocationsByHaulingAllocationId.size()) < maxQuantity) {
            //if enough space then return one else return null
            StockpileAllocation allocationToUse = new StockpileAllocation(VectorUtils.toGridPoint(parentEntity.getLocationComponent(true).getWorldPosition()));
            allocationToUse.setItemType(itemType);
            allocationToUse.setGameMaterial(itemMaterial);
            allocationToUse.setRaceCorpse(corpseRace);

            return allocationToUse;
        }
        return null;
    }

    @Override
    protected HaulingAllocation createHaulingAllocation(Entity entity, Entity requestingEntity, int quantityToAllocate, StockpileAllocation allocationToUse) {
        HaulingAllocation haulingAllocation = HaulingAllocationBuilder.createWithItemAllocation(quantityToAllocate, entity, requestingEntity)
                .toEntity(parentEntity);
        allocationsByHaulingAllocationId.put(haulingAllocation.getHaulingAllocationId(), allocationToUse);
        return haulingAllocation;
    }

    public void cancelAllocation(HaulingAllocation haulingAllocation) {
        allocationsByHaulingAllocationId.remove(haulingAllocation.getHaulingAllocationId());
    }

    public FurnitureStockpile clone() {
        FurnitureStockpile cloned = new FurnitureStockpile();
        cloned.maxQuantity = this.maxQuantity;
        cloned.parentEntity = this.parentEntity;
        cloned.allocationsByHaulingAllocationId.putAll(this.allocationsByHaulingAllocationId);
        return cloned;
    }

    @Override
    public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
        asJson.put("maxQuantity", maxQuantity);

        if (!allocationsByHaulingAllocationId.isEmpty()) {
            JSONArray allocationsJson = new JSONArray();
            for (Map.Entry<Long, StockpileAllocation> entry : allocationsByHaulingAllocationId.entrySet()) {
                JSONObject entryJson = new JSONObject(true);
                entryJson.put("haulingAllocationId", entry.getKey());
                if (entry.getValue() != null) {
                    JSONObject allocationJson = new JSONObject(true);
                    entry.getValue().writeTo(allocationJson, savedGameStateHolder);
                    entryJson.put("allocation", allocationJson);
                }
                allocationsJson.add(entryJson);
            }
            asJson.put("allocations", allocationsJson);
        }
    }

    @Override
    public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
        this.maxQuantity = asJson.getIntValue("maxQuantity");

        JSONArray allocationsJson = asJson.getJSONArray("allocations");
        if (allocationsJson != null) {
            for (int cursor = 0; cursor < allocationsJson.size(); cursor++) {
                JSONObject entryJson = allocationsJson.getJSONObject(cursor);
                Long haulingAllocationId = entryJson.getLong("haulingAllocationId");
                StockpileAllocation allocation = null;
                JSONObject allocationJson = entryJson.getJSONObject("allocation");
                if (allocationJson != null) {
                    allocation = new StockpileAllocation(null);
                    allocation.readFrom(allocationJson, savedGameStateHolder, relatedStores);
                }

                allocationsByHaulingAllocationId.put(haulingAllocationId, allocation);
            }
        }
    }

}