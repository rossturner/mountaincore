package technology.rocketjump.mountaincore.entities.model.physical.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.mountaincore.entities.components.EntityComponent;
import technology.rocketjump.mountaincore.entities.components.ItemAllocation;
import technology.rocketjump.mountaincore.entities.components.ItemAllocationComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.combat.WeaponInfo;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rendering.camera.GlobalSettings;

import java.util.ArrayList;
import java.util.List;

public class EquippedItemComponent implements EntityComponent {

	private Entity mainHandItem; // Note this can actually be another creature, not just an item
	private Entity offHandItem;
	private boolean hideMainHandItem;
	private boolean mainHandEnabled = true;
	private boolean offHandEnabled = true;
	private Entity equippedClothing;

	public Entity getMainHandItem() {
		return mainHandItem;
	}

	public Entity getOffHandItem() {
		return offHandItem;
	}

	public Entity getEquippedClothing() {
		return equippedClothing;
	}

	public boolean isEquippedToAnyHand(Entity targetEntity) {
		return targetEntity.equals(mainHandItem) || targetEntity.equals(offHandItem);
	}

	public boolean setEquippedToAnyHand(Entity itemToEquip, Entity parentEntity, MessageDispatcher messageDispatcher) {
		boolean successful = setMainHandItem(itemToEquip, parentEntity, messageDispatcher);
		if (successful) {
			return true;
		} else {
			return setOffHandItem(itemToEquip, parentEntity, messageDispatcher);
		}
	}

	public void clearFromEquippedHand(Entity targetEntity) {
		if (targetEntity.equals(mainHandItem)) {
			clearMainHandItem();
		} else if (targetEntity.equals(offHandItem)) {
			clearOffHandItem();
		}
	}

	public boolean setMainHandItem(Entity itemToEquip, Entity parentEntity, MessageDispatcher messageDispatcher) {
		boolean requiresTwoHands = requiresTwoHands(itemToEquip);
		if (
				(mainHandEnabled && !requiresTwoHands)
				|| (requiresTwoHands && mainHandEnabled && offHandEnabled)
		) {
			this.mainHandItem = itemToEquip;
			this.hideMainHandItem = false; // This shouldn't be necessary, but is here to guard against forgetting to unset this flag
			setContainerAndItemAllocations(itemToEquip, parentEntity, messageDispatcher);
			return true;
		} else {
			return false;
		}
	}

	public boolean isHideMainHandItem() {
		return hideMainHandItem;
	}

	public void setHideMainHandItem(boolean hideMainHandItem) {
		this.hideMainHandItem = hideMainHandItem;
	}

	public boolean setOffHandItem(Entity itemToEquip, Entity parentEntity, MessageDispatcher messageDispatcher) {
		if (offHandEnabled) {
			this.offHandItem = itemToEquip;
			setContainerAndItemAllocations(itemToEquip, parentEntity, messageDispatcher);
			return true;
		} else {
			return false;
		}
	}

	public void setEquippedClothing(Entity itemToEquip, Entity parentEntity, MessageDispatcher messageDispatcher) {
		this.equippedClothing = itemToEquip;
		setContainerAndItemAllocations(itemToEquip, parentEntity, messageDispatcher);
	}

	public List<Entity> clearHeldEquipment() {
		List<Entity> equipment = new ArrayList<>();
		Entity handItem = clearMainHandItem();
		if (handItem != null) {
			equipment.add(handItem);
		}
		Entity offHandItem = clearOffHandItem();
		if (offHandItem != null) {
			equipment.add(offHandItem);
		}
		return equipment;
	}

	public Entity disableMainHand() {
		mainHandEnabled = false;
		return clearMainHandItem();
	}

	public Entity disableOffHand() {
		offHandEnabled = false;
		if (requiresTwoHands(mainHandItem)) {
			return clearMainHandItem();
		}
		return clearOffHandItem();
	}

	public boolean isMainHandEnabled() {
		return mainHandEnabled;
	}

	public boolean isOffHandEnabled() {
		return offHandEnabled;
	}

	private boolean requiresTwoHands(Entity item) {
		if (item != null && item.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes itemAttributes) {
			WeaponInfo weaponInfo = itemAttributes.getItemType().getWeaponInfo();
			return weaponInfo != null && weaponInfo.isTwoHanded();
		}
		return false;
	}

	public Entity clearMainHandItem() {
		if (this.mainHandItem != null) {
			Entity mainHandItem = this.mainHandItem;
			clearContainerAndItemAllocations(mainHandItem);
			this.mainHandItem = null;
			return mainHandItem;
		} else {
			return null;
		}
	}

	public Entity clearOffHandItem() {
		if (this.offHandItem != null) {
			Entity offHandItem = this.offHandItem;
			clearContainerAndItemAllocations(offHandItem);
			this.offHandItem = null;
			return offHandItem;
		} else {
			return null;
		}
	}


