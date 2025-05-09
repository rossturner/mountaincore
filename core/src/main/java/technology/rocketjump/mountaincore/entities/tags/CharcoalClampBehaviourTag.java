package technology.rocketjump.mountaincore.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.DestroyWhenInventoryEmptyBehaviour;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.ItemCreationRequestMessage;
import technology.rocketjump.mountaincore.messaging.types.ItemPrimaryMaterialChangedMessage;

public class CharcoalClampBehaviourTag extends Tag {

	@Override
	public String getTagName() {
		return "CHARCOAL_CLAMP_BEHAVIOUR";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return true;
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (entity.getBehaviourComponent() == null) {
			// Don't apply to furniture which already doesn't have a BehaviourComponent e.g. when placing from UI
			return;
		} else if (!entity.getBehaviourComponent().getClass().equals(DestroyWhenInventoryEmptyBehaviour.class)) {
			// Only switch behaviour if already different

			InventoryComponent inventoryComponent = entity.getOrCreateComponent(InventoryComponent.class);
			inventoryComponent.setAddAsAllocationPurpose(null);
			// Create 2 lots of charcoal
			GameMaterial charcoalMaterial = tagProcessingUtils.materialDictionary.getByName("Charcoal");
			ItemType fuelItemType = tagProcessingUtils.itemTypeDictionary.getByName("Fuel-Sack");
			FurnitureEntityAttributes parentAttributes = (FurnitureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
			parentAttributes.setMaterial(charcoalMaterial);

			for (int x = 0; x < 2; x++) {
				messageDispatcher.dispatchMessage(MessageType.ITEM_CREATION_REQUEST, new ItemCreationRequestMessage(fuelItemType, (itemEntity) -> {
					if (itemEntity != null) {
						ItemEntityAttributes attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();
						GameMaterial oldPrimaryMaterial = attributes.getPrimaryMaterial();
						attributes.setQuantity(fuelItemType.getMaxStackSize());
						attributes.setMaterial(charcoalMaterial);
						if (!oldPrimaryMaterial.equals(attributes.getPrimaryMaterial())) {
							messageDispatcher.dispatchMessage(MessageType.ITEM_PRIMARY_MATERIAL_CHANGED, new ItemPrimaryMaterialChangedMessage(itemEntity, oldPrimaryMaterial));
						}
						inventoryComponent.add(itemEntity, entity, messageDispatcher, gameContext.getGameClock());
					}
				}));
			}

			DestroyWhenInventoryEmptyBehaviour behaviour = new DestroyWhenInventoryEmptyBehaviour();
			behaviour.init(entity, messageDispatcher, gameContext);
			entity.replaceBehaviourComponent(behaviour);
		}
	}

}
