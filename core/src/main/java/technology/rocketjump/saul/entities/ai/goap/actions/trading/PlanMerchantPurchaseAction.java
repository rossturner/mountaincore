package technology.rocketjump.saul.entities.ai.goap.actions.trading;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.saul.constants.CurrencyDefinition;
import technology.rocketjump.saul.constants.SettlementConstants;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.PlannedTrade;
import technology.rocketjump.saul.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.saul.entities.ai.goap.actions.Action;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.saul.entities.behaviour.creature.TraderCreatureGroup;
import technology.rocketjump.saul.entities.behaviour.furniture.TradingExportFurnitureBehaviour;
import technology.rocketjump.saul.entities.components.FactionComponent;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.components.ItemAllocation;
import technology.rocketjump.saul.entities.components.ItemAllocationComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.rooms.HaulingAllocation;
import technology.rocketjump.saul.rooms.HaulingAllocationBuilder;
import technology.rocketjump.saul.rooms.components.behaviour.TradeDepotBehaviour;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PlanMerchantPurchaseAction extends Action {

	public PlanMerchantPurchaseAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		if (parent.parentEntity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour &&
				creatureBehaviour.getCreatureGroup() instanceof TraderCreatureGroup traderCreatureGroup) {
			MapTile tile = gameContext.getAreaMap().getTile(traderCreatureGroup.getHomeLocation());
			if (tile != null && tile.getRoomTile() != null && tile.getRoomTile().getRoom().getComponent(TradeDepotBehaviour.class) != null) {

				List<Entity> availableWagons = tile.getRoomTile().getRoom().getRoomTiles().values().stream()
						.flatMap(roomTile -> roomTile.getTile().getEntities().stream())
						.filter(e -> e.getType().equals(EntityType.VEHICLE) && e.getOrCreateComponent(FactionComponent.class).getFaction().equals(parent.parentEntity.getOrCreateComponent(FactionComponent.class).getFaction()))
						// filter out any vehicles that have a trade in progress going to them
						.filter(e -> traderCreatureGroup.getPlannedTrades().stream().noneMatch(trade -> trade.getHaulingAllocation().getTargetId() == e.getId()))
						// filter to vehicles that still have inventory space available
						.filter(e -> e.getComponent(InventoryComponent.class).getInventoryEntries().size() < traderCreatureGroup.getCaravanDefinition().getVehicles().getMaxInventoryPerVehicle())
						.collect(Collectors.toList());

				if (!availableWagons.isEmpty()) {
					for (Entity tradeExportFurniture : tile.getRoomTile().getRoom().getRoomTiles().values().stream()
							.flatMap(roomTile -> roomTile.getTile().getEntities().stream())
							.filter(e -> e.getType().equals(EntityType.FURNITURE) && e.getBehaviourComponent() instanceof TradingExportFurnitureBehaviour)
							.collect(Collectors.toSet())) {

						TradingExportFurnitureBehaviour tradingExportFurnitureBehaviour = (TradingExportFurnitureBehaviour) tradeExportFurniture.getBehaviourComponent();
						InventoryComponent exportFurnitureInventory = tradeExportFurniture.getOrCreateComponent(InventoryComponent.class);

						if (tradingExportFurnitureBehaviour.getSelectedItemType() != null) {
							InventoryComponent.InventoryEntry itemInInventory = exportFurnitureInventory.findByItemType(tradingExportFurnitureBehaviour.getSelectedItemType(), gameContext.getGameClock());
							if (itemInInventory != null) {
								ItemAllocationComponent itemAllocationComponent = itemInInventory.entity.getOrCreateComponent(ItemAllocationComponent.class);
								ItemAllocation exportAllocation = itemAllocationComponent.getAllocationForPurpose(ItemAllocation.Purpose.TRADING_EXPORT);

								int amountToHaul = Math.min(
										exportAllocation == null ? 0 : exportAllocation.getAllocationAmount(),
										tradingExportFurnitureBehaviour.getSelectedItemType().getMaxHauledAtOnce()
								);
								if (amountToHaul > 0) {
									ItemEntityAttributes attributes = (ItemEntityAttributes) itemInInventory.entity.getPhysicalEntityComponent().getAttributes();
									int valueOfHauledAmount = attributes.getValuePerItem() * amountToHaul;

									ItemAllocation paymentAllocation = findPaymentFromInventory(valueOfHauledAmount, gameContext);
									if (paymentAllocation != null) {
										int remainder = exportAllocation.getAllocationAmount() - amountToHaul;

										itemAllocationComponent.cancel(exportAllocation);
										ItemAllocation hauledItemAllocation = itemAllocationComponent.createAllocation(amountToHaul, parent.parentEntity, ItemAllocation.Purpose.DUE_TO_BE_HAULED);
										if (remainder > 0) {
											itemAllocationComponent.createAllocation(remainder, itemInInventory.entity, ItemAllocation.Purpose.TRADING_EXPORT);
										}

										PlannedTrade plannedTrade = new PlannedTrade();

										HaulingAllocation haulingAllocation = HaulingAllocationBuilder
												.createWithItemAllocation(itemInInventory.entity, hauledItemAllocation)
												.toEntity(availableWagons.get(0));
										parent.setAssignedHaulingAllocation(haulingAllocation);
										plannedTrade.setHaulingAllocation(haulingAllocation);
										plannedTrade.setPaymentItemAllocation(paymentAllocation);
										plannedTrade.setImportExportFurniture(tradeExportFurniture);

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

	private ItemAllocation findPaymentFromInventory(int tradeValue, GameContext gameContext) {
		List<CurrencyDefinition> currencyDefinitions = new ArrayList<>();
		parent.messageDispatcher.dispatchMessage(MessageType.GET_SETTLEMENT_CONSTANTS, (Consumer<SettlementConstants>) (settlementConstants) -> {
			currencyDefinitions.addAll(settlementConstants.getCurrency());
		});
		currencyDefinitions.sort(Comparator.comparingInt(CurrencyDefinition::getValue));

		CurrencyDefinition smallDenomination = currencyDefinitions.get(0);
		CurrencyDefinition largeDenomination = currencyDefinitions.get(currencyDefinitions.size() - 1);

		InventoryComponent inventoryComponent = parent.parentEntity.getComponent(InventoryComponent.class);

		InventoryComponent.InventoryEntry smallDenominationEntry = inventoryComponent.findByItemTypeAndMaterial(smallDenomination.getItemType(), smallDenomination.getMaterial(), gameContext.getGameClock());
		InventoryComponent.InventoryEntry largeDenominationEntry = inventoryComponent.findByItemTypeAndMaterial(largeDenomination.getItemType(), largeDenomination.getMaterial(), gameContext.getGameClock());

		int largeDenominationRequired = tradeValue / largeDenomination.getValue();

		if (tradeValue % largeDenomination.getValue() == 0 && largeDenominationEntry != null) {
			ItemAllocation allocation = createAllocationIfAmountAvailable(largeDenominationEntry, largeDenominationRequired, gameContext);
			if (allocation != null) {
				return  allocation;
			}
		}

		int smallDenominationRequired = tradeValue/ smallDenomination.getValue();
		if (smallDenominationEntry != null) {
			ItemAllocation allocation = createAllocationIfAmountAvailable(smallDenominationEntry, smallDenominationRequired, gameContext);
			if (allocation != null) {
				return  allocation;
			}
		}

		largeDenominationRequired += 1;
		if (largeDenominationEntry != null) {
			ItemAllocation allocation = createAllocationIfAmountAvailable(largeDenominationEntry, largeDenominationRequired, gameContext);
			if (allocation != null) {
				return  allocation;
			}
		}
		return null;
	}

	private ItemAllocation createAllocationIfAmountAvailable(InventoryComponent.InventoryEntry inventoryEntry, int amountRequired, GameContext gameContext) {
		ItemAllocationComponent largeDenominationItemAllocationComponent = inventoryEntry.entity.getOrCreateComponent(ItemAllocationComponent.class);
		ItemAllocation heldInInventoryAllocation = largeDenominationItemAllocationComponent.getAllocationForPurpose(ItemAllocation.Purpose.HELD_IN_INVENTORY);
		if (heldInInventoryAllocation.getAllocationAmount() >= amountRequired) {
			int remainder = heldInInventoryAllocation.getAllocationAmount() - amountRequired;
			largeDenominationItemAllocationComponent.cancel(heldInInventoryAllocation);
			ItemAllocation paymentAllocation = largeDenominationItemAllocationComponent.createAllocation(amountRequired, parent.parentEntity, ItemAllocation.Purpose.TRADING_PAYMENT);
			if (remainder > 0) {
				largeDenominationItemAllocationComponent.createAllocation(remainder, parent.parentEntity, ItemAllocation.Purpose.HELD_IN_INVENTORY);
			}
			return paymentAllocation;
		}
		return null;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
	}
}