	public Entity clearEquippedClothing() {
		if (this.equippedClothing != null) {
			Entity equippedClothing = this.equippedClothing;
			clearContainerAndItemAllocations(equippedClothing);
			this.equippedClothing = null;
			return equippedClothing;
		} else {
			return null;
		}
	}

	private void setContainerAndItemAllocations(Entity itemToEquip, Entity parentEntity, MessageDispatcher messageDispatcher) {
		itemToEquip.getLocationComponent().setContainerEntity(parentEntity);

		if (itemToEquip.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes itemAttributes) {
			itemAttributes.setItemPlacement(ItemPlacement.BEING_CARRIED);
			itemToEquip.getLocationComponent().setOrientation(parentEntity.getLocationComponent().getOrientation());
			messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, itemToEquip);

			ItemAllocationComponent itemAllocationComponent = itemToEquip.getOrCreateComponent(ItemAllocationComponent.class);
			if (itemAllocationComponent.getNumAllocated() == 0) {
				itemAllocationComponent.createAllocation(itemAttributes.getQuantity(), parentEntity, ItemAllocation.Purpose.EQUIPPED);
			} else {
				Logger.error("Equipping item which already has some allocations");
				if (GlobalSettings.DEV_MODE) {
					throw new RuntimeException("This must be fixed");
				}
			}
		}
	}

	private void clearContainerAndItemAllocations(Entity entity) {
		entity.getLocationComponent().setContainerEntity(null);
		if (entity.getType().equals(EntityType.ITEM)) {
			ItemAllocationComponent itemAllocationComponent = entity.getOrCreateComponent(ItemAllocationComponent.class);
			itemAllocationComponent.cancelAll(ItemAllocation.Purpose.EQUIPPED);
		}
	}

	public void destroyAllEntities(MessageDispatcher messageDispatcher) {
		if (mainHandItem != null) {
			messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, mainHandItem);
		}
		if (offHandItem != null) {
			messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, offHandItem);
		}
		if (equippedClothing != null) {
			messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, equippedClothing);
		}
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		EquippedItemComponent clonedComponent = new EquippedItemComponent();
		if (mainHandItem != null) {
			clonedComponent.setMainHandItem(mainHandItem.clone(messageDispatcher, gameContext), mainHandItem.getLocationComponent().getContainerEntity(), messageDispatcher);
		}
		if (offHandItem != null) {
			clonedComponent.setOffHandItem(offHandItem.clone(messageDispatcher, gameContext), offHandItem.getLocationComponent().getContainerEntity(), messageDispatcher);
		}
		if (equippedClothing != null) {
			clonedComponent.setEquippedClothing(equippedClothing.clone(messageDispatcher, gameContext), equippedClothing.getLocationComponent().getContainerEntity(), messageDispatcher);
		}
		clonedComponent.hideMainHandItem = this.hideMainHandItem;
		clonedComponent.mainHandEnabled = this.mainHandEnabled;
		clonedComponent.offHandEnabled = this.offHandEnabled;
		return clonedComponent;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (mainHandItem != null) {
			mainHandItem.writeTo(savedGameStateHolder);
			asJson.put("equippedItem", mainHandItem.getId());
		}
		if (hideMainHandItem) {
			asJson.put("hideMainHandItem", true);
		}
		if (offHandItem != null) {
			offHandItem.writeTo(savedGameStateHolder);
			asJson.put("offHandItem", offHandItem.getId());
		}
		if (equippedClothing != null) {
			equippedClothing.writeTo(savedGameStateHolder);
			asJson.put("equippedClothing", equippedClothing.getId());
		}
		asJson.put("mainHandEnabled", mainHandEnabled);
		asJson.put("offHandEnabled", offHandEnabled);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		Long equippedItemId = asJson.getLong("equippedItem");
		if (equippedItemId != null) {
			this.mainHandItem = savedGameStateHolder.entities.get(equippedItemId);
			if (this.mainHandItem == null) {
				throw new InvalidSaveException("Could not find entity with ID " + equippedItemId);
			}
		}

		this.hideMainHandItem = asJson.getBooleanValue("hideMainHandItem");

		Long offHandItemId = asJson.getLong("offHandItem");
		if (offHandItemId != null) {
			this.offHandItem = savedGameStateHolder.entities.get(offHandItemId);
			if (this.offHandItem == null) {
				throw new InvalidSaveException("Could not find entity with ID " + offHandItemId);
			}
		}

		Long equippedClothingId = asJson.getLong("equippedClothing");
		if (equippedClothingId != null) {
			this.equippedClothing = savedGameStateHolder.entities.get(equippedClothingId);
			if (this.equippedClothing == null) {
				throw new InvalidSaveException("Could not find entity with ID " + equippedClothingId);
			}
		}

		if (asJson.containsKey("mainHandEnabled")) {
			this.mainHandEnabled = asJson.getBooleanValue("mainHandEnabled");
		} else {
			this.mainHandEnabled = true;
		}

		if (asJson.containsKey("offHandEnabled")) {
			this.offHandEnabled = asJson.getBooleanValue("offHandEnabled");
		} else {
			this.offHandEnabled = true;
		}
	}
}
