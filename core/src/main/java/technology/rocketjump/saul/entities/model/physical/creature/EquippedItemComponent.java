package technology.rocketjump.saul.entities.model.physical.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.saul.entities.components.EntityComponent;
import technology.rocketjump.saul.entities.components.ItemAllocationComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.rendering.camera.GlobalSettings;

import java.util.ArrayList;
import java.util.List;

import static technology.rocketjump.saul.entities.components.ItemAllocation.Purpose.EQUIPPED;

public class EquippedItemComponent implements EntityComponent {

	private Entity mainHandItem; // Note this can actually be another creature, not just an item
	private Entity offHandItem;
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

	public void setMainHandItem(Entity itemToEquip, Entity parentEntity, MessageDispatcher messageDispatcher) {
		this.mainHandItem = itemToEquip;
		setContainerAndItemAllocations(itemToEquip, parentEntity, messageDispatcher);
	}

	public void setOffHandItem(Entity itemToEquip, Entity parentEntity, MessageDispatcher messageDispatcher) {
		this.offHandItem = itemToEquip;
		setContainerAndItemAllocations(itemToEquip, parentEntity, messageDispatcher);
	}

	public void setEquippedClothing(Entity itemToEquip, Entity parentEntity, MessageDispatcher messageDispatcher) {
		this.equippedClothing = itemToEquip;
		setContainerAndItemAllocations(itemToEquip, parentEntity, messageDispatcher);
	}

	public List<Entity> clearAllEquipment() {
		List<Entity> equipment = new ArrayList<>();
		Entity handItem = clearMainHandItem();
		if (handItem != null) {
			equipment.add(handItem);
		}
		Entity offHandItem = clearOffHandItem();
		if (offHandItem != null) {
			equipment.add(offHandItem);
		}
		Entity clothing = clearEquippedClothing();
		if (clothing != null) {
			equipment.add(clothing);
		}
		return equipment;
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
			messageDispatcher.dispatchMessage(null, MessageType.ENTITY_ASSET_UPDATE_REQUIRED, itemToEquip);

			ItemAllocationComponent itemAllocationComponent = itemToEquip.getOrCreateComponent(ItemAllocationComponent.class);
			if (itemAllocationComponent.getNumAllocated() == 0) {
				itemAllocationComponent.createAllocation(itemAttributes.getQuantity(), parentEntity, EQUIPPED);
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
			itemAllocationComponent.cancelAll(EQUIPPED);
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
		return clonedComponent;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (mainHandItem != null) {
			mainHandItem.writeTo(savedGameStateHolder);
			asJson.put("equippedItem", mainHandItem.getId());
		}
		if (offHandItem != null) {
			offHandItem.writeTo(savedGameStateHolder);
			asJson.put("offHandItem", offHandItem.getId());
		}
		if (equippedClothing != null) {
			equippedClothing.writeTo(savedGameStateHolder);
			asJson.put("equippedClothing", equippedClothing.getId());
		}
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
	}
}
