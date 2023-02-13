package technology.rocketjump.saul.mapping.tile.underground;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.saul.entities.SequentialIdGenerator;
import technology.rocketjump.saul.entities.behaviour.furniture.PowerSourceBehaviour;
import technology.rocketjump.saul.entities.components.BehaviourComponent;
import technology.rocketjump.saul.entities.components.furniture.PoweredFurnitureComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.mechanism.MechanismEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.mechanism.MechanismType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.tile.CompassDirection;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.persistence.JSONUtils;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.Persistable;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.*;

import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;

public class PowerGrid implements Persistable {

	private long powerGridId;
	private Set<MapTile> tiles = new HashSet<>();
	private int totalPowerAvailable;

	public PowerGrid() {

	}

	public PowerGrid(long id) {
		powerGridId = id;
	}

	public long getPowerGridId() {
		return powerGridId;
	}

	public void addTile(MapTile tile) {
		this.tiles.add(tile);
		UnderTile underTile = tile.getUnderTile();
		underTile.setPowerGrid(this);
	}


	public PowerGrid mergeIn(PowerGrid other, GameContext gameContext) {
		for (MapTile tile : other.tiles) {
			addTile(tile);
		}

		update(gameContext);

		return this;
	}

	public void update(GameContext gameContext) {
		totalPowerAvailable = 0;
		for (MapTile tile : tiles) {
			for (Entity entity : tile.getEntities()) {
				// entity type null during loading
				if (entity.getType().equals(EntityType.FURNITURE)) {
					PoweredFurnitureComponent poweredFurnitureComponent = entity.getComponent(PoweredFurnitureComponent.class);
					if (poweredFurnitureComponent != null && toGridPoint(entity.getLocationComponent(true).getWorldPosition()).equals(tile.getTilePosition())) {
						// Only if this is furniture's main position
						BehaviourComponent behaviourComponent = entity.getBehaviourComponent();
						if (behaviourComponent instanceof PowerSourceBehaviour) {
							if (!((PowerSourceBehaviour)entity.getBehaviourComponent()).isWorking(gameContext)) {
								continue;
							}
						}
						totalPowerAvailable += poweredFurnitureComponent.getPowerAmount();
					}
				}
			}
		}

	}

	public int getTotalPowerAvailable() {
		return totalPowerAvailable;
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		if (savedGameStateHolder.powerGrids.containsKey(this.powerGridId)) {
			return;
		}

		JSONObject asJson = new JSONObject(true);
		asJson.put("id", powerGridId);

		JSONArray tilesJson = new JSONArray();
		for (MapTile tile : tiles) {
			tilesJson.add(JSONUtils.toJSON(tile.getTilePosition()));
		}
		asJson.put("tiles", tilesJson);

		if (totalPowerAvailable != 0) {
			asJson.put("totalPower", totalPowerAvailable);
		}

		savedGameStateHolder.powerGrids.put(powerGridId, this);
		savedGameStateHolder.powerGridJson.add(asJson);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.powerGridId = asJson.getLongValue("id");

		JSONArray tilesJson = asJson.getJSONArray("tiles");
		if (tilesJson == null) {
			throw new InvalidSaveException("No tiles json for power grid " + powerGridId);
		} else {
			for (int cursor = 0; cursor < tilesJson.size(); cursor++) {
				GridPoint2 location = JSONUtils.gridPoint2(tilesJson.getJSONObject(cursor));
				MapTile tile = savedGameStateHolder.getMap().getTile(location);
				addTile(tile);
			}
		}

		totalPowerAvailable = asJson.getIntValue("totalPower");
	}

	public void removeTile(MapTile mapTile, GameContext gameContext) {
		tiles.remove(mapTile);
		mapTile.getUnderTile().setPowerGrid(null);

		List<MapTile> remainingTiles = new ArrayList<>(tiles);
		int gridCursor = 0;
		while (!remainingTiles.isEmpty()) {
			MapTile initialTile = remainingTiles.remove(0);
			Deque<MapTile> frontier = new ArrayDeque<>();
			Set<MapTile> visited = new HashSet<>();
			Set<MapTile> newGridTiles = new HashSet<>();
			frontier.add(initialTile);

			while (!frontier.isEmpty()) {
				MapTile currentTile = frontier.pop();
				newGridTiles.add(currentTile);

				if (currentTile.getUnderTile() != null && currentTile.getUnderTile().getPowerMechanismEntity() != null) {
					MechanismEntityAttributes attributes = (MechanismEntityAttributes) currentTile.getUnderTile().getPowerMechanismEntity().getPhysicalEntityComponent().getAttributes();
					MechanismType mechanismType = attributes.getMechanismType();
					for (CompassDirection direction : mechanismType.getPowerTransmission()) {
						MapTile neighbour = gameContext.getAreaMap().getTile(currentTile.getTileX() + direction.getXOffset(), currentTile.getTileY() + direction.getYOffset());
						if (!visited.contains(neighbour)) {
							if (neighbour.getUnderTile() != null && neighbour.getUnderTile().getPowerMechanismEntity() != null) {
								MechanismEntityAttributes neighbourAttributes = (MechanismEntityAttributes) neighbour.getUnderTile().getPowerMechanismEntity().getPhysicalEntityComponent().getAttributes();
								if (neighbourAttributes.getMechanismType().getPowerTransmission().contains(CompassDirection.oppositeOf(direction))) {
									frontier.add(neighbour);
								}
							}
						}
					}
				}

				visited.add(currentTile);
			}

			if (gridCursor > 0) {
				// This needs to be a new power grid
				PowerGrid newGrid = new PowerGrid(SequentialIdGenerator.nextId());
				for (MapTile newGridTile : newGridTiles) {
					this.tiles.remove(newGridTile);
					newGrid.addTile(newGridTile);
				}
				newGrid.update(gameContext);
			}

			remainingTiles.removeAll(newGridTiles);

			gridCursor++;
		}
		this.update(gameContext);
	}
}
