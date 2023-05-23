package technology.rocketjump.mountaincore.entities.ai.goap.actions.trading;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.location.GoToLocationAction;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.mountaincore.entities.behaviour.creature.TraderCreatureGroup;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.TradingExportFurnitureBehaviour;
import technology.rocketjump.mountaincore.entities.components.ItemAllocation;
import technology.rocketjump.mountaincore.entities.components.ItemAllocationComponent;
import technology.rocketjump.mountaincore.entities.components.furniture.FurnitureStockpileComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.misc.VectorUtils;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;
import technology.rocketjump.mountaincore.rooms.components.behaviour.TradeDepotBehaviour;

public class GoToPaymentLocationAction  extends GoToLocationAction {

	public GoToPaymentLocationAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	protected Vector2 selectDestination(GameContext gameContext) {
		if (parent.getPlannedTrade() != null) {
			if (parent.getPlannedTrade().getImportExportFurniture().getBehaviourComponent() instanceof TradingExportFurnitureBehaviour) {
				// This is a trade export for payment location is any suitable chest in the trading depot

				Entity paymentItemEntity = gameContext.getEntity(parent.getPlannedTrade().getPaymentItemAllocation().getTargetItemEntityId());
				// need to cancel item allocation so items are unallocated so they can find a stockpile
				ItemAllocationComponent paymentItemAllocationComponent = paymentItemEntity.getComponent(ItemAllocationComponent.class);
				paymentItemAllocationComponent.cancel(parent.getPlannedTrade().getPaymentItemAllocation());

				if (parent.parentEntity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour &&
					creatureBehaviour.getCreatureGroup() instanceof TraderCreatureGroup traderCreatureGroup) {
					MapTile tile = gameContext.getAreaMap().getTile(traderCreatureGroup.getHomeLocation());
					if (tile != null && tile.getRoomTile() != null && tile.getRoomTile().getRoom().getComponent(TradeDepotBehaviour.class) != null) {
						// find stockpile furniture stocking payment item
						for (Entity entityInRoom : tile.getRoomTile().getRoom().getRoomTiles().values().stream()
								.flatMap(r -> r.getTile().getEntities().stream()).toList()) {
							if (findStockpileWithSpaceFor(paymentItemEntity, entityInRoom, gameContext)) {
								FurnitureLayout.Workspace navigableWorkspace = FurnitureLayout.getNearestNavigableWorkspace(entityInRoom, gameContext.getAreaMap(), VectorUtils.toGridPoint(parent.parentEntity.getLocationComponent().getWorldOrParentPosition()));
								if (navigableWorkspace != null) {
									return VectorUtils.toVector(navigableWorkspace.getAccessedFrom());
								}
								return entityInRoom.getLocationComponent().getWorldOrParentPosition();
							}
						}
					}
				}

				// Could not find a chest/stockpile to place the payment into, need to re-create the payment allocation
				paymentItemAllocationComponent.cancelAll(ItemAllocation.Purpose.DUE_TO_BE_HAULED);
				ItemAllocation recreatedPaymentAllocation = paymentItemAllocationComponent.createAllocation(parent.getPlannedTrade().getPaymentItemAllocation().getAllocationAmount(),
						parent.parentEntity, ItemAllocation.Purpose.TRADING_PAYMENT);
				parent.getPlannedTrade().setPaymentItemAllocation(recreatedPaymentAllocation);
			} else {
				// This is a trade import so payment location is the chest containing the payment
				Entity itemEntity = gameContext.getEntity(parent.getPlannedTrade().getPaymentItemAllocation().getTargetItemEntityId());
				if (itemEntity != null) {
					Entity containerEntity = itemEntity.getLocationComponent().getContainerEntity();
					if (containerEntity != null && containerEntity.getPhysicalEntityComponent().getAttributes() instanceof FurnitureEntityAttributes) {
						FurnitureLayout.Workspace navigableWorkspace = FurnitureLayout.getNearestNavigableWorkspace(containerEntity, gameContext.getAreaMap(), VectorUtils.toGridPoint(parent.parentEntity.getLocationComponent().getWorldOrParentPosition()));
						if (navigableWorkspace != null) {
							return VectorUtils.toVector(navigableWorkspace.getAccessedFrom());
						}
					}
					return itemEntity.getLocationComponent().getWorldOrParentPosition();
				}
			}

		}
		return null;
	}

	private boolean findStockpileWithSpaceFor(Entity paymentItemEntity, Entity entity, GameContext gameContext) {
		if (entity.getType().equals(EntityType.FURNITURE)) {
			FurnitureStockpileComponent stockpileComponent = entity.getComponent(FurnitureStockpileComponent.class);
			if (stockpileComponent != null && stockpileComponent.getStockpileSettings().canHold(paymentItemEntity)) {
				HaulingAllocation haulingAllocation = stockpileComponent.getStockpile().requestAllocation(paymentItemEntity, gameContext.getAreaMap(), parent.parentEntity);
				if (haulingAllocation != null) {
					parent.setAssignedHaulingAllocation(haulingAllocation);
					return true;
				}
			}
		}
		return false;
	}
}
