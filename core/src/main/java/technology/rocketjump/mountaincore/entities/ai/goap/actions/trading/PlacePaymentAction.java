package technology.rocketjump.mountaincore.entities.ai.goap.actions.trading;

import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.PlaceEntityAction;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.components.ItemAllocationComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.RequestSoundMessage;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;

import static technology.rocketjump.mountaincore.entities.ai.goap.actions.Action.CompletionType.SUCCESS;

public class PlacePaymentAction extends PlaceEntityAction {

	public PlacePaymentAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		InventoryComponent parentInventory = parent.parentEntity.getComponent(InventoryComponent.class);
		Entity originalItem = parentInventory.getById(parent.getPlannedTrade().getPaymentItemAllocation().getTargetItemEntityId());
		MapTile currentTile = gameContext.getAreaMap().getTile(parent.parentEntity.getLocationComponent().getWorldOrParentPosition());

		if (originalItem != null) {
			Entity clonedItem = originalItem.clone(parent.messageDispatcher, gameContext);
			int amountToPlace = parent.getPlannedTrade().getPaymentItemAllocation().getAllocationAmount();

			ItemEntityAttributes originalAttributes = (ItemEntityAttributes) originalItem.getPhysicalEntityComponent().getAttributes();
			originalAttributes.setQuantity(originalAttributes.getQuantity() - amountToPlace);
			if (originalAttributes.getQuantity() <= 0) {
				parent.messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, originalItem);
			} else {
				parent.messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, originalItem);
			}

			ItemEntityAttributes clonedAttributes = (ItemEntityAttributes) clonedItem.getPhysicalEntityComponent().getAttributes();
			clonedAttributes.setQuantity(amountToPlace);
			clonedItem.getComponent(ItemAllocationComponent.class).cancelAll();
			parent.messageDispatcher.dispatchMessage(MessageType.ENTITY_CREATED, clonedItem);

			HaulingAllocation haulingAllocation = parent.getAssignedHaulingAllocation();
			if (haulingAllocation == null) {
				placeEntityIntoTile(clonedItem, parentInventory, currentTile);
			} else {
				originalItem.getComponent(ItemAllocationComponent.class).cancel(haulingAllocation.getItemAllocation());

				Entity targetFurniture = gameContext.getEntity(haulingAllocation.getTargetId());
				if (adjacentTo(targetFurniture)) {
					placeEntityInFurniture(clonedItem, gameContext.getEntity(haulingAllocation.getTargetId()), gameContext, haulingAllocation);
				} else {
					placeEntityIntoTile(clonedItem, parentInventory, currentTile);
				}
			}

			if (completionType == SUCCESS) {
				if (clonedItem.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes itemEntityAttributes) {
					if (itemEntityAttributes.getItemType().getPlacementSoundAsset() != null) {
						parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(
								itemEntityAttributes.getItemType().getPlacementSoundAsset(), parent.parentEntity
						));
					}
				}
			}
		}
	}

}
