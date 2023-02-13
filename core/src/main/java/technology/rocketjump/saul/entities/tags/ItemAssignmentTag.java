package technology.rocketjump.saul.entities.tags;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import org.apache.commons.lang3.EnumUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.saul.entities.behaviour.items.ItemBehaviour;
import technology.rocketjump.saul.entities.components.BehaviourComponent;
import technology.rocketjump.saul.entities.components.EntityComponent;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.components.ItemAllocationComponent;
import technology.rocketjump.saul.entities.components.creature.ItemAssignmentComponent;
import technology.rocketjump.saul.entities.components.creature.MilitaryComponent;
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
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.rooms.HaulingAllocation;
import technology.rocketjump.saul.rooms.HaulingAllocationBuilder;

import java.util.Comparator;

import static technology.rocketjump.saul.entities.tags.ItemAssignmentTag.ItemAssignmentTagArg.PREFER_MILITARY;

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
            ItemAssignmentBehaviour itemAssignmentBehaviour = new ItemAssignmentBehaviour();
            if (args.size() > 0) {
                ItemAssignmentTagArg tagArg = EnumUtils.getEnum(ItemAssignmentTagArg.class, args.get(0));
                if (tagArg == null) {
                    Logger.error("Could not parse argument to " + getClass().getSimpleName());
                } else if (tagArg.equals(PREFER_MILITARY)) {
                    itemAssignmentBehaviour.setPreferMilitary(true);
                } else {
                    Logger.error("Not yet implemented: " + tagArg + " in " + getClass().getSimpleName());
                }
            }
            itemBehaviour.addAdditionalBehaviour(itemAssignmentBehaviour);
        }
    }

    public enum ItemAssignmentTagArg {

        PREFER_MILITARY

    }

    //TODO: Consider a more cutdown interface of just the update/updateWhenPaused/updateInfrequently
    private class ItemAssignmentBehaviour implements BehaviourComponent {
        private Entity parentEntity;
        private LocationComponent locationComponent;
        private MessageDispatcher messageDispatcher;
        private boolean preferMilitary;

        @Override
        public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
            ItemAssignmentBehaviour clone = new ItemAssignmentBehaviour();
            clone.init(parentEntity , messageDispatcher, gameContext);
            return clone;
        }

        @Override
        public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
            this.locationComponent = parentEntity.getLocationComponent(true);
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
					//Check they don't have one already in inventory
					//And don't have one already assigned
					//And don't have one already equipped
					//And are not currently hauling anything (e.g. another item assignment process)
					entities.stream()
							.filter(settler -> {
								//Check they don't have one already in inventory
								InventoryComponent inventoryComponent = settler.getComponent(InventoryComponent.class);
								if (inventoryComponent == null) {
									return true;
								} else {
									return inventoryComponent.findByItemType(itemTypeToAssign, gameContext.getGameClock()) == null;
								}
							})
							.filter(settler -> {
								//And don't have one already assigned
								ItemAssignmentComponent itemAssignmentComponent = settler.getComponent(ItemAssignmentComponent.class);
								if (itemAssignmentComponent == null) {
									return true;
								} else {
									return itemAssignmentComponent.findByItemType(itemTypeToAssign, gameContext) == null;
								}
							})
							.filter(settler -> {
								//And don't have one already equipped
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
							.filter(settler -> {
								//And are not currently hauling anything (e.g. another item assignment process)
								BehaviourComponent behavior = settler.getBehaviourComponent();
								if (behavior instanceof CreatureBehaviour creatureBehaviour) {
									if (creatureBehaviour.getCurrentGoal() != null) {
										return creatureBehaviour.getCurrentGoal().getAssignedHaulingAllocation() == null;
									}
								}
								return false;
							})
							.min(preferMilitary ? militaryPreferredSort : sortByDistance)
                            .ifPresent(settler -> {
                                HaulingAllocation haulingAllocation = HaulingAllocationBuilder.createWithItemAllocation(1, parentEntity, settler)
                                                .toEntity(settler);
                                ItemAssignmentComponent assignmentComponent = settler.getOrCreateComponent(ItemAssignmentComponent.class);
                                assignmentComponent.getHaulingAllocations().add(haulingAllocation);
                            });
                }));
            }
        }

		private final Comparator<Entity> sortByDistance = ((o1, o2) -> {
			Vector2 parentPosition = parentEntity.getLocationComponent(true).getWorldOrParentPosition();
			float o1Dist = o1.getLocationComponent(true).getWorldOrParentPosition().dst2(parentPosition);
			float o2Dist = o2.getLocationComponent(true).getWorldOrParentPosition().dst2(parentPosition);
			return (int)(1000f * (o2Dist - o1Dist));
		});

        private final Comparator<Entity> militaryPreferredSort = ((Comparator<Entity>)(o1, o2) -> Boolean.compare(isInMilitary(o2), isInMilitary(o1)))
                    .thenComparing(sortByDistance);


        private boolean isInMilitary(Entity entity) {
            MilitaryComponent militaryComponent = entity.getComponent(MilitaryComponent.class);
            if (militaryComponent != null) {
                return militaryComponent.isInMilitary();
            } else {
                return false;
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

        public void setPreferMilitary(boolean preferMilitary) {
            this.preferMilitary = preferMilitary;
        }

        public boolean isPreferMilitary() {
            return preferMilitary;
        }

        @Override
        public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
            if (preferMilitary) {
                asJson.put("preferMilitary", true);
            }
        }

        @Override
        public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
            this.preferMilitary = asJson.getBooleanValue("preferMilitary");
        }
    }
}
