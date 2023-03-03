package technology.rocketjump.saul.settlement;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import org.apache.commons.lang3.EnumUtils;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.gamecontext.GameState;
import technology.rocketjump.saul.invasions.model.InvasionDefinition;
import technology.rocketjump.saul.mapping.model.ImpendingMiningCollapse;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.misc.twitch.model.TwitchViewer;
import technology.rocketjump.saul.persistence.EnumParser;
import technology.rocketjump.saul.persistence.JSONUtils;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.Persistable;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.settlement.notifications.NotificationType;
import technology.rocketjump.saul.settlement.trading.model.TraderInfo;

import java.util.*;

import static technology.rocketjump.saul.gamecontext.GameState.GAME_OVER;
import static technology.rocketjump.saul.gamecontext.GameState.NORMAL;

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
		return gameState.equals(GAME_OVER);
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

		if (!gameState.equals(NORMAL)) {
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

		this.gameState = EnumParser.getEnumValue(asJson, "gameState", GameState.class, NORMAL);


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
	}

}
