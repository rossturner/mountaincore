package technology.rocketjump.mountaincore.production;

import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CorpseBehaviour;
import technology.rocketjump.mountaincore.entities.components.ItemAllocationComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Race;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.mapping.model.TiledMap;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;

public abstract class AbstractStockpile {

    // Picks and allocates a position for the item
    public HaulingAllocation requestAllocation(Entity entity, TiledMap map, Entity requestingEntity) {
        boolean isItem = entity.getType().equals(EntityType.ITEM);
        boolean isCorpse = entity.getType().equals(EntityType.CREATURE) && entity.getBehaviourComponent() instanceof CorpseBehaviour;
        if (!isItem && !isCorpse) {
            return null;
        }

        ItemType itemType = isItem ? ((ItemEntityAttributes)entity.getPhysicalEntityComponent().getAttributes()).getItemType() : null;
        GameMaterial itemMaterial = isItem ? ((ItemEntityAttributes)entity.getPhysicalEntityComponent().getAttributes()).getPrimaryMaterial() : null;
        Race race = isCorpse ? ((CreatureEntityAttributes)entity.getPhysicalEntityComponent().getAttributes()).getRace() : null;
        final int maxStackSize = isCorpse ? 1 : itemType.getMaxStackSize();

        int numUnallocated = entity.getComponent(ItemAllocationComponent.class).getNumUnallocated();
        int quantityToAllocate = Math.min(numUnallocated, isCorpse ? 1 : itemType.getMaxHauledAtOnce());

        StockpileAllocation allocationToUse = findExistingAllocation(entity, map, maxStackSize, quantityToAllocate);

        if (allocationToUse == null) {
            // Not found one yet so use a new allocation
            allocationToUse = createAllocation(map, itemType, itemMaterial, race); //TODO: look into why parameters are so different for find and create
        }

        if (allocationToUse != null) {
            int spaceInAllocation = maxStackSize - allocationToUse.getTotalQuantity();
            if (quantityToAllocate == 0) {
                Logger.error("Quantity to requestAllocation in " + this.getClass().getSimpleName() + " is 0, investigate why");
                return null;
            }
            quantityToAllocate = Math.min(quantityToAllocate, spaceInAllocation);

            allocationToUse.incrementIncomingHaulingQuantity(quantityToAllocate);

            return createHaulingAllocation(entity, requestingEntity, quantityToAllocate, allocationToUse);
        }

        return null;
    }

    protected boolean matches(Entity entity, StockpileAllocation stockpileAllocation) {
        if (entity.getType().equals(EntityType.CREATURE)) {
            return ((CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes()).getRace().equals(stockpileAllocation.getRaceCorpse());
        } else if (entity.getType().equals(EntityType.ITEM)) {
            ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
            return attributes.getItemType().equals(stockpileAllocation.getItemType()) &&
                    attributes.getPrimaryMaterial().equals(stockpileAllocation.getGameMaterial());
        } else {
            return false;
        }
    }

    protected abstract StockpileAllocation findExistingAllocation(Entity entity, TiledMap map, int maxStackSize, int quantityToAllocate);
    protected abstract StockpileAllocation createAllocation(TiledMap map, ItemType itemType, GameMaterial itemMaterial, Race corpseRace);
    protected abstract HaulingAllocation createHaulingAllocation(Entity entity, Entity requestingEntity, int quantityToAllocate, StockpileAllocation allocationToUse);
}
