package technology.rocketjump.mountaincore.settlement;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import org.apache.commons.lang3.EnumUtils;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Race;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.gamecontext.GameState;
import technology.rocketjump.mountaincore.invasions.model.InvasionDefinition;
import technology.rocketjump.mountaincore.mapping.model.ImpendingMiningCollapse;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.misc.twitch.model.TwitchViewer;
import technology.rocketjump.mountaincore.persistence.EnumParser;
import technology.rocketjump.mountaincore.persistence.JSONUtils;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.Persistable;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.settlement.notifications.NotificationType;
import technology.rocketjump.mountaincore.settlement.production.CraftingQuota;
import technology.rocketjump.mountaincore.settlement.trading.model.TraderInfo;

import java.util.*;

/**
 * Simple bean to hold "global" settlement data
 */
public class SettlementState implements Persistable {

	private String settlementName;

	public final Map<Long, Entity> furnitureHoldingCompletedCooking = new HashMap<>();
	public final List<MapTile> activeLiquidFlowTiles = new ArrayList<>();

	public final List<ImpendingMiningCollapse> impendingMiningCollapses = new ArrayList<>();
	public final Map<String, Boolean> previousHints = new HashMap<>();
	public final List<String> currentHints = new ArrayList<>();
	public final Set<TwitchViewer> usedTwitchViewers = new HashSet<>();
	public final Map<InvasionDefinition, Integer> daysUntilNextInvasionCheck = new HashMap<>();
	public final Set<NotificationType> suppressedNotificationTypes = new HashSet<>();
	private final List<CraftingQuota> craftingQuotas = new ArrayList<>();

	private boolean allowImmigration = true;
	private int immigrantsDue;
	private int immigrantCounter;
	private Vector2 immigrationPoint;
	private Double nextImmigrationGameTime;
	private int fishRemainingInRiver;
	private float currentCombatRoundElapsed;
	private GameState gameState;
	private Race settlerRace;

	private InvasionDefinition incomingInvasion;
	private Double hoursUntilInvasion;
	private boolean peacefulMode;

	private final TraderInfo traderInfo = new TraderInfo();

	public String getSettlementName() {
		return settlementName;
	}

	public void setSettlementName(String settlementName) {
		this.settlementName = settlementName;
	}

	public boolean isAllowImmigration() {
		return allowImmigration;
	}

	public void setAllowImmigration(boolean allowImmigration) {
		this.allowImmigration = allowImmigration;
	}

	public int getImmigrantsDue() {
		return immigrantsDue;
	}

	public void setImmigrantsDue(int immigrantsDue) {
		this.immigrantsDue = immigrantsDue;
	}

	public Double getNextImmigrationGameTime() {
		return nextImmigrationGameTime;
	}

	public void setNextImmigrationGameTime(Double nextImmigrationGameTime) {
		this.nextImmigrationGameTime = nextImmigrationGameTime;
	}

	public void setImmigrantCounter(int numImmigrants) {
		this.immigrantCounter = numImmigrants;
	}

	public int getImmigrantCounter() {
		return immigrantCounter;
	}

	public Vector2 getImmigrationPoint() {
		return immigrationPoint;
	}

	public void setImmigrationPoint(Vector2 immigrationPoint) {
		this.immigrationPoint = immigrationPoint;
	}

	public boolean isGameOver() {
		return gameState.equals(GameState.GAME_OVER);
	}

	public Map<String, Boolean> getPreviousHints() {
		return previousHints;
	}

	public List<String> getCurrentHints() {
		return currentHints;
	}

	public GameState getGameState() {
		return gameState;
	}

	public void setGameState(GameState currentState) {
		this.gameState = currentState;
	}

	public int getFishRemainingInRiver() {
		return fishRemainingInRiver;
	}

	public void setFishRemainingInRiver(int fishRemainingInRiver) {
		this.fishRemainingInRiver = fishRemainingInRiver;
	}

	public InvasionDefinition getIncomingInvasion() {
		return incomingInvasion;
	}

	public void setIncomingInvasion(InvasionDefinition incomingInvasion) {
		this.incomingInvasion = incomingInvasion;
	}

	public Double getHoursUntilInvasion() {
		return hoursUntilInvasion;
	}

