package technology.rocketjump.saul.entities.behaviour.creature;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.saul.entities.ai.combat.EnteringCombatException;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.memory.Memory;
import technology.rocketjump.saul.entities.ai.memory.MemoryType;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.components.ItemAllocation;
import technology.rocketjump.saul.entities.components.ItemAllocationComponent;
import technology.rocketjump.saul.entities.components.LiquidAllocation;
import technology.rocketjump.saul.entities.components.creature.CombatStateComponent;
import technology.rocketjump.saul.entities.components.creature.HappinessComponent;
import technology.rocketjump.saul.entities.components.creature.MemoryComponent;
import technology.rocketjump.saul.entities.components.creature.MilitaryComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.HaulingComponent;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.tile.CompassDirection;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.RequestLiquidAllocationMessage;
import technology.rocketjump.saul.rooms.HaulingAllocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static technology.rocketjump.saul.entities.ItemEntityMessageHandler.findStockpileAllocation;
import static technology.rocketjump.saul.entities.ai.goap.SpecialGoal.*;
import static technology.rocketjump.saul.entities.components.ItemAllocation.Purpose.DUE_TO_BE_HAULED;
import static technology.rocketjump.saul.entities.components.ItemAllocation.Purpose.HELD_IN_INVENTORY;
import static technology.rocketjump.saul.entities.model.EntityType.*;
import static technology.rocketjump.saul.entities.model.physical.creature.Consciousness.DEAD;
import static technology.rocketjump.saul.entities.model.physical.creature.Consciousness.KNOCKED_UNCONSCIOUS;
import static technology.rocketjump.saul.misc.VectorUtils.toVector;

public class AssignedGoalFactory {

	// MODDING expose these
	private static final float MAX_DISTANCE_TO_LIQUID_FOR_DOUSE_FIRE = 12f;
	private static final float AMOUNT_REQUIRED_TO_DOUSE_FIRE = 0.5f;


	public static AssignedGoal placeHauledItemGoal(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		Entity hauledEntity = parentEntity.getComponent(HaulingComponent.class).getHauledEntity();
		// need somewhere to place it

		HaulingAllocation stockpileAllocation = null;
		// Special case - if recently attempted to place item and failed, just dump it instead
		boolean recentlyFailedPlaceItemGoal = parentEntity.getOrCreateComponent(MemoryComponent.class)
				.getShortTermMemories(gameContext.getGameClock())
				.stream()
				.anyMatch(m -> m.getType().equals(MemoryType.FAILED_GOAL) && PLACE_ITEM.goalName.equals(m.getRelatedGoalName()));

		if (!recentlyFailedPlaceItemGoal) {
			// Temp un-requestAllocation
			ItemAllocationComponent itemAllocationComponent = hauledEntity.getComponent(ItemAllocationComponent.class);
			if (itemAllocationComponent == null) {
				itemAllocationComponent = new ItemAllocationComponent();
				itemAllocationComponent.init(hauledEntity, messageDispatcher, gameContext);
				hauledEntity.addComponent(itemAllocationComponent);
			}
			itemAllocationComponent.cancelAll(ItemAllocation.Purpose.HAULING);

			stockpileAllocation = findStockpileAllocation(gameContext.getAreaMap(), hauledEntity, parentEntity, messageDispatcher);

			if (stockpileAllocation != null && stockpileAllocation.getItemAllocation() != null) {
				// Stockpile allocation found, swap from DUE_TO_BE_HAULED
				ItemAllocation newAllocation = itemAllocationComponent.swapAllocationPurpose(DUE_TO_BE_HAULED, ItemAllocation.Purpose.HAULING, stockpileAllocation.getItemAllocation().getAllocationAmount());
				stockpileAllocation.setItemAllocation(newAllocation);
			}

			// Always re-allocate remaining amount to hauling
			if (itemAllocationComponent.getNumUnallocated() > 0) {
				itemAllocationComponent.createAllocation(itemAllocationComponent.getNumUnallocated(), parentEntity, ItemAllocation.Purpose.HAULING);
			}
		}

		if (stockpileAllocation == null) {
			// Couldn't find any stockpile, just go somewhere nearby and dump
			return new AssignedGoal(DUMP_ITEM.getInstance(), parentEntity, messageDispatcher);
		} else {
			AssignedGoal assignedGoal = new AssignedGoal(PLACE_ITEM.getInstance(), parentEntity, messageDispatcher);
			assignedGoal.setAssignedHaulingAllocation(stockpileAllocation);
			return assignedGoal;
		}
	}

