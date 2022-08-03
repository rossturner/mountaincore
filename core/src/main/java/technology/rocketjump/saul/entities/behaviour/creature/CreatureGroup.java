package technology.rocketjump.saul.entities.behaviour.creature;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.saul.entities.components.creature.MemoryComponent;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.tile.CompassDirection;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.persistence.JSONUtils;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.Persistable;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.*;

/**
 * This is used to track a group of Creatures such as a herd of deer, so they can move around a central point
 */
public class CreatureGroup implements Persistable {

	private static final double GAME_TIME_BETWEEN_UPDATES = 0.2;
	private long groupId;
	private GridPoint2 homeLocation;
	private double lastUpdateGameTime;
	private Set<Long> memberEntityIds = new HashSet<>();

	private final MemoryComponent sharedMemoryComponent = new MemoryComponent();

	public long getGroupId() {
		return groupId;
	}

	public void setGroupId(long groupId) {
		this.groupId = groupId;
	}

	public GridPoint2 getHomeLocation() {
		return homeLocation;
	}

	public void setHomeLocation(GridPoint2 homeLocation) {
		this.homeLocation = homeLocation;
	}

	/**
	 * This is called by child entity infrequent updates so it is not accurately updated, but "every so often" is good enough
	 */
	public void infrequentUpdate(GameContext gameContext) {
		double now = gameContext.getGameClock().getCurrentGameTime();
		if (now - lastUpdateGameTime > GAME_TIME_BETWEEN_UPDATES) {
			lastUpdateGameTime = now;
			moveHomeLocation(gameContext);
		}
	}

	public MemoryComponent getSharedMemoryComponent() {
		return sharedMemoryComponent;
	}

	/**
	 * This moves the home location by one tile, orthogonally, randomly
	 */
	private void moveHomeLocation(GameContext gameContext) {
		List<CompassDirection> directions = new ArrayList<>(CompassDirection.CARDINAL_DIRECTIONS);
		Collections.shuffle(directions, gameContext.getRandom());

		for (CompassDirection direction : directions) {
			MapTile adjacentTile = gameContext.getAreaMap().getTile(homeLocation.x + direction.getXOffset(), homeLocation.y + direction.getYOffset());
			if (adjacentTile != null && adjacentTile.isNavigable(null) && !adjacentTile.hasDoorway()) {
				this.homeLocation = adjacentTile.getTilePosition();
				break;
			}
		}
	}

	public void addMemberId(long entityId) {
		memberEntityIds.add(entityId);
	}

	public void removeMemberId(long entityId) {
		memberEntityIds.remove(entityId);
	}

	public Set<Long> getMemberIds() {
		return memberEntityIds;
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		if (savedGameStateHolder.creatureGroups.containsKey(groupId)) {
			return;
		}
		JSONObject asJson = new JSONObject(true);

		asJson.put("groupId", groupId);
		asJson.put("home", JSONUtils.toJSON(homeLocation));

		JSONObject memoryJson = new JSONObject(true);
		sharedMemoryComponent.writeTo(memoryJson, savedGameStateHolder);
		asJson.put("memories", memoryJson);

		JSONArray memberIdsJson = new JSONArray();
		memberIdsJson.addAll(memberEntityIds);
		asJson.put("memberIds", memberIdsJson);

		savedGameStateHolder.creatureGroupJson.add(asJson);
		savedGameStateHolder.creatureGroups.put(groupId, this);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.groupId = asJson.getLong("groupId");
		this.homeLocation = JSONUtils.gridPoint2(asJson.getJSONObject("home"));

		JSONObject memoryJson = asJson.getJSONObject("memories");
		sharedMemoryComponent.readFrom(memoryJson, savedGameStateHolder, relatedStores);

		JSONArray memberIdsJson = asJson.getJSONArray("memberIds");
		for (Object o : memberIdsJson) {
			this.memberEntityIds.add((Long)o);
		}

		savedGameStateHolder.creatureGroups.put(groupId, this);
	}
}
