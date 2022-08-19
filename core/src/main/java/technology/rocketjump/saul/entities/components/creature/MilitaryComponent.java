package technology.rocketjump.saul.entities.components.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.apache.commons.lang3.NotImplementedException;
import technology.rocketjump.saul.entities.components.EntityComponent;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.components.ItemAllocationComponent;
import technology.rocketjump.saul.entities.components.ParentDependentEntityComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.misc.Destructible;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.List;

public class MilitaryComponent implements ParentDependentEntityComponent, Destructible {

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
		assignedWeaponId = checkItemStillAvailable(assignedWeaponId);
		this.assignedWeaponId = assignedWeaponId;
	}

	public void setAssignedShieldId(Long assignedShieldId) {
		assignedShieldId = checkItemStillAvailable(assignedShieldId);
		this.assignedShieldId = assignedShieldId;
	}

	public void setAssignedArmorId(Long assignedArmorId) {
		assignedArmorId = checkItemStillAvailable(assignedArmorId);
		this.assignedArmorId = assignedArmorId;
	}

	private Long checkItemStillAvailable(Long itemEntityId) {
		// TODO change this to set up ItemAssignmentComponent or whatever we called it
		if (itemEntityId != null) {
			Entity itemEntity = gameContext.getEntities().get(itemEntityId);
			if (itemEntity != null) {
				ItemAllocationComponent allocationComponent = itemEntity.getComponent(ItemAllocationComponent.class);
				if (allocationComponent != null) {
					if (allocationComponent.getNumUnallocated() > 0) {
						return itemEntityId;
					}
				}
			}
		}
		return null;
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		throw new NotImplementedException("Creatures are not cloned");
	}

	public boolean isInMilitary() {
		return squadId != null;
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
		throw new NotImplementedException("Implement this");
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		throw new NotImplementedException("Implement this");
	}
}
