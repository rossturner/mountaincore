package technology.rocketjump.mountaincore.entities.components.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.apache.commons.lang3.NotImplementedException;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.mountaincore.entities.components.*;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.misc.Destructible;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;
import technology.rocketjump.mountaincore.rooms.HaulingAllocationBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MilitaryComponent implements InfrequentlyUpdatableComponent, Destructible {

	private Long squadId; // null means not in military

	private Long assignedWeaponId;
	private Long assignedShieldId;
	private Long assignedArmorId;

	private transient Entity parentEntity;
	private transient MessageDispatcher messageDispatcher;
	private transient GameContext gameContext;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.parentEntity = parentEntity;
		this.messageDispatcher = messageDispatcher;
		this.gameContext = gameContext;
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (squadId != null) {
			removeFromMilitary();
		}
	}

	public void addToMilitary(long squadId) {
		this.squadId = squadId;
		messageDispatcher.dispatchMessage(MessageType.MILITARY_ASSIGNMENT_CHANGED, parentEntity);

	}

	public void removeFromMilitary() {
		InventoryComponent inventoryComponent = parentEntity.getOrCreateComponent(InventoryComponent.class);
		for (Long itemId : getItemIdsToHoldOnto()) {
			setToRemoveFromInventory(itemId, inventoryComponent);
		}

		this.squadId = null;
		messageDispatcher.dispatchMessage(MessageType.MILITARY_ASSIGNMENT_CHANGED, parentEntity);
	}

	public Long getSquadId() {
		return squadId;
	}

	public Long getAssignedWeaponId() {
		return assignedWeaponId;
	}

	public Long getAssignedShieldId() {
		return assignedShieldId;
	}

	public Long getAssignedArmorId() {
		return assignedArmorId;
	}

	public void setAssignedWeaponId(Long assignedWeaponId) {
		cancelHauling(this.assignedWeaponId);
		this.assignedWeaponId = assignedWeaponId;
	}

	public void setAssignedShieldId(Long assignedShieldId) {
		cancelHauling(this.assignedShieldId);
		this.assignedShieldId = assignedShieldId;
	}

	public void setAssignedArmorId(Long assignedArmorId) {
		if (this.assignedArmorId != null) {
			cancelHauling(this.assignedArmorId);
			unequipArmor();
		}
		this.assignedArmorId = assignedArmorId;
	}

	@Override
	public void infrequentUpdate(double elapsedTime) {
		// set up hauling allocations
		if (isInMilitary()) {
			createHaulingAllocationIfRequired(assignedWeaponId);
			createHaulingAllocationIfRequired(assignedShieldId);
			createHaulingAllocationIfRequired(assignedArmorId);

			equipArmor();
		} else {
			unequipArmor();
		}
	}

	private void cancelHauling(Long entityId) {
		if (entityId != null) {
			ItemAssignmentComponent assignmentComponent = parentEntity.getOrCreateComponent(ItemAssignmentComponent.class);
			HaulingAllocation haulingAllocation = assignmentComponent.getByHauledItemId(entityId);
			if (haulingAllocation != null) {
				messageDispatcher.dispatchMessage(MessageType.HAULING_ALLOCATION_CANCELLED, haulingAllocation);
			}
		}
	}

	private void unequipArmor() {
		if (assignedArmorId != null) {
			EquippedItemComponent equippedItemComponent = parentEntity.getOrCreateComponent(EquippedItemComponent.class);
			if (equippedItemComponent.getEquippedClothing() != null) {
				if (equippedItemComponent.getEquippedClothing().getId() == assignedArmorId) {
					Entity armorItem = equippedItemComponent.clearEquippedClothing();
					InventoryComponent inventoryComponent = parentEntity.getOrCreateComponent(InventoryComponent.class);
					inventoryComponent.add(armorItem, parentEntity, messageDispatcher, gameContext.getGameClock());
					setToRemoveFromInventory(armorItem.getId(), inventoryComponent);
					messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, parentEntity);
				}
			}
		}
	}

	private void equipArmor() {
		if (assignedArmorId != null) {
			InventoryComponent inventoryComponent = parentEntity.getOrCreateComponent(InventoryComponent.class);
			Entity armorFromInventory = inventoryComponent.remove(assignedArmorId);
			if (armorFromInventory != null) {
				EquippedItemComponent equippedItemComponent = parentEntity.getOrCreateComponent(EquippedItemComponent.class);
				Entity currentClothing = equippedItemComponent.clearEquippedClothing();
				if (currentClothing != null) {
					inventoryComponent.add(currentClothing, parentEntity, messageDispatcher, gameContext.getGameClock());
				}
				equippedItemComponent.setEquippedClothing(armorFromInventory, parentEntity, messageDispatcher);
				messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, parentEntity);
			}
		}
	}

	private void createHaulingAllocationIfRequired(Long itemId) {
		if (itemId != null) {
			InventoryComponent inventoryComponent = parentEntity.getOrCreateComponent(InventoryComponent.class);
			ItemAssignmentComponent assignmentComponent = parentEntity.getOrCreateComponent(ItemAssignmentComponent.class);

			boolean isInInventory = inventoryComponent.getById(itemId) != null;
			boolean incomingHaulingAllocation = getCurrentHaulingAllocation(itemId) != null;

			if (!isInInventory && !incomingHaulingAllocation) {
				Entity itemEntity = gameContext.getEntities().get(itemId);
				if (itemEntity != null) {
					ItemAllocationComponent itemAllocationComponent = itemEntity.getOrCreateComponent(ItemAllocationComponent.class);
					if (itemAllocationComponent.getNumUnallocated() > 0) {
						HaulingAllocation haulingAllocation = HaulingAllocationBuilder.createWithItemAllocation(1, itemEntity, parentEntity)
								.toEntity(parentEntity);

						assignmentComponent.getHaulingAllocations().add(haulingAllocation);
					}
				}
			}
		}
	}

	private HaulingAllocation getCurrentHaulingAllocation(Long itemId) {
		if (parentEntity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour) {
			HaulingAllocation currentlyHauling = creatureBehaviour.getCurrentGoal().getAssignedHaulingAllocation();
			if (currentlyHauling != null && Objects.equals(currentlyHauling.getHauledEntityId(), itemId)) {
				return currentlyHauling;
			}
		}
		ItemAssignmentComponent itemAssignmentComponent = parentEntity.getOrCreateComponent(ItemAssignmentComponent.class);
		return itemAssignmentComponent.getByHauledItemId(itemId);
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		throw new NotImplementedException("Creatures are not cloned");
	}

	public boolean isInMilitary() {
		return squadId != null || parentEntity.getOrCreateComponent(FactionComponent.class).getFaction().equals(Faction.HOSTILE_INVASION);
	}

	public List<Long> getItemIdsToHoldOnto() {
		if (!isInMilitary()) {
			return List.of();
		} else {
			List<Long> itemIds = new ArrayList<>();
			if (assignedWeaponId != null) {
				itemIds.add(assignedWeaponId);
			}
			if (assignedShieldId != null) {
				itemIds.add(assignedShieldId);
			}
			if (assignedArmorId != null) {
				itemIds.add(assignedArmorId);
			}
			return itemIds;
		}
	}

	private void setToRemoveFromInventory(Long itemId, InventoryComponent inventoryComponent) {
		InventoryComponent.InventoryEntry entry = inventoryComponent.getEntryById(itemId);
		if (entry != null) {
			ItemEntityAttributes attributes = (ItemEntityAttributes) entry.entity.getPhysicalEntityComponent().getAttributes();
			entry.setLastUpdateGameTime(gameContext.getGameClock().getCurrentGameTime() - attributes.getItemType().getHoursInInventoryUntilUnused());
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("squadId", squadId);
		asJson.put("assignedWeaponId", assignedWeaponId);
		asJson.put("assignedShieldId", assignedShieldId);
		asJson.put("assignedArmorId", assignedArmorId);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.squadId = asJson.getLong("squadId");
		this.assignedWeaponId = asJson.getLong("assignedWeaponId");
		this.assignedShieldId = asJson.getLong("assignedShieldId");
		this.assignedArmorId = asJson.getLong("assignedArmorId");
	}
}