	public void setHoursUntilInvasion(Double hoursUntilInvasion) {
		this.hoursUntilInvasion = hoursUntilInvasion;
	}

	public boolean isPeacefulMode() {
		return peacefulMode;
	}

	public void setPeacefulMode(boolean peacefulMode) {
		this.peacefulMode = peacefulMode;
	}

	public Race getSettlerRace() {
		return settlerRace;
	}

	public void setSettlerRace(Race settlerRace) {
		this.settlerRace = settlerRace;
	}

	public float getCurrentCombatRoundElapsed() {
		return currentCombatRoundElapsed;
	}

	public void setCurrentCombatRoundElapsed(float currentCombatRoundElapsed) {
		this.currentCombatRoundElapsed = currentCombatRoundElapsed;
	}

	public TraderInfo getTraderInfo() {
		return traderInfo;
	}

	public CraftingQuota getCraftingQuota(ItemType itemType, GameMaterial material) {
		return craftingQuotas.stream()
				.filter(q -> Objects.equals(itemType, q.getItemType()) && Objects.equals(material, q.getGameMaterial()))
				.findAny()
				.orElse(CraftingQuota.UNLIMITED);
	}

	public CraftingQuota newCraftingQuota(ItemType itemType, GameMaterial material, int quantity) {
		removeCraftingQuota(itemType, material);
		CraftingQuota quota = new CraftingQuota();
		quota.setItemType(itemType);
		quota.setGameMaterial(material);
		quota.setQuantity(quantity);
		craftingQuotas.add(quota);
		return quota;
	}

