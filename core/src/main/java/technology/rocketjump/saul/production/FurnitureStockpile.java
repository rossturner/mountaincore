package technology.rocketjump.saul.production;

import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.mapping.model.TiledMap;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.misc.VectorUtils;
import technology.rocketjump.saul.rooms.HaulingAllocation;
import technology.rocketjump.saul.rooms.HaulingAllocationBuilder;

import java.util.ArrayList;
import java.util.List;

public class FurnitureStockpile extends AbstractStockpile {
    private final Entity parentEntity;
    private final List<StockpileAllocation> allocations = new ArrayList<>(100);

    public FurnitureStockpile(Entity parentEntity) {
        this.parentEntity = parentEntity;
    }


    @Override
    protected StockpileAllocation findExistingAllocation(Entity entity, TiledMap map, int maxStackSize, int quantityToAllocate) {
        return null;
    }

    @Override
    protected StockpileAllocation createAllocation(TiledMap map, ItemType itemType, GameMaterial itemMaterial, Race corpseRace) {
        //if enough space then return one else return null
        StockpileAllocation allocationToUse = new StockpileAllocation(VectorUtils.toGridPoint(parentEntity.getLocationComponent().getWorldPosition()));
        allocationToUse.setItemType(itemType);
        allocationToUse.setGameMaterial(itemMaterial);
        allocationToUse.setRaceCorpse(corpseRace);
        allocations.add(allocationToUse);
        return allocationToUse;
    }

    @Override
    protected HaulingAllocation createHaulingAllocation(Entity entity, Entity requestingEntity, int quantityToAllocate, StockpileAllocation allocationToUse) {
        return HaulingAllocationBuilder.createWithItemAllocation(quantityToAllocate, entity, requestingEntity)
                .toEntity(parentEntity);
    }
}