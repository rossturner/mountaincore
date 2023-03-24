package technology.rocketjump.mountaincore.production;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Race;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.mapping.model.TiledMap;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.misc.VectorUtils;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.ChildPersistable;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;
import technology.rocketjump.mountaincore.rooms.HaulingAllocationBuilder;

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
        InventoryComponent parentInventory = parentEntity.getComponent(InventoryComponent.class);
        if (entity.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes itemAttributes) {
            InventoryComponent.InventoryEntry matchingInventoryEntry = parentInventory.findByItemTypeAndMaterial(itemAttributes.getItemType(), itemAttributes.getPrimaryMaterial(), null);
            if (matchingInventoryEntry != null) {
                ItemEntityAttributes inventoryItemAttributes = (ItemEntityAttributes) matchingInventoryEntry.entity.getPhysicalEntityComponent().getAttributes();
                int quantityAssignedToEntry = inventoryItemAttributes.getQuantity();
                quantityAssignedToEntry += quantityToAllocate;
                for (StockpileAllocation stockpileAllocation : allocationsByHaulingAllocationId.values()) {
                    if (stockpileAllocation.getItemType().equals(itemAttributes.getItemType()) &&
                            stockpileAllocation.getGameMaterial().equals(itemAttributes.getPrimaryMaterial())) {
                        quantityAssignedToEntry += stockpileAllocation.getIncomingHaulingQuantity();
                    }
                }

                if (quantityAssignedToEntry <= maxStackSize) {
                    StockpileAllocation allocationToUse = new StockpileAllocation(VectorUtils.toGridPoint(parentEntity.getLocationComponent().getWorldPosition()));
                    allocationToUse.setItemType(inventoryItemAttributes.getItemType());
                    allocationToUse.setGameMaterial(inventoryItemAttributes.getPrimaryMaterial());
                    return allocationToUse;
                }
            }
        }
        return null;
    }

    @Override
    protected StockpileAllocation createAllocation(TiledMap map, ItemType itemType, GameMaterial itemMaterial, Race corpseRace) {
        InventoryComponent inventoryComponent = parentEntity.getComponent(InventoryComponent.class);
        if (inventoryComponent != null && (inventoryComponent.getInventoryEntries().size() + allocationsByHaulingAllocationId.size()) < maxQuantity) {
            //if enough space then return one else return null
            StockpileAllocation allocationToUse = new StockpileAllocation(VectorUtils.toGridPoint(parentEntity.getLocationComponent().getWorldPosition()));
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