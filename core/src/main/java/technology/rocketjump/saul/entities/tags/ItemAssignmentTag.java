package technology.rocketjump.saul.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.saul.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.saul.entities.behaviour.items.ItemBehaviour;
import technology.rocketjump.saul.entities.components.*;
import technology.rocketjump.saul.entities.components.creature.ItemAssignmentComponent;
import technology.rocketjump.saul.entities.components.creature.SteeringComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.LocationComponent;
import technology.rocketjump.saul.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.LocateSettlersMessage;
import technology.rocketjump.saul.misc.VectorUtils;
import technology.rocketjump.saul.rooms.HaulingAllocation;

/**
 * Attach to an item for automatic inventory allocation
 */
public class ItemAssignmentTag extends Tag {
    @Override
    public String getTagName() {
        return "ITEM_ASSIGNMENT";
    }

    @Override
    public boolean isValid(TagProcessingUtils tagProcessingUtils) {
        return true;
    }

    @Override
    public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
        BehaviourComponent behaviourComponent = entity.getBehaviourComponent();
        if (behaviourComponent instanceof ItemBehaviour itemBehaviour) {
            itemBehaviour.addAdditionalBehaviour(new ItemAssignmentBehaviour());
        }
    }

    //TODO: Consider a more cutdown interface of just the update/updateWhenPaused/updateInfrequently
    private class ItemAssignmentBehaviour implements BehaviourComponent {
        private Entity parentEntity;
        private LocationComponent locationComponent;
        private MessageDispatcher messageDispatcher;

        @Override
        public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
            ItemAssignmentBehaviour clone = new ItemAssignmentBehaviour();
            clone.init(parentEntity , messageDispatcher, gameContext);
            return null;
        }

        @Override
        public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
            this.locationComponent = parentEntity.getLocationComponent();
            this.messageDispatcher = messageDispatcher;
            this.parentEntity = parentEntity;
        }


        @Override
        public void update(float deltaTime) {

        }

        @Override
        public void updateWhenPaused() {

        }

        @Override
        public void infrequentUpdate(GameContext gameContext) {
            ItemEntityAttributes attributes = (ItemEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
            ItemAllocationComponent itemAllocationComponent = parentEntity.getComponent(ItemAllocationComponent.class);
            Vector2 worldPosition = locationComponent.getWorldPosition();
            MapTile tile = gameContext.getAreaMap().getTile(worldPosition);

            if (worldPosition != null && attributes.getItemPlacement().equals(ItemPlacement.ON_GROUND) && itemAllocationComponent.getNumUnallocated() > 0) {
                int regionId = tile.getRegionId();

                ItemType itemTypeToAssign = attributes.getItemType();
                messageDispatcher.dispatchMessage(MessageType.LOCATE_SETTLERS_IN_REGION, new LocateSettlersMessage(regionId, entities -> {
                    entities.stream()
                            .filter(settler -> {
                                InventoryComponent inventoryComponent = settler.getComponent(InventoryComponent.class);
                                if (inventoryComponent == null) {
                                    return true;
                                } else {
                                    return inventoryComponent.findByItemType(itemTypeToAssign, gameContext.getGameClock()) == null;
                                }
                            })
                            .filter(settler -> {
                                ItemAssignmentComponent itemAssignmentComponent = settler.getComponent(ItemAssignmentComponent.class);
                                if (itemAssignmentComponent == null) {
                                    return true;
                                } else {
                                    return itemAssignmentComponent.findByItemType(itemTypeToAssign, gameContext) == null;
                                }
                            })
                            .filter(settler -> {
                                EquippedItemComponent equippedItemComponent = settler.getComponent(EquippedItemComponent.class);
                                if (equippedItemComponent == null) {
                                    return true;
                                } else {
                                    Entity mainHandItem = equippedItemComponent.getMainHandItem();
                                    if (mainHandItem != null && mainHandItem.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes equippedItemAttributes) {
                                        return !equippedItemAttributes.getItemType().equals(itemTypeToAssign);
                                    } else {
                                        return true;
                                    }
                                }
                            })
                            .findFirst()
                            .ifPresent(settler -> {
                                ItemAllocation itemAllocation = itemAllocationComponent.createAllocation(1, settler, ItemAllocation.Purpose.DUE_TO_BE_HAULED);
                                HaulingAllocation haulingAllocation = new HaulingAllocation();
                                haulingAllocation.setItemAllocation(itemAllocation);
                                haulingAllocation.setSourcePositionType(HaulingAllocation.AllocationPositionType.FLOOR);
                                haulingAllocation.setSourcePosition(VectorUtils.toGridPoint(worldPosition));
                                haulingAllocation.setHauledEntityId(this.parentEntity.getId());

                                ItemAssignmentComponent assignmentComponent = settler.getOrCreateComponent(ItemAssignmentComponent.class);
                                assignmentComponent.getHaulingAllocations().add(haulingAllocation);
                            });
                }));
            }
        }

        @Override
        public SteeringComponent getSteeringComponent() {
            return null;
        }

        @Override
        public boolean isUpdateEveryFrame() {
            return false;
        }

        @Override
        public boolean isUpdateInfrequently() {
            return true;
        }

        @Override
        public boolean isJobAssignable() {
            return false;
        }

    }
}
