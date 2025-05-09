package technology.rocketjump.mountaincore.entities.behaviour.creature;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.mountaincore.entities.ai.combat.EnteringCombatException;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SpecialGoal;
import technology.rocketjump.mountaincore.entities.ai.memory.Memory;
import technology.rocketjump.mountaincore.entities.ai.memory.MemoryType;
import technology.rocketjump.mountaincore.entities.components.*;
import technology.rocketjump.mountaincore.entities.components.creature.CombatStateComponent;
import technology.rocketjump.mountaincore.entities.components.creature.HappinessComponent;
import technology.rocketjump.mountaincore.entities.components.creature.MemoryComponent;
import technology.rocketjump.mountaincore.entities.components.creature.MilitaryComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.creature.HaulingComponent;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Race;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.tile.CompassDirection;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.RequestLiquidAllocationMessage;
import technology.rocketjump.mountaincore.misc.VectorUtils;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;
import technology.rocketjump.mountaincore.rooms.HaulingAllocationBuilder;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static technology.rocketjump.mountaincore.entities.ItemEntityMessageHandler.findStockpileAllocation;
import static technology.rocketjump.mountaincore.entities.model.physical.creature.Consciousness.DEAD;
import static technology.rocketjump.mountaincore.entities.model.physical.creature.Consciousness.KNOCKED_UNCONSCIOUS;

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
				.anyMatch(m -> m.getType().equals(MemoryType.FAILED_GOAL) && SpecialGoal.PLACE_ITEM.goalName.equals(m.getRelatedGoalName()));

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
				ItemAllocation newAllocation = itemAllocationComponent.swapAllocationPurpose(ItemAllocation.Purpose.DUE_TO_BE_HAULED, ItemAllocation.Purpose.HAULING, stockpileAllocation.getItemAllocation());
				stockpileAllocation.setItemAllocation(newAllocation);
			}

			// Always re-allocate remaining amount to hauling
			if (itemAllocationComponent.getNumUnallocated() > 0) {
				itemAllocationComponent.createAllocation(itemAllocationComponent.getNumUnallocated(), parentEntity, ItemAllocation.Purpose.HAULING);
			}
		}

		if (stockpileAllocation == null) {
			// Couldn't find any stockpile, just go somewhere nearby and dump
			return new AssignedGoal(SpecialGoal.DUMP_ITEM.getInstance(), parentEntity, messageDispatcher, gameContext);
		} else {
			AssignedGoal assignedGoal = new AssignedGoal(SpecialGoal.PLACE_ITEM.getInstance(), parentEntity, messageDispatcher, gameContext);
			assignedGoal.setAssignedHaulingAllocation(stockpileAllocation);
			return assignedGoal;
		}
	}

	public static AssignedGoal checkToPlaceInventoryItems(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (!parentEntity.getOrCreateComponent(FactionComponent.class).getFaction().equals(Faction.SETTLEMENT)) {
			return null;
		}
		// Place an unused item into a stockpile if a space is available
		InventoryComponent inventory = parentEntity.getComponent(InventoryComponent.class);
		if (inventory != null) {
			MilitaryComponent militaryComponent = parentEntity.getComponent(MilitaryComponent.class);
			List<Long> assignedMilitaryItems = militaryComponent == null ? List.of() : militaryComponent.getItemIdsToHoldOnto();

			double currentGameTime = gameContext.getGameClock().getCurrentGameTime();
			for (InventoryComponent.InventoryEntry entry : inventory.getInventoryEntries()) {
				final double hoursInInventoryUntilUnused;
				final int quantity;
				if (entry.entity.getType().equals(EntityType.ITEM)) {
					ItemEntityAttributes attributes = (ItemEntityAttributes) entry.entity.getPhysicalEntityComponent().getAttributes();
					hoursInInventoryUntilUnused = attributes.getItemType().getHoursInInventoryUntilUnused();
					quantity = attributes.getQuantity();

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
				} else {
					hoursInInventoryUntilUnused = ItemType.DEFAULT_HOURS_FOR_ITEM_TO_BECOME_UNUSED;
					quantity = 1;
				}

				if (entry.getLastUpdateGameTime() + hoursInInventoryUntilUnused < currentGameTime) {

					// Temp un-requestAllocation
					ItemAllocationComponent itemAllocationComponent = entry.entity.getOrCreateComponent(ItemAllocationComponent.class);

					ItemAllocation dueToHaulPurpose = itemAllocationComponent.getAllocationForPurpose(ItemAllocation.Purpose.DUE_TO_BE_HAULED);
					if (dueToHaulPurpose != null) {
						boolean haulingJobExists = gameContext.getJobs().values().stream()
								.filter(j -> j.getHaulingAllocation() != null)
								.anyMatch(j -> Objects.equals(j.getHaulingAllocation().getHaulingAllocationId(), dueToHaulPurpose.getRelatedHaulingAllocationId()));
						if (!haulingJobExists) {
							//something has gone wrong, cannot find hauling job
							itemAllocationComponent.cancelAll(ItemAllocation.Purpose.DUE_TO_BE_HAULED);
						}
					}


					if (itemAllocationComponent.getNumUnallocated() > 0 ||
							(itemAllocationComponent.getAllocationForPurpose(ItemAllocation.Purpose.HELD_IN_INVENTORY) != null && itemAllocationComponent.getAllocationForPurpose(ItemAllocation.Purpose.HELD_IN_INVENTORY).getAllocationAmount() > 0)) {
						itemAllocationComponent.cancelAll(ItemAllocation.Purpose.HELD_IN_INVENTORY);

						HaulingAllocation stockpileAllocation = findStockpileAllocation(gameContext.getAreaMap(), entry.entity, parentEntity, messageDispatcher);

						if (stockpileAllocation == null) {

							AssignedGoal dumpItemGoal = new AssignedGoal(SpecialGoal.DUMP_ITEM.getInstance(), parentEntity, messageDispatcher, gameContext);
							dumpItemGoal.setAssignedHaulingAllocation(HaulingAllocationBuilder.createWithItemAllocation(itemAllocationComponent.getNumUnallocated(), entry.entity, parentEntity).toUnspecifiedLocation());
							return dumpItemGoal;
						} else {

							ItemAllocation newAllocation = itemAllocationComponent.swapAllocationPurpose(ItemAllocation.Purpose.DUE_TO_BE_HAULED, ItemAllocation.Purpose.HELD_IN_INVENTORY, stockpileAllocation.getItemAllocation());
							stockpileAllocation.setItemAllocation(newAllocation);

							if (itemAllocationComponent.getNumUnallocated() > 0) {
								itemAllocationComponent.createAllocation(itemAllocationComponent.getNumUnallocated(), parentEntity, ItemAllocation.Purpose.HELD_IN_INVENTORY);
							}

							return placeItemIntoStockpileGoal(entry.entity, parentEntity, messageDispatcher, gameContext, stockpileAllocation);
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

						if (entity.getType().equals(EntityType.CREATURE)) {
							CreatureEntityAttributes attributes = (CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
							if (!attributes.getConsciousness().equals(DEAD) && !attributes.getConsciousness().equals(KNOCKED_UNCONSCIOUS)) {
								target = entity;
								break;
							}
						}
						if (entity.getType().equals(EntityType.FURNITURE)) {
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
			return new AssignedGoal(SpecialGoal.IDLE.getInstance(), parentEntity, messageDispatcher, gameContext);
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

	public static AssignedGoal placeItemIntoStockpileGoal(Entity itemEntity, Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext, HaulingAllocation stockpileAllocation) {
		AssignedGoal assignedGoal = new AssignedGoal(SpecialGoal.PLACE_ITEM.getInstance(), parentEntity, messageDispatcher, gameContext);
		assignedGoal.setAssignedHaulingAllocation(stockpileAllocation);
		if (itemEntity.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes attributes) {
			if (attributes.getItemType().isEquippedWhileWorkingOnJob()) {
				// Switch to hauling component
				HaulingComponent haulingComponent = parentEntity.getOrCreateComponent(HaulingComponent.class);
				InventoryComponent inventoryComponent = parentEntity.getComponent(InventoryComponent.class);
				inventoryComponent.remove(itemEntity.getId());
				haulingComponent.setHauledEntity(itemEntity, messageDispatcher, parentEntity);
			}
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
				float distanceToLiquidAllocation = parentEntity.getLocationComponent().getWorldOrParentPosition().dst(VectorUtils.toVector(accessLocation));
				if (distanceToLiquidAllocation > MAX_DISTANCE_TO_LIQUID_FOR_DOUSE_FIRE && liquidAllocation.get().isPresent()) {
					messageDispatcher.dispatchMessage(MessageType.LIQUID_ALLOCATION_CANCELLED, liquidAllocation.get().get());
					liquidAllocation.set(Optional.empty());
				} else {
					AssignedGoal douseSelfGoal = new AssignedGoal(SpecialGoal.DOUSE_SELF.getInstance(), parentEntity, messageDispatcher, gameContext);
					// return douse goal with allocation set
					douseSelfGoal.setLiquidAllocation(liquidAllocation.get().get());
					return douseSelfGoal;
				}
			}
		}

		if (gameContext.getRandom().nextBoolean()) {
			return new AssignedGoal(SpecialGoal.ROLL_ON_FLOOR.getInstance(), parentEntity, messageDispatcher, gameContext);
		}
		return new AssignedGoal(SpecialGoal.IDLE.getInstance(), parentEntity, messageDispatcher, gameContext);
	}

	public static AssignedGoal doNothingGoal(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		return new AssignedGoal(SpecialGoal.DO_NOTHING.getInstance(), parentEntity, messageDispatcher, gameContext);
	}

}
