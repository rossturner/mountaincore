package technology.rocketjump.saul.entities.ai.goap.actions.trading;

import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.actions.PlaceEntityAction;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.components.ItemAllocationComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.RequestSoundMessage;
import technology.rocketjump.saul.rooms.HaulingAllocation;

import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.SUCCESS;

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