	public void removeCraftingQuota(ItemType itemType, GameMaterial material) {
		craftingQuotas.removeIf(q -> Objects.equals(itemType, q.getItemType()) && Objects.equals(material, q.getGameMaterial()));
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		JSONObject asJson = savedGameStateHolder.settlementStateJson;

		asJson.put("settlementName", settlementName);
		asJson.put("peacefulMode", peacefulMode);

		JSONObject furnitureEntityJson = new JSONObject(true);
		for (Map.Entry<Long, Entity> entry : furnitureHoldingCompletedCooking.entrySet()) {
			furnitureEntityJson.put(String.valueOf(entry.getKey()), entry.getValue().getId());
		}
		asJson.put("furnitureHoldingCompletedCooking", furnitureEntityJson);

		JSONArray activeFlowTilesJson = new JSONArray();
		for (MapTile activeLiquidFlowTile : activeLiquidFlowTiles) {
			JSONObject activeFlowTileJson = JSONUtils.toJSON(activeLiquidFlowTile.getTilePosition());
			activeFlowTilesJson.add(activeFlowTileJson);
		}
		asJson.put("activeFlowTiles", activeFlowTilesJson);

		if (!previousHints.isEmpty()) {
			JSONArray previousHintsJson = new JSONArray();
			previousHintsJson.addAll(previousHints.keySet());
			asJson.put("previousHints", previousHintsJson);
		}

		if (!currentHints.isEmpty()) {
			JSONArray currentHintsJson = new JSONArray();
			currentHintsJson.addAll(currentHints);
			asJson.put("currentHints", currentHintsJson);
		}

		asJson.put("allowImmigration", allowImmigration);

		if (immigrantsDue != 0) {
			asJson.put("immigrantsDue", immigrantsDue);
		}
		if (immigrantCounter != 0) {
			asJson.put("immigrantCounter", immigrantCounter);
		}
		if (immigrationPoint != null) {
			asJson.put("immigrationPoint", JSONUtils.toJSON(immigrationPoint));
		}
		if (nextImmigrationGameTime != null) {
			asJson.put("nextImmigration", nextImmigrationGameTime);
		}

		if (!impendingMiningCollapses.isEmpty()) {
			JSONArray collapsesJson = new JSONArray();
			for (ImpendingMiningCollapse impendingMiningCollapse : impendingMiningCollapses) {
				JSONObject collapseJson = new JSONObject(true);
				impendingMiningCollapse.writeTo(collapseJson, savedGameStateHolder);
				collapsesJson.add(collapseJson);
			}
			asJson.put("impendingCollapses", collapsesJson);
		}

		if (!gameState.equals(GameState.NORMAL)) {
			asJson.put("gameState", gameState.name());
		}

		if (!usedTwitchViewers.isEmpty()) {
			JSONArray viewerNames = new JSONArray();
			for (TwitchViewer twitchViewer : usedTwitchViewers) {
				viewerNames.add(twitchViewer.getUsername());
			}
			asJson.put("usedTwitchViewers", viewerNames);
		}

		asJson.put("fishRemaining", fishRemainingInRiver);

		if (!suppressedNotificationTypes.isEmpty()) {
			JSONArray suppressedNotificationJson = new JSONArray();
			for (NotificationType notificationType : suppressedNotificationTypes) {
				suppressedNotificationJson.add(notificationType.name());
			}
			asJson.put("suppressedNotificationTypes", suppressedNotificationJson);
		}

		if (settlerRace != null) {
			asJson.put("settlerRace", settlerRace.getName());
		}

		asJson.put("currentCombatRoundElapsed", currentCombatRoundElapsed);

		JSONObject invasionCheckJson = new JSONObject(true);
		for (Map.Entry<InvasionDefinition, Integer> entry : daysUntilNextInvasionCheck.entrySet()) {
			invasionCheckJson.put(entry.getKey().getName(), entry.getValue());
		}
		asJson.put("daysUntilNextInvasionCheck", invasionCheckJson);

		if (incomingInvasion != null) {
			asJson.put("incomingInvasion", incomingInvasion.getName());
		}
		if (hoursUntilInvasion != null) {
			asJson.put("hoursUntilInvasion", hoursUntilInvasion);
		}

		JSONObject traderInfoJson = new JSONObject(true);
		traderInfo.writeTo(traderInfoJson, savedGameStateHolder);
		asJson.put("traderInfo", traderInfoJson);

		JSONArray craftingQuotasJson = new JSONArray();
		for (CraftingQuota craftingQuota : craftingQuotas) {
			JSONObject quotaJson = new JSONObject();
			quotaJson.put("quantity", craftingQuota.getQuantity());
			if (craftingQuota.getGameMaterial() != null) {
				quotaJson.put("gameMaterial", craftingQuota.getGameMaterial().getMaterialName());
			}
			if (craftingQuota.getItemType() != null) {
				quotaJson.put("itemType", craftingQuota.getItemType().getItemTypeName());
			}
			craftingQuotasJson.add(quotaJson);
		}
		asJson.put("craftingQuotas", craftingQuotasJson);

		savedGameStateHolder.setSettlementState(this);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.settlementName = asJson.getString("settlementName");
		if (this.settlementName == null) {
			throw new InvalidSaveException("Settlement name not specified");
		}
		this.peacefulMode = asJson.getBooleanValue("peacefulMode");

		JSONObject furnitureEntityJson = asJson.getJSONObject("furnitureHoldingCompletedCooking");
		for (String keyString : furnitureEntityJson.keySet()) {
			long key = Long.valueOf(keyString);
			long value = furnitureEntityJson.getLongValue(keyString);
			Entity entity = savedGameStateHolder.entities.get(value);
			if (entity == null) {
				throw new InvalidSaveException("Could not find entity by ID " + value);
			}
			furnitureHoldingCompletedCooking.put(key, entity);
		}

		JSONArray activeFlowTilesJson = asJson.getJSONArray("activeFlowTiles");
		if (activeFlowTilesJson != null) {
			for (int cursor = 0; cursor < activeFlowTilesJson.size(); cursor++) {
				JSONObject tilePositionJson = activeFlowTilesJson.getJSONObject(cursor);
				GridPoint2 location = JSONUtils.gridPoint2(tilePositionJson);
				MapTile tile = savedGameStateHolder.getMap().getTile(location);
				if (tile == null) {
					throw new InvalidSaveException("Can not find tile at " + location);
				} else {
					activeLiquidFlowTiles.add(tile);
				}
			}
		}

		JSONArray previousHintsJson = asJson.getJSONArray("previousHints");
		if (previousHintsJson != null) {
			for (Object hintIdObj : previousHintsJson) {
				previousHints.put(hintIdObj.toString(), true);
			}
		}

		JSONArray currentHintsJson = asJson.getJSONArray("currentHints");
		if (currentHintsJson != null) {
			for (Object o : currentHintsJson) {
				currentHints.add(o.toString());
			}
		}

		this.allowImmigration = asJson.getBooleanValue("allowImmigration");
		this.immigrantsDue = asJson.getIntValue("immigrantsDue");
		this.immigrantCounter = asJson.getIntValue("immigrantCounter");
		this.immigrationPoint = JSONUtils.vector2(asJson.getJSONObject("immigrationPoint"));
		this.nextImmigrationGameTime = asJson.getDouble("nextImmigration");
		this.currentCombatRoundElapsed = asJson.getFloatValue("currentCombatRoundElapsed");

		JSONArray collapsesJson = asJson.getJSONArray("impendingCollapses");
		if (collapsesJson != null) {
			for (int cursor = 0; cursor < collapsesJson.size(); cursor++) {
				JSONObject collapseJson = collapsesJson.getJSONObject(cursor);
				ImpendingMiningCollapse impendingCollapse = new ImpendingMiningCollapse();
				impendingCollapse.readFrom(collapseJson, savedGameStateHolder, relatedStores);
				this.impendingMiningCollapses.add(impendingCollapse);
			}
		}

		this.gameState = EnumParser.getEnumValue(asJson, "gameState", GameState.class, GameState.NORMAL);


		JSONArray usedTwitchViewersJson = asJson.getJSONArray("usedTwitchViewers");
		if (usedTwitchViewersJson != null) {
			for (Object o : usedTwitchViewersJson) {
				usedTwitchViewers.add(new TwitchViewer(o.toString()));
			}
		}

		JSONArray suppressedNotificationJson = asJson.getJSONArray("suppressedNotificationTypes");
		if (suppressedNotificationJson != null) {
			for (Object o : suppressedNotificationJson) {
				NotificationType type = EnumUtils.getEnum(NotificationType.class, o.toString());
				if (type == null) {
					throw new InvalidSaveException("No notification type with name " + o.toString());
				} else {
					suppressedNotificationTypes.add(type);
				}
			}
		}

		String settlerRaceName = asJson.getString("settlerRace");
		if (settlerRaceName == null) {
			settlerRaceName = "Dwarf";
		}
		this.settlerRace = relatedStores.raceDictionary.getByName(settlerRaceName);

		this.fishRemainingInRiver = asJson.getIntValue("fishRemaining");

		JSONObject invasionCheckJson = asJson.getJSONObject("daysUntilNextInvasionCheck");
		for (String invasionName : invasionCheckJson.keySet()) {
			InvasionDefinition invasionDefinition = relatedStores.invasionDefinitionDictionary.getByName(invasionName);
			if (invasionDefinition == null) {
				throw new InvalidSaveException("Could not find invasion with name " + invasionName);
			} else {
				daysUntilNextInvasionCheck.put(invasionDefinition, invasionCheckJson.getInteger(invasionName));
			}
		}

		if (asJson.getString("incomingInvasion") != null) {
			this.incomingInvasion = relatedStores.invasionDefinitionDictionary.getByName(asJson.getString("incomingInvasion"));
			if (this.incomingInvasion == null) {
				throw new InvalidSaveException("Could not find invasion with name " + asJson.getString("incomingInvasion"));
			}
		}
		this.hoursUntilInvasion = asJson.getDouble("hoursUntilInvasion");

		JSONObject traderInfoJson = asJson.getJSONObject("traderInfo");
		traderInfo.readFrom(traderInfoJson, savedGameStateHolder, relatedStores);

		JSONArray craftingQuotasJson = asJson.getJSONArray("craftingQuotas");
		if (craftingQuotasJson != null) {
			for (int i = 0; i < craftingQuotasJson.size(); i++) {
				JSONObject quotaJson = craftingQuotasJson.getJSONObject(i);
				CraftingQuota craftingQuota = new CraftingQuota();
				craftingQuota.setQuantity(quotaJson.getIntValue("quantity"));
				if (quotaJson.getString("gameMaterial") != null) {
					GameMaterial gameMaterial = relatedStores.gameMaterialDictionary.getByName(quotaJson.getString("gameMaterial"));
					craftingQuota.setGameMaterial(gameMaterial);
				}
				if (quotaJson.getString("itemType") != null) {
					ItemType itemType = relatedStores.itemTypeDictionary.getByName(quotaJson.getString("itemType"));
					craftingQuota.setItemType(itemType);
				}
				craftingQuotas.add(craftingQuota);
			}
		}

	}

}