	public static AssignedGoal checkToPlaceInventoryItems(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		// Place an unused item into a stockpile if a space is available
		InventoryComponent inventory = parentEntity.getComponent(InventoryComponent.class);
		if (inventory != null) {
			MilitaryComponent militaryComponent = parentEntity.getComponent(MilitaryComponent.class);
			List<Long> assignedMilitaryItems = militaryComponent == null ? List.of() : militaryComponent.getItemIdsToHoldOnto();

			double currentGameTime = gameContext.getGameClock().getCurrentGameTime();
			for (InventoryComponent.InventoryEntry entry : inventory.getInventoryEntries()) {
				if (entry.entity.getType().equals(ITEM)) {
					ItemEntityAttributes attributes = (ItemEntityAttributes) entry.entity.getPhysicalEntityComponent().getAttributes();

					if (assignedMilitaryItems.contains(entry.entity.getId())) {
						// This is one of our assigned items of military equipment
						continue;
					}

					if (attributes.getItemType().getIsAmmoType() != null) {
						Long assignedWeaponId = militaryComponent.getAssignedWeaponId();
						if (assignedWeaponId != null && militaryComponent.isInMilitary()) {
							Entity assignedWeapon = gameContext.getEntities().get(assignedWeaponId);
							if (assignedWeapon != null && assignedWeapon.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes weaponAttributes) {
								if (attributes.getItemType().getIsAmmoType().equals(
										weaponAttributes.getItemType().getWeaponInfo().getRequiresAmmoType())) {
									// This item is ammo for the currently assigned weapon
									continue;
								}
							}
						}
					}

					if (entry.getLastUpdateGameTime() + attributes.getItemType().getHoursInInventoryUntilUnused() < currentGameTime) {
						// Temp un-requestAllocation
						ItemAllocationComponent itemAllocationComponent = entry.entity.getOrCreateComponent(ItemAllocationComponent.class);
						itemAllocationComponent.cancelAll(HELD_IN_INVENTORY);

						HaulingAllocation stockpileAllocation = findStockpileAllocation(gameContext.getAreaMap(), entry.entity, parentEntity, messageDispatcher);

						if (stockpileAllocation == null) {
							itemAllocationComponent.createAllocation(attributes.getQuantity(), parentEntity, HELD_IN_INVENTORY);
						} else {
							ItemAllocation newAllocation = itemAllocationComponent.swapAllocationPurpose(DUE_TO_BE_HAULED, HELD_IN_INVENTORY, stockpileAllocation.getItemAllocation().getAllocationAmount());
							stockpileAllocation.setItemAllocation(newAllocation);

							if (itemAllocationComponent.getNumUnallocated() > 0) {
								itemAllocationComponent.createAllocation(itemAllocationComponent.getNumUnallocated(), parentEntity, HELD_IN_INVENTORY);
							}

							return placeItemIntoStockpileGoal(entry.entity, parentEntity, messageDispatcher, stockpileAllocation);
						}
					}
				}
			}
		}

		return null;
	}

	public static AssignedGoal tantrumGoal(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) throws EnteringCombatException {
		GridPoint2 parentLocation = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldOrParentPosition()).getTilePosition();

		Entity target = null;
		List<CompassDirection> directions = new ArrayList<>(List.of(CompassDirection.values()));
		Collections.shuffle(directions, gameContext.getRandom());

