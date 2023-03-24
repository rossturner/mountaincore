package technology.rocketjump.mountaincore.rooms.components;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.RandomXS128;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CorpseBehaviour;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Race;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.mapping.model.TiledMap;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.production.AbstractStockpile;
import technology.rocketjump.mountaincore.production.StockpileAllocation;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;
import technology.rocketjump.mountaincore.rooms.HaulingAllocationBuilder;
import technology.rocketjump.mountaincore.rooms.Room;

import java.util.*;

public class RoomStockpile extends AbstractStockpile {
    private final Room room;
    // This keeps track of allocations - null for empty spaces
    private final Map<GridPoint2, StockpileAllocation> allocations = new HashMap<>();

    RoomStockpile(Room room) {
        this.room = room;
    }

    @Override
    protected StockpileAllocation findExistingAllocation(Entity entity, TiledMap map, int maxStackSize, int quantityToAllocate) {
        List<GridPoint2> pointsToTraverse = new ArrayList<>(room.getRoomTiles().keySet());
        // Randomly traverse to see if we can fit into existing
        Collections.shuffle(pointsToTraverse);
        // First try to find a matching allocation
        for (GridPoint2 position : pointsToTraverse) {
            MapTile tileAtPosition = map.getTile(position);
            StockpileAllocation allocationAtPosition = allocations.get(position);
            if (allocationAtPosition == null) {
                // No allocation here yet
                if (!tileAtPosition.isEmpty()) {
                    Entity itemAlreadyInTile = tileAtPosition.getFirstItem();
                    if (itemAlreadyInTile != null) {
                        // There is already an item here but no existing allocation, so add a new allocation matching it
                        // This is for pre-existing items where a stockpile is placed
                        ItemEntityAttributes attributesItemAlreadyInTile = (ItemEntityAttributes) itemAlreadyInTile.getPhysicalEntityComponent().getAttributes();

                        allocationAtPosition = new StockpileAllocation(position);
                        allocationAtPosition.setGameMaterial(attributesItemAlreadyInTile.getPrimaryMaterial());
                        allocationAtPosition.setItemType(attributesItemAlreadyInTile.getItemType());
                        allocationAtPosition.refreshQuantityInTile(tileAtPosition);
                        allocations.put(position, allocationAtPosition);
                        continue;
                    }

                    Entity corpseEntity = tileAtPosition.getFirstCorpse();
                    if (corpseEntity != null) {
                        allocationAtPosition = new StockpileAllocation(position);
                        allocationAtPosition.setRaceCorpse(((CreatureEntityAttributes) corpseEntity.getPhysicalEntityComponent().getAttributes()).getRace());
                        allocationAtPosition.refreshQuantityInTile(tileAtPosition);
                        allocations.put(position, allocationAtPosition);
                        continue;
                    }
                }
            } else if (matches(entity, allocationAtPosition) &&
                    allocationAtPosition.getTotalQuantity() + quantityToAllocate <= maxStackSize &&
                    allocationIsCorrectForTileContents(tileAtPosition, allocationAtPosition)) {
                return allocationAtPosition;
            }
        }
        return null;
    }

    @Override
    protected StockpileAllocation createAllocation(TiledMap map, ItemType itemType, GameMaterial itemMaterial, Race corpseRace) {
        // Deterministically go through points to traverse for a new location
        List<GridPoint2> pointsToTraverse = new ArrayList<>(room.getRoomTiles().keySet());
        Random random = new RandomXS128(room.getRoomId());
        Collections.shuffle(pointsToTraverse, random);

        for (GridPoint2 position : pointsToTraverse) {
            MapTile tileAtPosition = map.getTile(position);
            StockpileAllocation allocationAtPosition = allocations.get(position);
            if (allocationAtPosition == null) {
                if (tileAtPosition.isEmpty()) {
                    StockpileAllocation allocationToUse = new StockpileAllocation(position);
                    allocationToUse.setItemType(itemType);
                    allocationToUse.setGameMaterial(itemMaterial);
                    allocationToUse.setRaceCorpse(corpseRace);
                    allocations.put(position, allocationToUse);
                    return allocationToUse;
                }
            }
        }
        return null;
    }

    @Override
    protected HaulingAllocation createHaulingAllocation(Entity entity, Entity requestingEntity, int quantityToAllocate, StockpileAllocation allocationToUse) {
        return HaulingAllocationBuilder.createWithItemAllocation(quantityToAllocate, entity, requestingEntity)
                .toRoom(room, allocationToUse.getPosition());
    }


    private boolean allocationIsCorrectForTileContents(MapTile tileAtPosition, StockpileAllocation allocationAtPosition) {
        Entity itemAtPosition = null;
        Entity corpseAtPosition = null;
        for (Entity entity : tileAtPosition.getEntities()) {
            if (entity.getType().equals(EntityType.PLANT)) {
                return false; // a plant has grown into the tile
            }
            if (entity.getType().equals(EntityType.ITEM)) {
                itemAtPosition = entity;
                break;
            }
            if (entity.getType().equals(EntityType.CREATURE) && entity.getBehaviourComponent() instanceof CorpseBehaviour) {
                corpseAtPosition = entity;
                break;
            }
        }

        if (itemAtPosition == null && corpseAtPosition == null) {
            return true; // nothing here so can place allocation
        } else if (corpseAtPosition != null) {
            return allocationAtPosition.getRaceCorpse() != null && allocationAtPosition.getRaceCorpse().equals(((CreatureEntityAttributes) corpseAtPosition.getPhysicalEntityComponent().getAttributes()).getRace());
        } else {
            ItemEntityAttributes attributes = (ItemEntityAttributes) itemAtPosition.getPhysicalEntityComponent().getAttributes();
            return attributes.getItemType().equals(allocationAtPosition.getItemType()) &&
                    attributes.getPrimaryMaterial().equals(allocationAtPosition.getGameMaterial());
        }
    }

    public Map<GridPoint2, StockpileAllocation> getAllocations() {
        return allocations;
    }
}
