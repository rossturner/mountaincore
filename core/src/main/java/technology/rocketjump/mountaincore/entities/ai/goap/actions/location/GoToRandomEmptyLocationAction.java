package technology.rocketjump.mountaincore.entities.ai.goap.actions.location;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.model.physical.LocationComponent;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.model.TiledMap;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import static technology.rocketjump.mountaincore.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.mountaincore.entities.ai.goap.actions.Action.CompletionType.SUCCESS;

public class GoToRandomEmptyLocationAction extends GoToLocationAction {

	private static final float MAX_ELAPSED_TIME = 10f;
	private static final int LIMIT = 10;

	private float elapsedTime;

	public GoToRandomEmptyLocationAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (overrideLocation == null) {
			Vector2 target = findEmptyLocation(gameContext);
			if (target != null) {
				overrideLocation = target;
			} else {
				completionType = FAILURE;
				return;
			}
		}

		elapsedTime += deltaTime;
		if (elapsedTime > MAX_ELAPSED_TIME) {
			completionType = SUCCESS;
		}

		super.update(deltaTime, gameContext);
	}

	private Vector2 findEmptyLocation(GameContext gameContext) {
		TiledMap areaMap = gameContext.getAreaMap();
		LocationComponent locationComponent = parent.parentEntity.getLocationComponent();
		
		MapTile startingTile = areaMap.getTile(locationComponent.getWorldPosition());

		Deque<MapTile> frontier = new ArrayDeque<>();
		Set<MapTile> explored = new HashSet<>();
		frontier.add(startingTile);

		while (!frontier.isEmpty()) {
			MapTile currentTile = frontier.pop();
			explored.add(currentTile);
			if (currentTile.isEmpty()) {
				return currentTile.getWorldPositionOfCenter();
			}

			for (MapTile neighbour : areaMap.getNeighbours(currentTile.getTileX(), currentTile.getTileY()).values()) {
				if (!explored.contains(neighbour) &&
						neighbour.getRegionId() == currentTile.getRegionId() &&
						distance(startingTile, currentTile) < LIMIT) {
					frontier.add(neighbour);
				}
			}
		}

		return null;
	}

	private int distance(MapTile a, MapTile b) {
		return Math.abs(a.getTileX() - b.getTileX()) + Math.abs(a.getTileY() - b.getTileY());
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);

		asJson.put("elapsed", elapsedTime);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		this.elapsedTime = asJson.getFloatValue("elapsed");
	}
}
