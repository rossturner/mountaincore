package technology.rocketjump.saul.entities.behaviour.furniture;

import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.components.ItemAllocationComponent;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.RequestHaulingMessage;

public class DestroyWhenInventoryEmptyBehaviour extends FurnitureBehaviour implements Prioritisable {

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		super.infrequentUpdate(gameContext);

		InventoryComponent inventoryComponent = parentEntity.getComponent(InventoryComponent.class);
		if (inventoryComponent.isEmpty()) {
			messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, parentEntity);
		} else {
			for (InventoryComponent.InventoryEntry entry : inventoryComponent.getInventoryEntries()) {
				if (entry.entity.getType().equals(EntityType.ITEM)) {
					ItemAllocationComponent itemAllocationComponent = entry.entity.getOrCreateComponent(ItemAllocationComponent.class);
					if (itemAllocationComponent.getNumUnallocated() > 0) {
						messageDispatcher.dispatchMessage(MessageType.REQUEST_ENTITY_HAULING, new RequestHaulingMessage(entry.entity, parentEntity, false, priority, null));
					}
				} else {
					Logger.warn("To be implemented: Handle non-item type inventory items in " + this.getClass().getSimpleName());
				}
			}

		}
	}
}
