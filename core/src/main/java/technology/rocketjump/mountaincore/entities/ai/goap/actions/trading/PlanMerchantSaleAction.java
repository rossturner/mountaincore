package technology.rocketjump.mountaincore.entities.ai.goap.actions.trading;

import com.alibaba.fastjson.JSONObject;
import com.google.common.math.IntMath;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.constants.CurrencyDefinition;
import technology.rocketjump.mountaincore.constants.SettlementConstants;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.PlannedTrade;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.mountaincore.entities.behaviour.creature.TraderCreatureGroup;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.TradingImportFurnitureBehaviour;
import technology.rocketjump.mountaincore.entities.components.FactionComponent;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.components.ItemAllocation;
import technology.rocketjump.mountaincore.entities.components.ItemAllocationComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;
import technology.rocketjump.mountaincore.rooms.HaulingAllocationBuilder;
import technology.rocketjump.mountaincore.rooms.Room;
import technology.rocketjump.mountaincore.rooms.components.behaviour.TradeDepotBehaviour;
import technology.rocketjump.mountaincore.rooms.tags.StockpileTag;

import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PlanMerchantSaleAction extends Action {

	public PlanMerchantSaleAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		if (parent.parentEntity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour &&
				creatureBehaviour.getCreatureGroup() instanceof TraderCreatureGroup traderCreatureGroup) {
			MapTile tile = gameContext.getAreaMap().getTile(traderCreatureGroup.getHomeLocation());
			if (tile != null && tile.getRoomTile() != null && tile.getRoomTile().getRoom().getComponent(TradeDepotBehaviour.class) != null) {

				Set<Entity> myFactionVehiclesInRoom = tile.getRoomTile().getRoom().getRoomTiles().values().stream()
						.flatMap(roomTile -> roomTile.getTile().getEntities().stream())
						.filter(e -> e.getType().equals(EntityType.VEHICLE) && e.getOrCreateComponent(FactionComponent.class).getFaction().equals(parent.parentEntity.getOrCreateComponent(FactionComponent.class).getFaction()))
						.collect(Collectors.toSet());

				for (Entity tradeImportFurniture : tile.getRoomTile().getRoom().getRoomTiles().values().stream()
						.flatMap(roomTile -> roomTile.getTile().getEntities().stream())
						.filter(e -> e.getType().equals(EntityType.FURNITURE) && e.getBehaviourComponent() instanceof TradingImportFurnitureBehaviour)
						.collect(Collectors.toSet())) {

					// Removing trades in progress removes any issues with multiple items going to or from a pallet at the same time
					if (currentTradeInProgress(tradeImportFurniture, traderCreatureGroup)) {
						continue;
					}

					TradingImportFurnitureBehaviour tradingImportFurnitureBehaviour = (TradingImportFurnitureBehaviour) tradeImportFurniture.getBehaviourComponent();
					InventoryComponent importFurnitureInventory = tradeImportFurniture.getOrCreateComponent(InventoryComponent.class);
					GameMaterial materialRequired = tradingImportFurnitureBehaviour.getSelectedMaterial();

					if (tradingImportFurnitureBehaviour.getSelectedItemType() != null) {
						int stackSizeAvailable = 0;

						if (importFurnitureInventory.isEmpty()) {
							stackSizeAvailable = tradingImportFurnitureBehaviour.getSelectedItemType().getMaxStackSize();
						} else {
							InventoryComponent.InventoryEntry existingEntry =
									tradingImportFurnitureBehaviour.getSelectedMaterial() != null ?
									importFurnitureInventory.findByItemTypeAndMaterial(tradingImportFurnitureBehaviour.getSelectedItemType(), tradingImportFurnitureBehaviour.getSelectedMaterial(), gameContext.getGameClock()) :
									importFurnitureInventory.findByItemType(tradingImportFurnitureBehaviour.getSelectedItemType(), gameContext.getGameClock());
							if (existingEntry != null) {
								ItemEntityAttributes attributes = (ItemEntityAttributes) existingEntry.entity.getPhysicalEntityComponent().getAttributes();
								stackSizeAvailable = tradingImportFurnitureBehaviour.getSelectedItemType().getMaxStackSize() - attributes.getQuantity();
								materialRequired = attributes.getPrimaryMaterial(); // if merging into stack, have to match on material
							}
						}

						if (stackSizeAvailable > 0) {
							ItemAllocation allocationFromWagon = createAllocation(tradingImportFurnitureBehaviour.getSelectedItemType(), materialRequired, stackSizeAvailable, myFactionVehiclesInRoom, gameContext);

							if (allocationFromWagon != null) {
								Entity allocatedItem = gameContext.getEntity(allocationFromWagon.getTargetItemEntityId());

								ItemPayment payment = findPayment(getValuePerItem(allocationFromWagon, gameContext), allocationFromWagon.getAllocationAmount(), tile.getRoomTile().getRoom(), allocationFromWagon, gameContext);

								parent.messageDispatcher.dispatchMessage(MessageType.CANCEL_ITEM_ALLOCATION, allocationFromWagon);
								if (payment != null && payment.amountToPurchase > 0) {
									// recreate allocation as it may be less than the full stack size
									allocationFromWagon = createAllocation(tradingImportFurnitureBehaviour.getSelectedItemType(), materialRequired, payment.amountToPurchase, myFactionVehiclesInRoom, gameContext);

									if (allocationFromWagon != null) {
										PlannedTrade plannedTrade = new PlannedTrade();
										HaulingAllocation haulingAllocation = HaulingAllocationBuilder
												.createWithItemAllocation(allocatedItem, allocationFromWagon)
												.toEntity(tradeImportFurniture);
										parent.setAssignedHaulingAllocation(haulingAllocation);
										plannedTrade.setHaulingAllocation(haulingAllocation);
										plannedTrade.setPaymentItemAllocation(payment.paymentAllocation);
										plannedTrade.setImportExportFurniture(tradeImportFurniture);

										parent.setPlannedTrade(plannedTrade);
										traderCreatureGroup.getPlannedTrades().add(plannedTrade);
										completionType = CompletionType.SUCCESS;
										return;
									}
								}
							}
						}
					}
				}
			}
		}

		completionType = CompletionType.FAILURE;
	}

	private ItemAllocation createAllocation(ItemType itemType, GameMaterial materialRequired, int maxQuantity, Set<Entity> vehiclesWithInventory, GameContext gameContext) {
		for (Entity vehicle : vehiclesWithInventory) {
			InventoryComponent inventoryComponent = vehicle.getOrCreateComponent(InventoryComponent.class);
			InventoryComponent.InventoryEntry inventoryEntry = materialRequired == null ?
					inventoryComponent.findByItemType(itemType, gameContext.getGameClock()) :
					inventoryComponent.findByItemTypeAndMaterial(itemType, materialRequired, gameContext.getGameClock());
			if (inventoryEntry != null) {
				ItemAllocationComponent itemAllocationComponent = inventoryEntry.entity.getOrCreateComponent(ItemAllocationComponent.class);
				int quantityToAllocate = Math.min(Math.min(itemAllocationComponent.getNumUnallocated(), maxQuantity), itemType.getMaxHauledAtOnce());
				if (quantityToAllocate > 0) {
					return itemAllocationComponent.createAllocation(quantityToAllocate, parent.parentEntity, ItemAllocation.Purpose.TRADING_EXPORT);
				}
			}
		}
		return null;
	}

	private int getValuePerItem(ItemAllocation itemAllocation, GameContext gameContext) {
		Entity entity = gameContext.getEntities().get(itemAllocation.getTargetItemEntityId());
		if (entity == null) {
			Logger.error("Entity just selected for trade is null");
			return Integer.MAX_VALUE;
		} else {
			ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
			return attributes.getValuePerItem();
		}
	}

	private ItemPayment findPayment(int valuePerItem, int maxAmountAvailable, Room tradeDepotRoom, ItemAllocation itemAllocationFromWagon, GameContext gameContext) {
		List<CurrencyDefinition> currencyDefinitions = new ArrayList<>();
		parent.messageDispatcher.dispatchMessage(MessageType.GET_SETTLEMENT_CONSTANTS, (Consumer<SettlementConstants>) (settlementConstants) -> {
			currencyDefinitions.addAll(settlementConstants.getCurrency());
		});

		for (Entity stockpileFurniture : tradeDepotRoom.getRoomTiles().values().stream()
				.flatMap(roomTile -> roomTile.getTile().getEntities().stream())
				.filter(e -> e.getType().equals(EntityType.FURNITURE) && e.getTag(StockpileTag.class) != null)
				.collect(Collectors.toSet())) {

			InventoryComponent stockpileInventory = stockpileFurniture.getOrCreateComponent(InventoryComponent.class);
			for (CurrencyDefinition currencyDefinition : currencyDefinitions) {
				InventoryComponent.InventoryEntry inventoryEntry = stockpileInventory.findByItemTypeAndMaterial(currencyDefinition.getItemType(), currencyDefinition.getMaterial(), gameContext.getGameClock());
				if (inventoryEntry != null) {
					ItemAllocationComponent itemAllocationComponent = inventoryEntry.entity.getOrCreateComponent(ItemAllocationComponent.class);
					int currencyQuantityAvailable = itemAllocationComponent.getNumUnallocated();
					int currencyValueAvailable = currencyQuantityAvailable * currencyDefinition.getValue();

					if (currencyValueAvailable >= valuePerItem) {
						int amountToPurchase;
						if (currencyValueAvailable >= valuePerItem * maxAmountAvailable) {
							amountToPurchase = maxAmountAvailable;
						} else {
							amountToPurchase = currencyValueAvailable / valuePerItem;
						}

						int currencyQuantityRequired = Math.min(IntMath.divide(amountToPurchase * valuePerItem, currencyDefinition.getValue(), RoundingMode.CEILING), currencyQuantityAvailable);
						ItemAllocation currencyAllocation = itemAllocationComponent.createAllocation(currencyQuantityRequired, parent.parentEntity, ItemAllocation.Purpose.TRADING_PAYMENT);

						return new ItemPayment(currencyAllocation, amountToPurchase);
					}
				}
			}
		}
		return null;
	}

	private boolean currentTradeInProgress(Entity tradeImportFurniture, TraderCreatureGroup traderCreatureGroup) {
		return traderCreatureGroup.getPlannedTrades().stream()
				.anyMatch(trade -> trade.getImportExportFurniture().equals(tradeImportFurniture));
	}

	private class ItemPayment {
		public final ItemAllocation paymentAllocation;
		public final int amountToPurchase;

		private ItemPayment(ItemAllocation paymentAllocation, int amountToPurchase) {
			this.paymentAllocation = paymentAllocation;
			this.amountToPurchase = amountToPurchase;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
	}
}
