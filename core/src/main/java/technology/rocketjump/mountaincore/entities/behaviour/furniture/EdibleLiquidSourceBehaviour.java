package technology.rocketjump.mountaincore.entities.behaviour.furniture;

import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.EntityCreatedCallback;
import technology.rocketjump.mountaincore.entities.components.LiquidAllocation;
import technology.rocketjump.mountaincore.entities.components.LiquidContainerComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.ItemCreationRequestMessage;
import technology.rocketjump.mountaincore.messaging.types.ItemPrimaryMaterialChangedMessage;
import technology.rocketjump.mountaincore.messaging.types.LiquidSplashMessage;

public class EdibleLiquidSourceBehaviour extends FurnitureBehaviour implements EntityCreatedCallback {

	private transient Entity createdItem;

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		super.infrequentUpdate(gameContext);
		LiquidContainerComponent liquidContainerComponent = parentEntity.getComponent(LiquidContainerComponent.class);
		if (liquidContainerComponent != null) {
			if (liquidContainerComponent.getLiquidQuantity() <= 0) {
				liquidContainerComponent.setTargetLiquidMaterial(null);
				messageDispatcher.dispatchMessage(MessageType.REQUEST_FURNITURE_REMOVAL, parentEntity);
				parentEntity.replaceBehaviourComponent(null);
				messageDispatcher.dispatchMessage(MessageType.FURNITURE_PLACEMENT, parentEntity);
			}
		}
	}

	public Entity createItem(LiquidAllocation allocationToCreateFrom, GameContext gameContext) {
		if (relatedItemTypes.isEmpty() || relatedItemTypes.get(0) == null) {
			Logger.error("No item type specified to create in " + this.getClass().getSimpleName());
			return null;
		}


		LiquidContainerComponent parentLiquidContainerComponent = parentEntity.getComponent(LiquidContainerComponent.class);
		LiquidAllocation success = parentLiquidContainerComponent.cancelAllocationAndDecrementQuantity(allocationToCreateFrom);
		if (success == null) {
			return null;
		}

		messageDispatcher.dispatchMessage(MessageType.ITEM_CREATION_REQUEST, new ItemCreationRequestMessage(relatedItemTypes.get(0), this));
		if (createdItem == null) {
			return null;
		}
		GameMaterial oldPrimaryMaterial = ((ItemEntityAttributes) createdItem.getPhysicalEntityComponent().getAttributes()).getPrimaryMaterial();

		LiquidContainerComponent itemLiquidContainerComponent = new LiquidContainerComponent();
		itemLiquidContainerComponent.init(createdItem, messageDispatcher, gameContext);
		createdItem.addComponent(itemLiquidContainerComponent);


		itemLiquidContainerComponent.setTargetLiquidMaterial(parentLiquidContainerComponent.getTargetLiquidMaterial());
		itemLiquidContainerComponent.setLiquidQuantity(1f);
		messageDispatcher.dispatchMessage(MessageType.LIQUID_SPLASH, new LiquidSplashMessage(parentEntity, parentLiquidContainerComponent.getTargetLiquidMaterial()));

		if (!oldPrimaryMaterial.equals(((ItemEntityAttributes)createdItem.getPhysicalEntityComponent().getAttributes()).getPrimaryMaterial())) {
			// Tracker needs updating due to change in material
			messageDispatcher.dispatchMessage(MessageType.ITEM_PRIMARY_MATERIAL_CHANGED, new ItemPrimaryMaterialChangedMessage(createdItem, oldPrimaryMaterial));
		}
		return createdItem;
	}

	@Override
	public void entityCreated(Entity entity) {
		this.createdItem = entity;
	}
}
