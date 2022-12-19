package technology.rocketjump.saul.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.entities.model.ColoringLayer;
import technology.rocketjump.saul.cooking.model.FoodAllocation;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.behaviour.furniture.EdibleLiquidSourceBehaviour;
import technology.rocketjump.saul.entities.components.*;
import technology.rocketjump.saul.entities.components.creature.HappinessComponent;
import technology.rocketjump.saul.entities.components.creature.MilitaryComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.creature.Consciousness;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.HaulingComponent;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.QuantifiedItemType;
import technology.rocketjump.saul.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.model.JobPriority;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.ItemCreationRequestMessage;
import technology.rocketjump.saul.messaging.types.ItemPrimaryMaterialChangedMessage;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.rooms.HaulingAllocation;
import technology.rocketjump.saul.rooms.components.StockpileRoomComponent;
import technology.rocketjump.saul.rooms.constructions.Construction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static technology.rocketjump.saul.cooking.model.FoodAllocation.FoodAllocationType.REQUESTER_INVENTORY;
import static technology.rocketjump.saul.entities.FurnitureEntityMessageHandler.otherColorsToCopy;
import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.SUCCESS;
import static technology.rocketjump.saul.entities.components.creature.HappinessComponent.HappinessModifier.CARRIED_DEAD_BODY;
import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.saul.rooms.HaulingAllocation.AllocationPositionType.FURNITURE;

