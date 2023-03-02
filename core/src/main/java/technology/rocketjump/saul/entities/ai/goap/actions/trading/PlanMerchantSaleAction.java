package technology.rocketjump.saul.entities.ai.goap.actions.trading;

import com.alibaba.fastjson.JSONObject;
import com.google.common.math.IntMath;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.constants.CurrencyDefinition;
import technology.rocketjump.saul.constants.SettlementConstants;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.PlannedTrade;
import technology.rocketjump.saul.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.saul.entities.ai.goap.actions.Action;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.saul.entities.behaviour.creature.TraderCreatureGroup;
import technology.rocketjump.saul.entities.behaviour.furniture.TradingImportFurnitureBehaviour;
import technology.rocketjump.saul.entities.components.FactionComponent;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.components.ItemAllocation;
import technology.rocketjump.saul.entities.components.ItemAllocationComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.rooms.HaulingAllocation;
import technology.rocketjump.saul.rooms.HaulingAllocationBuilder;
import technology.rocketjump.saul.rooms.Room;
import technology.rocketjump.saul.rooms.components.behaviour.TradeDepotBehaviour;
import technology.rocketjump.saul.rooms.tags.StockpileTag;

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

								ItemAllocation paymentAllocation = findPayment(calculateTradeValue(allocationFromWagon, gameContext), tile.getRoomTile().getRoom(), gameContext);


								if (paymentAllocation == null) {
									parent.messageDispatcher.dispatchMessage(MessageType.CANCEL_ITEM_ALLOCATION, allocationFromWagon);
								} else {
									PlannedTrade plannedTrade = new PlannedTrade();

									HaulingAllocation haulingAllocation = HaulingAllocationBuilder
											.createWithItemAllocation(allocatedItem, allocationFromWagon)
											.toEntity(tradeImportFurniture);
									parent.setAssignedHaulingAllocation(haulingAllocation);
									plannedTrade.setHaulingAllocation(haulingAllocation);
									plannedTrade.setPaymentItemAllocation(paymentAllocation);
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

	private int calculateTradeValue(ItemAllocation itemAllocation, GameContext gameContext) {
		Entity entity = gameContext.getEntities().get(itemAllocation.getTargetItemEntityId());
		if (entity == null) {
			Logger.error("Entity just selected for trade is null");
			return Integer.MAX_VALUE;
		} else {
			ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
			return attributes.getValuePerItem() * itemAllocation.getAllocationAmount();
		}
	}

	private ItemAllocation findPayment(int tradeValue, Room tradeDepotRoom, GameContext gameContext) {
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
					int quantityAvailable = itemAllocationComponent.getNumUnallocated();
					int valueAvailable = quantityAvailable * currencyDefinition.getValue();

					if (tradeValue < valueAvailable) {
						int quantityRequired = Math.min(IntMath.divide(tradeValue, currencyDefinition.getValue(), RoundingMode.CEILING), quantityAvailable);
						return itemAllocationComponent.createAllocation(quantityRequired, parent.parentEntity, ItemAllocation.Purpose.TRADING_PAYMENT);
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

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
	}
}
