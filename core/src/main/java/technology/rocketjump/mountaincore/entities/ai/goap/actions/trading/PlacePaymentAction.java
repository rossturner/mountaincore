package technology.rocketjump.mountaincore.entities.ai.goap.actions.trading;

import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.PlaceEntityAction;
import technology.rocketjump.mountaincore.entities.components.*;
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
			ItemAllocationComponent originalItemAllocationComponent = originalItem.getComponent(ItemAllocationComponent.class);
			originalItemAllocationComponent.cancelAll(ItemAllocation.Purpose.TRADING_PAYMENT);

			ItemEntityAttributes originalAttributes = (ItemEntityAttributes) originalItem.getPhysicalEntityComponent().getAttributes();
			int remainder = originalAttributes.getQuantity() - amountToPlace;
			originalAttributes.setQuantity(remainder);
			if (remainder <= 0) {
				parent.messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, originalItem);
			} else {
				parent.messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, originalItem);
			}

			ItemEntityAttributes clonedAttributes = (ItemEntityAttributes) clonedItem.getPhysicalEntityComponent().getAttributes();
			clonedAttributes.setQuantity(amountToPlace);
			clonedItem.getComponent(ItemAllocationComponent.class).cancelAll();
			parent.messageDispatcher.dispatchMessage(MessageType.ENTITY_CREATED, clonedItem);

			HaulingAllocation haulingAllocation = parent.getAssignedHaulingAllocation();
			if (haulingAllocation == null || haulingAllocation.getItemAllocation().isCancelled()) {
				placeEntityIntoTile(clonedItem, parentInventory, currentTile);
			} else {
				Entity targetFurniture = gameContext.getEntity(haulingAllocation.getTargetId());
				if (adjacentTo(targetFurniture)) {
					placeEntityInFurniture(clonedItem, gameContext.getEntity(haulingAllocation.getTargetId()), gameContext, haulingAllocation);
				} else {
					placeEntityIntoTile(clonedItem, parentInventory, currentTile);
				}
				originalItemAllocationComponent.cancel(parent.getAssignedHaulingAllocation().getItemAllocation());
			}

			if (completionType == SUCCESS) {
				if (clonedItem.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes itemEntityAttributes) {
					if (itemEntityAttributes.getItemType().getPlacementSoundAsset() != null) {
						parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(
								itemEntityAttributes.getItemType().getPlacementSoundAsset(), parent.parentEntity
						));
					}
				}
				clonedItem.getOrCreateComponent(FactionComponent.class).setFaction(Faction.SETTLEMENT);
			} else {
				InventoryComponent.InventoryEntry entry = parentInventory.add(clonedItem, parent.parentEntity, parent.messageDispatcher, gameContext.getGameClock());

				ItemAllocationComponent itemAllocationComponent = entry.entity.getComponent(ItemAllocationComponent.class);
				itemAllocationComponent.cancelAll(ItemAllocation.Purpose.HELD_IN_INVENTORY);
				if (remainder > 0) {
					itemAllocationComponent.createAllocation(remainder, parent.parentEntity, ItemAllocation.Purpose.HELD_IN_INVENTORY);
				}
				ItemAllocation recreatedPaymentAllocation = itemAllocationComponent.createAllocation(amountToPlace, parent.parentEntity, ItemAllocation.Purpose.TRADING_PAYMENT);
				parent.getPlannedTrade().setPaymentItemAllocation(recreatedPaymentAllocation);
			}
		}
	}

}