public class PickUpEntityAction extends Action implements EntityCreatedCallback {
	public PickUpEntityAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (parent.getFoodAllocation() != null) {
			updateForFoodAllocation(gameContext);
		} else if (parent.getAssignedHaulingAllocation() != null) {
			updateForHaulingAllocation(gameContext);
		}
	}

	private void updateForFoodAllocation(GameContext gameContext) {
		MapTile currentTile = gameContext.getAreaMap().getTile(parent.parentEntity.getLocationComponent().getWorldPosition());
		FoodAllocation foodAllocation = parent.getFoodAllocation();
		switch (foodAllocation.getType()) {
			case REQUESTER_INVENTORY:
				// Already in inventory, nothing to do
				completionType = SUCCESS;
				break;
			case LIQUID_CONTAINER: {
				Entity containerEntity = foodAllocation.getTargetEntity();
				if (containerEntity != null) {
					if (isAdjacent(containerEntity)) {
						BehaviourComponent behaviourComponent = containerEntity.getBehaviourComponent();
						if (behaviourComponent instanceof EdibleLiquidSourceBehaviour) {
							EdibleLiquidSourceBehaviour edibleLiquidSourceBehaviour = (EdibleLiquidSourceBehaviour) behaviourComponent;
							Entity item = edibleLiquidSourceBehaviour.createItem(foodAllocation.getLiquidAllocation(), gameContext);
							if (item == null) {
								completionType = FAILURE;
							} else {
								parent.getFoodAllocation().setTargetEntity(item);
								parent.getFoodAllocation().setLiquidAllocation(null);

								InventoryComponent inventoryComponent = parent.parentEntity.getOrCreateComponent(InventoryComponent.class);
								inventoryComponent.add(item, parent.parentEntity, parent.messageDispatcher, gameContext.getGameClock());
								completionType = SUCCESS;
							}
						} else {
							Logger.error("not implemented: Picking item from liquid container without " + EdibleLiquidSourceBehaviour.class.getSimpleName());
							completionType = FAILURE;
						}
					} else {
						completionType = FAILURE;
					}
				} else {
					Logger.error("Container entity for " + foodAllocation.getType() + " food allocation is null");
					completionType = FAILURE;
				}
				break;
			}
			case FURNITURE_INVENTORY: {
				Entity entityToPickUp = foodAllocation.getTargetEntity();
				Entity containerEntity = foodAllocation.getTargetEntity().getLocationComponent().getContainerEntity();
				if (containerEntity != null) {
					if (isAdjacent(containerEntity)) {
						pickUpItemEntity(gameContext, currentTile, entityToPickUp, foodAllocation.getItemAllocaton());
					} else {
						completionType = FAILURE;
					}
				} else {
					Logger.error("Container entity for " + foodAllocation.getType() + " food allocation is null");
					completionType = FAILURE;
				}
				break;
			}
			case LOOSE_ITEM: {
				// Pick up as a normal item, but only if in same tile
				Entity containerEntity = foodAllocation.getTargetEntity().getLocationComponent().getContainerEntity();
				if (containerEntity == null) {
					boolean entityInCurrentTile = currentTile.getEntity(foodAllocation.getTargetEntity().getId()) != null;
					if (entityInCurrentTile || isAdjacent(foodAllocation.getTargetEntity())) {
						pickUpItemEntity(gameContext, currentTile, foodAllocation.getTargetEntity(), foodAllocation.getItemAllocaton());
					} else {
						completionType = FAILURE;
					}
				} else {
					if (isAdjacent(containerEntity)) {
						pickUpItemEntity(gameContext, currentTile, foodAllocation.getTargetEntity(), foodAllocation.getItemAllocaton());
					} else {
						completionType = FAILURE;
					}
				}
				break;
			}
			default: {
				Logger.error("Unrecognised " + foodAllocation.getClass().getSimpleName() + " type in " + this.getSimpleName());
				completionType = FAILURE;
			}
		}
	}

	private boolean isAdjacent(Entity containerEntity) {
		GridPoint2 parentPosition = toGridPoint(parent.parentEntity.getLocationComponent().getWorldPosition());
		GridPoint2 containerPosition = toGridPoint(containerEntity.getLocationComponent().getWorldOrParentPosition());
		if (containerEntity.getType().equals(EntityType.FURNITURE)) {
			if (adjacent(containerPosition, parentPosition)) {
				return true;
			} else {
				// Try other locations
				FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) containerEntity.getPhysicalEntityComponent().getAttributes();
				for (GridPoint2 extraTile : attributes.getCurrentLayout().getExtraTiles()) {
					if (adjacent(extraTile.cpy().add(containerPosition), parentPosition)) {
						return true;
					}
				}
				return false;
			}
		} else {
			return adjacent(containerPosition, parentPosition);
		}
	}

	private boolean adjacent(GridPoint2 position1, GridPoint2 position2) {
		if (position1 == null || position2 == null) {
			return false;
		} else {
			return Math.abs(position1.x - position2.x) <= 1 && Math.abs(position1.y - position2.y) <= 1;
		}
	}

	private void updateForHaulingAllocation(GameContext gameContext) {
		MapTile currentTile = gameContext.getAreaMap().getTile(parent.parentEntity.getLocationComponent().getWorldPosition());

		// Target item entity should be in this tile
		Entity entityToPickUp = null;
		Entity containerEntity = null;
		HaulingAllocation haulingAllocation = parent.getAssignedHaulingAllocation();
		MapTile sourcePositionTile = gameContext.getAreaMap().getTile(haulingAllocation.getSourcePosition());
		if (haulingAllocation.getHauledEntityType().equals(EntityType.FURNITURE)) {
			Entity sourceEntity = sourcePositionTile.getEntity(haulingAllocation.getHauledEntityId());
			if (sourceEntity == null) {
				Logger.error("Could not find container entity to pick up item from");
				completionType = FAILURE;
				return;
			}
			entityToPickUp = createItemFromEntireFurniture(sourceEntity, parent.messageDispatcher, gameContext);
		} else if (FURNITURE.equals(haulingAllocation.getSourcePositionType())) {
			containerEntity = sourcePositionTile.getEntity(haulingAllocation.getSourceContainerId());
			if (containerEntity == null) {
				Logger.error("Could not find container entity to pick up item from");
				completionType = FAILURE;
				return;
			}

			InventoryComponent inventory = containerEntity.getComponent(InventoryComponent.class);
			if (inventory != null) {
				entityToPickUp = inventory.getById(haulingAllocation.getHauledEntityId());
			}
		} else {
			entityToPickUp = currentTile.getEntity(haulingAllocation.getHauledEntityId());
		}

		if (entityToPickUp == null) {
			completionType = FAILURE;
		} else if (haulingAllocation.getHauledEntityType().equals(EntityType.CREATURE)) {
			pickUpCreatureEntity(entityToPickUp, gameContext);
		} else {
			pickUpItemEntity(gameContext, currentTile, entityToPickUp, haulingAllocation.getItemAllocation());
		}
		if (containerEntity != null) {
			parent.messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, containerEntity);
		}
	}

	private void pickUpCreatureEntity(Entity entityToPickUp, GameContext gameContext) {
		// Take inventory of entityToPickUp
		InventoryComponent pickedUpEntityInventoryComponent = entityToPickUp.getOrCreateComponent(InventoryComponent.class);
		InventoryComponent parentInventoryComponent = parent.parentEntity.getOrCreateComponent(InventoryComponent.class);

		for (InventoryComponent.InventoryEntry entry : new ArrayList<>(pickedUpEntityInventoryComponent.getInventoryEntries())) {
			pickedUpEntityInventoryComponent.remove(entry.entity.getId());
			parentInventoryComponent.add(entry.entity, parent.parentEntity, parent.messageDispatcher, gameContext.getGameClock());
		}

		CreatureEntityAttributes attributesOfEntityToPickUp = (CreatureEntityAttributes) entityToPickUp.getPhysicalEntityComponent().getAttributes();
		if (attributesOfEntityToPickUp.getConsciousness().equals(Consciousness.DEAD) && sameRace(attributesOfEntityToPickUp)) {
			HappinessComponent parentHappinessComponent = parent.parentEntity.getOrCreateComponent(HappinessComponent.class);
			parentHappinessComponent.add(CARRIED_DEAD_BODY);
		}

		ItemAllocationComponent itemAllocationComponent = entityToPickUp.getComponent(ItemAllocationComponent.class);
		if (itemAllocationComponent != null) {
			itemAllocationComponent.cancelAll();
		}

		HaulingComponent haulingComponent = parent.parentEntity.getOrCreateComponent(HaulingComponent.class);
		haulingComponent.setHauledEntity(entityToPickUp, parent.messageDispatcher, parent.parentEntity);
		completionType = SUCCESS;

		MapTile currentTile = gameContext.getAreaMap().getTile(parent.parentEntity.getLocationComponent().getWorldPosition());
		if (currentTile.getRoomTile() != null && currentTile.getRoomTile().getRoom().getComponent(StockpileRoomComponent.class) != null) {
			StockpileRoomComponent stockpileRoomComponent = currentTile.getRoomTile().getRoom().getComponent(StockpileRoomComponent.class);
			stockpileRoomComponent.itemOrCreaturePickedUp(currentTile);
		}
	}

	private void pickUpItemEntity(GameContext gameContext, MapTile currentTile, Entity entityToPickUp, ItemAllocation itemAllocation) {
		int quantityToPick = itemAllocation.getAllocationAmount();

		ItemEntityAttributes targetItemAttributes = (ItemEntityAttributes) entityToPickUp.getPhysicalEntityComponent().getAttributes();
		if (targetItemAttributes.getQuantity() < quantityToPick) {
			completionType = FAILURE;
		} else {

			if (quantityToPick > targetItemAttributes.getItemType().getMaxHauledAtOnce()) {
				// Want to haul more items than able to carry at once
				quantityToPick = targetItemAttributes.getItemType().getMaxHauledAtOnce();
				// Reduce item allocation quantity - will be picked up on by construction or whatever
				itemAllocation.setAllocationAmount(quantityToPick);
			}

			Entity pickedUpItem = entityToPickUp.clone(parent.messageDispatcher, gameContext);
			pickedUpItem.getLocationComponent().clearWorldPosition();
			pickedUpItem.getOrCreateComponent(ItemAllocationComponent.class).cancelAll();
			pickedUpItem.getComponent(FactionComponent.class).setFaction(parent.parentEntity.getOrCreateComponent(FactionComponent.class).getFaction());

			ItemEntityAttributes cloneAttributes = (ItemEntityAttributes) pickedUpItem.getPhysicalEntityComponent().getAttributes();
			cloneAttributes.setQuantity(quantityToPick);


			// Need to do this before adding to inventory, as it may be destroyed if added to an existing inventory item
			parent.messageDispatcher.dispatchMessage(MessageType.ENTITY_CREATED, pickedUpItem);

			if ((parent.getAssignedJob() != null && (parent.getAssignedJob().getType().isHaulItemWhileWorking())) ||
				parent.getAssignedHaulingAllocation() != null) {
				HaulingComponent haulingComponent = parent.parentEntity.getOrCreateComponent(HaulingComponent.class);
				haulingComponent.setHauledEntity(pickedUpItem, parent.messageDispatcher, parent.parentEntity);
			} else {
				InventoryComponent inventoryComponent = parent.parentEntity.getComponent(InventoryComponent.class);
				InventoryComponent.InventoryEntry inventoryEntry = inventoryComponent.add(pickedUpItem, parent.parentEntity, parent.messageDispatcher, gameContext.getGameClock());
				// Might have merged into a different entity so need to update target ID
				pickedUpItem = inventoryEntry.entity;
			}

			entityToPickUp.getComponent(ItemAllocationComponent.class).cancel(itemAllocation);
			targetItemAttributes.setQuantity(targetItemAttributes.getQuantity() - quantityToPick);

			if (currentTile.getRoomTile() != null && currentTile.getRoomTile().getRoom().getComponent(StockpileRoomComponent.class) != null) {
				StockpileRoomComponent stockpileRoomComponent = currentTile.getRoomTile().getRoom().getComponent(StockpileRoomComponent.class);
				stockpileRoomComponent.itemOrCreaturePickedUp(currentTile);
			}

			if (targetItemAttributes.getQuantity() <= 0) {
				parent.messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, entityToPickUp);
			} else {
				parent.messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, entityToPickUp);
			}

			updateAllocationTarget(pickedUpItem);
			updateSpecificAssignments(entityToPickUp.getId(), pickedUpItem.getId());

			completionType = SUCCESS;
		}
	}

	private void updateSpecificAssignments(long oldId, long newId) {
		MilitaryComponent militaryComponent = parent.parentEntity.getComponent(MilitaryComponent.class);
		if (militaryComponent != null) {
			if (militaryComponent.getAssignedWeaponId() != null && militaryComponent.getAssignedWeaponId() == oldId) {
				militaryComponent.setAssignedWeaponId(newId);
			}
			if (militaryComponent.getAssignedShieldId() != null && militaryComponent.getAssignedShieldId() == oldId) {
				militaryComponent.setAssignedShieldId(newId);
			}
			if (militaryComponent.getAssignedArmorId() != null && militaryComponent.getAssignedArmorId() == oldId) {
				militaryComponent.setAssignedArmorId(newId);
			}
		}
	}

	private void updateAllocationTarget(Entity clonedItem) {
		if (parent.getFoodAllocation() != null) {
			parent.getFoodAllocation().setTargetEntity(clonedItem);
			parent.getFoodAllocation().setType(REQUESTER_INVENTORY);
			ItemAllocationComponent itemAllocationComponent = clonedItem.getOrCreateComponent(ItemAllocationComponent.class);
			parent.getFoodAllocation().setItemAllocaton(itemAllocationComponent.getAll().get(0));
		} else if (parent.getAssignedHaulingAllocation() != null) {
			parent.getAssignedHaulingAllocation().setHauledEntityId(clonedItem.getId());
			ItemAllocationComponent itemAllocationComponent = clonedItem.getOrCreateComponent(ItemAllocationComponent.class);
			List<ItemAllocation> all = itemAllocationComponent.getAll();
			if (all.size() == 1) {
				parent.getAssignedHaulingAllocation().setItemAllocation(all.get(0));
			} else {
				Logger.error("Expecting picked up item to only have one ItemAllocation");
			}
		}
	}

	private Entity createItemFromEntireFurniture(Entity furnitureEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		FurnitureEntityAttributes furnitureAttributes = (FurnitureEntityAttributes) furnitureEntity.getPhysicalEntityComponent().getAttributes();
		List<QuantifiedItemType> requirements = furnitureAttributes.getFurnitureType().getRequirements().get(furnitureAttributes.getPrimaryMaterialType());
		if (furnitureAttributes.getFurnitureType().isAutoConstructed()) {
			if (requirements.size() == 1) {
				messageDispatcher.dispatchMessage(MessageType.ITEM_CREATION_REQUEST, new ItemCreationRequestMessage(requirements.get(0).getItemType(), this));
				if (createdItem != null) {
					createdItem.getLocationComponent().setWorldPosition(furnitureEntity.getLocationComponent().getWorldPosition(), false);
					copyMaterialsAndContents(furnitureEntity, createdItem, messageDispatcher, gameContext);
					messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, furnitureEntity);

					/*
					The following line removes the behaviour from the furniture as it is about to be cloned in the FURNITURE_PLACEMENT message
					Cloning the entity re-initialises a liquid container attached zone, which is unwanted behaviour in this case. A bit of a code smell
					 */
					furnitureEntity.replaceBehaviourComponent(null);

					messageDispatcher.dispatchMessage(MessageType.FURNITURE_PLACEMENT, furnitureEntity); // Queue up new construction of furnitureEntity
					Construction construction = gameContext.getAreaMap().getTile(furnitureEntity.getLocationComponent().getWorldPosition()).getConstruction();
					removeMaterialsFromRequirements(construction);
					construction.setPriority(JobPriority.HIGHER, messageDispatcher);

					parent.getAssignedHaulingAllocation().setHauledEntityId(createdItem.getId());

					ItemAllocationComponent itemAllocationComponent = createdItem.getOrCreateComponent(ItemAllocationComponent.class);
					ItemEntityAttributes attributes = (ItemEntityAttributes) createdItem.getPhysicalEntityComponent().getAttributes();
					ItemAllocation itemAllocation = itemAllocationComponent.createAllocation(attributes.getQuantity(), parent.parentEntity, ItemAllocation.Purpose.DUE_TO_BE_HAULED);
					parent.getAssignedHaulingAllocation().setItemAllocation(itemAllocation);
				}
				return createdItem;
			} else {
				Logger.error("Attempting to pick up furniture with multiple item requirements");
			}
		} else {
			Logger.error("Attempting to pick up non-auto-constructed furniture");
		}

		return null;
	}

	private void removeMaterialsFromRequirements(Construction construction) {
		for (QuantifiedItemTypeWithMaterial requirement : construction.getRequirements()) {
			requirement.setMaterial(null);
			requirement.setMaterialName(null);
		}
	}

	private void copyMaterialsAndContents(Entity furnitureEntity, Entity itemEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		FurnitureEntityAttributes furnitureAttributes = (FurnitureEntityAttributes) furnitureEntity.getPhysicalEntityComponent().getAttributes();
		ItemEntityAttributes itemAttributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();
		GameMaterial oldPrimaryMaterial = itemAttributes.getPrimaryMaterial();

		for (GameMaterial gameMaterial : furnitureAttributes.getMaterials().values()) {
			itemAttributes.setMaterial(gameMaterial);
		}

		if (!oldPrimaryMaterial.equals(itemAttributes.getPrimaryMaterial())) {
			messageDispatcher.dispatchMessage(MessageType.ITEM_PRIMARY_MATERIAL_CHANGED, new ItemPrimaryMaterialChangedMessage(itemEntity, oldPrimaryMaterial));
		}
		for (ColoringLayer coloringLayer : otherColorsToCopy) {
			itemAttributes.setColor(coloringLayer, furnitureAttributes.getColor(coloringLayer));
		}

		for (Class<? extends EntityComponent> componentClass : Arrays.asList(LiquidContainerComponent.class, InventoryComponent.class)) {
			EntityComponent component = furnitureEntity.getComponent(componentClass);
			if (component != null) {
				EntityComponent cloned = component.clone(messageDispatcher, gameContext);
				itemEntity.addComponent(component.clone(messageDispatcher, gameContext));
				if (cloned instanceof ParentDependentEntityComponent) {
					((ParentDependentEntityComponent)cloned).init(itemEntity, messageDispatcher, gameContext);
				}
			}
		}
	}

	private Entity createdItem = null; // FIXME Don't think this needs persisting, but need to check use is synchronous

	@Override
	public void entityCreated(Entity entity) {
		this.createdItem = entity;
	}

	private boolean sameRace(CreatureEntityAttributes targetCreature) {
		return targetCreature.getRace().equals(((CreatureEntityAttributes)parent.parentEntity.getPhysicalEntityComponent().getAttributes()).getRace());
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		// No state to write
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		// No state to read
	}
}
