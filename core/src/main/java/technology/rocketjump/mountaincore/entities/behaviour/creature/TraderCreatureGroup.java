package technology.rocketjump.mountaincore.entities.behaviour.creature;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.ai.goap.PlannedTrade;
import technology.rocketjump.mountaincore.entities.ai.goap.SpecialGoal;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.TradingImportFurnitureBehaviour;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeWithMaterial;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.persistence.EnumParser;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.settlement.trading.model.TradeCaravanDefinition;

import java.util.ArrayList;
import java.util.List;

public class TraderCreatureGroup extends CreatureGroup {

	private TraderGroupStage stage = TraderGroupStage.SPAWNED;
	private SpecialGoal pendingSpecialGoal;
	private double hoursInCurrentStage;

	private List<PlannedTrade> plannedTrades = new ArrayList<>();
	private TradeCaravanDefinition caravanDefinition;

	@Override
	public void init(GameContext gameContext) {
		plannedTrades.forEach(p -> p.init(gameContext));
	}

	@Override
	public void infrequentUpdate(GameContext gameContext, MessageDispatcher messageDispatcher) {
		double now = gameContext.getGameClock().getCurrentGameTime();
		if (lastUpdateGameTime == 0) {
			lastUpdateGameTime = now;
		}
		double elapsed = now - lastUpdateGameTime;
		lastUpdateGameTime = now;
		hoursInCurrentStage += elapsed;

		// TODO leave after spending too long in current stage
		if (hoursInCurrentStage > stage.maxHoursInStage) {
			progressToNextStage();
		}

		switch (stage) {
			case SPAWNED -> {
				this.pendingSpecialGoal = SpecialGoal.MOVE_GROUP_TOWARDS_SETTLEMENT;
				progressToNextStage();
			}
			case PREPARING_TO_LEAVE -> {
				removeFurnitureAssignmentsAndUpdateItemsForNextVisit(gameContext);
			}
		}
	}

	private void removeFurnitureAssignmentsAndUpdateItemsForNextVisit(GameContext gameContext) {
		MapTile homeTile = gameContext.getAreaMap().getTile(homeLocation);
		if (homeTile != null && homeTile.getRoomTile() != null) {
			homeTile.getRoomTile().getRoom().getRoomTiles().values().stream()
					.flatMap(roomTile -> roomTile.getTile().getEntities().stream())
					.filter(entity -> entity.getType().equals(EntityType.FURNITURE))
					.map(e -> (FurnitureEntityAttributes)e.getPhysicalEntityComponent().getAttributes())
					.forEach(a -> {
						if (a.getAssignedToEntityId() != null && memberEntityIds.contains(a.getAssignedToEntityId())) {
							a.setAssignedToEntityId(null);
						}
					});

			List<ItemTypeWithMaterial> nextVisitItems = gameContext.getSettlementState().getTraderInfo().getRequestedItemsForNextVisit();
			nextVisitItems.clear();

			homeTile.getRoomTile().getRoom().getRoomTiles().values().stream()
					.flatMap(roomTile -> roomTile.getTile().getEntities().stream())
					.forEach(entity -> {
						if (entity.getBehaviourComponent() instanceof TradingImportFurnitureBehaviour importBehaviour) {
							if (importBehaviour.getSelectedItemType() != null) {
								ItemTypeWithMaterial request = new ItemTypeWithMaterial();
								request.setItemType(importBehaviour.getSelectedItemType());
								if (importBehaviour.getSelectedMaterial() != null) {
									request.setMaterial(importBehaviour.getSelectedMaterial());
								}
								nextVisitItems.add(request);
							}
						}
					});
		}

	}

	public List<PlannedTrade> getPlannedTrades() {
		return plannedTrades;
	}

	public void progressToNextStage() {
		this.stage = this.stage.nextStage();
		this.hoursInCurrentStage = 0;
	}

	public SpecialGoal popSpecialGoal() {
		if (pendingSpecialGoal != null) {
			SpecialGoal temp = this.pendingSpecialGoal;
			this.pendingSpecialGoal = null;
			return temp;
		} else {
			return null;
		}
	}

	public TraderGroupStage getStage() {
		return stage;
	}

	public void setCaravanDefinition(TradeCaravanDefinition caravanDefinition) {
		this.caravanDefinition = caravanDefinition;
	}

	public TradeCaravanDefinition getCaravanDefinition() {
		return caravanDefinition;
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(savedGameStateHolder);

		JSONObject asJson = savedGameStateHolder.creatureGroupJson.getJSONObject(savedGameStateHolder.creatureGroupJson.size() - 1);
		if (asJson.getLongValue("groupId") == this.groupId) {
			asJson.put("_class", getClass().getName());

			asJson.put("stage", stage.name());
			asJson.put("hoursInCurrentStage", hoursInCurrentStage);

			if (pendingSpecialGoal != null) {
				asJson.put("pendingSpecialGoal", pendingSpecialGoal.name());
			}

			if (!plannedTrades.isEmpty()) {
				JSONArray plannedTradesJson = new JSONArray();
				for (PlannedTrade plannedTrade : plannedTrades) {
					JSONObject plannedTradeJson = new JSONObject(true);
					plannedTrade.writeTo(plannedTradeJson, savedGameStateHolder);
					plannedTradesJson.add(plannedTradeJson);
				}
				asJson.put("plannedTrades", plannedTradesJson);
			}
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		this.stage = EnumParser.getEnumValue(asJson, "stage", TraderGroupStage.class, TraderGroupStage.ARRIVING);
		this.hoursInCurrentStage = asJson.getDoubleValue("hoursInCurrentStage");

		this.pendingSpecialGoal = EnumParser.getEnumValue(asJson, "pendingSpecialGoal", SpecialGoal.class, null);
		this.caravanDefinition = relatedStores.tradeCaravanDefinitionDictionary.get();

		JSONArray plannedTradesJson = asJson.getJSONArray("plannedTrades");
		if (plannedTradesJson != null) {
			for (int i = 0; i < plannedTradesJson.size(); i++) {
				JSONObject plannedTradeJson = plannedTradesJson.getJSONObject(i);
				PlannedTrade plannedTrade = new PlannedTrade();
				plannedTrade.readFrom(plannedTradeJson, savedGameStateHolder, relatedStores);
				plannedTrades.add(plannedTrade);
			}
		}
	}

}