		for (CompassDirection direction : directions) {
			for (int distance = 1; distance < 6; distance++) {
				MapTile tile = gameContext.getAreaMap().getTile(parentLocation.x + (direction.getXOffset() * distance),
						parentLocation.y + (direction.getYOffset() * distance));
				if (tile != null) {
					for (Entity entity : tile.getEntities()) {
						if (entity.equals(parentEntity)) {
							continue;
						}

						if (entity.getType().equals(CREATURE)) {
							CreatureEntityAttributes attributes = (CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
							if (!attributes.getConsciousness().equals(DEAD) && !attributes.getConsciousness().equals(KNOCKED_UNCONSCIOUS)) {
								target = entity;
								break;
							}
						}
						if (entity.getType().equals(FURNITURE)) {
							FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
							if (!attributes.isDestroyed()) {
								target = entity;
								break;
							}
						}
					}
					if (target != null) {
						break;
					}
					if (!tile.isNavigable(parentEntity)) {
						break;
					}
				}
			}
			if (target != null) {
				break;
			}
		}
		// giving happiness buff even with no target found or else player gets multiple tantrum notifications until dead
		parentEntity.getComponent(HappinessComponent.class).add(HappinessComponent.HappinessModifier.HAD_A_TANTRUM);
		if (target == null) {
			return new AssignedGoal(IDLE.getInstance(), parentEntity, messageDispatcher);
		} else {
			Memory tantrumMemory = new Memory(MemoryType.HAD_A_TANTRUM, gameContext.getGameClock());
			parentEntity.getComponent(MemoryComponent.class).addLongTerm(tantrumMemory);
			tantrumMemory.setRelatedEntityId(target.getId());

			CombatStateComponent combatStateComponent = parentEntity.getComponent(CombatStateComponent.class);
			combatStateComponent.clearState();
			combatStateComponent.setTargetedOpponentId(target.getId());
			throw new EnteringCombatException();
		}
	}

	public static AssignedGoal placeItemIntoStockpileGoal(Entity itemEntity, Entity parentEntity, MessageDispatcher messageDispatcher, HaulingAllocation stockpileAllocation) {
		AssignedGoal assignedGoal = new AssignedGoal(PLACE_ITEM.getInstance(), parentEntity, messageDispatcher);
		assignedGoal.setAssignedHaulingAllocation(stockpileAllocation);
		ItemEntityAttributes attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();
		if (attributes.getItemType().isEquippedWhileWorkingOnJob()) {
			// Switch to hauling component
			HaulingComponent haulingComponent = parentEntity.getOrCreateComponent(HaulingComponent.class);
			InventoryComponent inventoryComponent = parentEntity.getComponent(InventoryComponent.class);
			inventoryComponent.remove(itemEntity.getId());
			haulingComponent.setHauledEntity(itemEntity, messageDispatcher, parentEntity);
		}
		return assignedGoal;
	}

	public static AssignedGoal onFireGoal(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		Race race = ((CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes()).getRace();
		if (race.getBehaviour().getIsSapient()) {
			// Only sapient creates will attempt to get a liquid allocation and douse themselves
			final AtomicReference<Optional<LiquidAllocation>> liquidAllocation = new AtomicReference<>(Optional.empty());
			messageDispatcher.dispatchMessage(MessageType.REQUEST_LIQUID_ALLOCATION, new RequestLiquidAllocationMessage(
					parentEntity, AMOUNT_REQUIRED_TO_DOUSE_FIRE, false, true, allocation -> {
						liquidAllocation.set(allocation);
			}));

			if (liquidAllocation.get().isPresent()) {
				GridPoint2 accessLocation = liquidAllocation.get().get().getTargetZoneTile().getAccessLocation();
				float distanceToLiquidAllocation = parentEntity.getLocationComponent().getWorldOrParentPosition().dst(toVector(accessLocation));
				if (distanceToLiquidAllocation > MAX_DISTANCE_TO_LIQUID_FOR_DOUSE_FIRE) {
					messageDispatcher.dispatchMessage(MessageType.LIQUID_ALLOCATION_CANCELLED, liquidAllocation.get());
					liquidAllocation.set(Optional.empty());
				} else {
					AssignedGoal douseSelfGoal = new AssignedGoal(DOUSE_SELF.getInstance(), parentEntity, messageDispatcher);
					// return douse goal with allocation set
					douseSelfGoal.setLiquidAllocation(liquidAllocation.get().get());
					return douseSelfGoal;
				}
			}
		}

		if (gameContext.getRandom().nextBoolean()) {
			return new AssignedGoal(ROLL_ON_FLOOR.getInstance(), parentEntity, messageDispatcher);
		}
		return new AssignedGoal(IDLE.getInstance(), parentEntity, messageDispatcher);
	}

}
