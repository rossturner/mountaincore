package technology.rocketjump.mountaincore.mapping.tile;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.mountaincore.assets.model.FloorType;
import technology.rocketjump.mountaincore.assets.model.WallType;
import technology.rocketjump.mountaincore.doors.Doorway;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CorpseBehaviour;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.DoorwayEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.mechanism.MechanismEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantSpeciesType;
import technology.rocketjump.mountaincore.mapping.tile.designation.Designation;
import technology.rocketjump.mountaincore.mapping.tile.floor.FloorOverlap;
import technology.rocketjump.mountaincore.mapping.tile.floor.OverlapLayout;
import technology.rocketjump.mountaincore.mapping.tile.floor.TileFloor;
import technology.rocketjump.mountaincore.mapping.tile.layout.WallConstructionLayout;
import technology.rocketjump.mountaincore.mapping.tile.layout.WallLayout;
import technology.rocketjump.mountaincore.mapping.tile.roof.TileRoof;
import technology.rocketjump.mountaincore.mapping.tile.underground.ChannelLayout;
import technology.rocketjump.mountaincore.mapping.tile.underground.PipeLayout;
import technology.rocketjump.mountaincore.mapping.tile.underground.UnderTile;
import technology.rocketjump.mountaincore.mapping.tile.wall.Wall;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.particles.model.ParticleEffectInstance;
import technology.rocketjump.mountaincore.persistence.EnumParser;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.Persistable;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rendering.camera.GlobalSettings;
import technology.rocketjump.mountaincore.rooms.RoomTile;
import technology.rocketjump.mountaincore.rooms.constructions.Construction;
import technology.rocketjump.mountaincore.rooms.constructions.ConstructionType;
import technology.rocketjump.mountaincore.rooms.constructions.WallConstruction;
import technology.rocketjump.mountaincore.zones.Zone;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * This class represents each tile on the 2D area map
 */
public class MapTile implements Persistable {

	private final long seed;
	private final GridPoint2 tilePosition;
	private int regionId = -1; // -1 for unset

	private final Map<Long, Entity> entities = new ConcurrentHashMap<>(); // Concurrent for access by PathfindingTask
	private final Map<Long, ParticleEffectInstance> particleEffects = new HashMap<>();

	private TileRoof roof;
	private Wall wall = null;
	private Doorway doorway = null;
	private final int tilePercentile;
	//A temporary overlapping floor, like snow covered ground. This is separate for rendering on top of the floor, with effects like transparency
	private TileFloor transitoryFloor = null;
	private final Deque<TileFloor> floors = new ArrayDeque<>();
	private UnderTile underTile;

	private Designation designation = null;
	private RoomTile roomTile = null;
	private Set<Zone> zones = new HashSet<>();
	private Construction construction = null;
	private TileExploration exploration = TileExploration.UNEXPLORED;

	public static final MapTile NULL_TILE = new MapTile(-1L, 0, 0, FloorType.NULL_FLOOR, GameMaterial.NULL_MATERIAL);

	public MapTile(long seed, int tileX, int tileY, FloorType floorType, GameMaterial floorMaterial) {
		this.seed = seed;
		this.tilePercentile = (int) Math.abs(seed % 100);
		this.tilePosition = new GridPoint2(tileX, tileY);
		floors.push(new TileFloor(floorType, floorMaterial));
		this.roof = new TileRoof();

		if (GlobalSettings.MAP_REVEALED) {
			exploration = TileExploration.EXPLORED;
		}
	}

	public void update(TileNeighbours neighbours, MapVertex[] vertexNeighboursOfCell, MessageDispatcher messageDispatcher) {
		if (hasWall()) {
			WallLayout newLayout = new WallLayout(neighbours);
			wall.setTrueLayout(newLayout);
		}
		if (hasChannel()) {
			ChannelLayout newLayout = new ChannelLayout(neighbours);
			getUnderTile().setChannelLayout(newLayout);
		}
		if (hasPipe()) {
			PipeLayout newPipeLayout = new PipeLayout(neighbours);
			MechanismEntityAttributes attributes = (MechanismEntityAttributes) underTile.getPipeEntity().getPhysicalEntityComponent().getAttributes();
			if (!newPipeLayout.equals(attributes.getPipeLayout())) {
				attributes.setPipeLayout(newPipeLayout);
				if (messageDispatcher != null) {
					messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, underTile.getPipeEntity());
				}
			}
		}

		// Always update FloorOverlaps for all tiles
		// ================= Actual Floor Overlaps ================
		for (TileFloor floor : floors) {
			floor.getOverlaps().clear();
			floor.getTransitoryOverlaps().clear();
		}
		if (getTransitoryFloor() != null) {
			getTransitoryFloor().getOverlaps().clear();
			getTransitoryFloor().getTransitoryOverlaps().clear();
		}
		updateFloorOverlaps(neighbours, vertexNeighboursOfCell, MapTile::getActualFloor, MapTile::getActualFloor, TileFloor::getOverlaps);

		// ================== Transitory floor overlaps ===================
		// Keep these separate, so that they can be overlapped after the transitory tiles are rendered
		updateFloorOverlaps(neighbours, vertexNeighboursOfCell, MapTile::getFloor, MapTile::getTransitoryFloor, TileFloor::getTransitoryOverlaps);

		if (hasConstruction()) {
			if (construction.getConstructionType().equals(ConstructionType.WALL_CONSTRUCTION)) {
				WallConstruction wallConstruction = (WallConstruction) construction;
				wallConstruction.setLayout(new WallConstructionLayout(neighbours));
			}
		}
	}

	//TODO Optimize
	private void updateFloorOverlaps(TileNeighbours neighbours, MapVertex[] vertexNeighboursOfCell,
									 Function<MapTile, TileFloor> myFloorFunction, Function<MapTile, TileFloor> neighbourFloorFunction,
									 Function<TileFloor, List<FloorOverlap>> overlapFunction) {
		TileFloor floor = myFloorFunction.apply(this);
		List<FloorOverlap> toReplace = overlapFunction.apply(floor);

		Set<FloorOverlap> overlaps = new TreeSet<>(new FloorType.FloorDefinitionComparator());
		int thisLayer = floor.getFloorType().getLayer();
		if (this.hasWall()) {
			thisLayer = Integer.MIN_VALUE;
		}
		for (MapTile neighbour : neighbours.values()) {
			TileFloor neighbourFloor = neighbourFloorFunction.apply(neighbour);
			if (neighbourFloor != null && neighbour.hasFloor() && neighbourFloor.getFloorType().getLayer() > thisLayer) {
				OverlapLayout layout = OverlapLayout.fromNeighbours(neighbours, neighbourFloor.getFloorType());
				overlaps.add(new FloorOverlap(layout, neighbourFloor.getFloorType(), neighbourFloor.getMaterial(), vertexNeighboursOfCell));
			}
		}
		toReplace.clear();
		toReplace.addAll(overlaps);

		if (floor.getFloorType().isUseMaterialColor()) {
			Color floorMaterialColor = floor.getMaterial().getColor();
			floor.setVertexColors(floorMaterialColor, floorMaterialColor, floorMaterialColor, floorMaterialColor);
		} else {
			floor.setVertexColors(
				floor.getFloorType().getColorForHeightValue(vertexNeighboursOfCell[0].getHeightmapValue()),
				floor.getFloorType().getColorForHeightValue(vertexNeighboursOfCell[1].getHeightmapValue()),
				floor.getFloorType().getColorForHeightValue(vertexNeighboursOfCell[2].getHeightmapValue()),
				floor.getFloorType().getColorForHeightValue(vertexNeighboursOfCell[3].getHeightmapValue())
			);
		}
	}

	public Collection<Entity> getEntities() {
		return entities.values();
	}

	public List<Long> getEntityIds() {
		return new ArrayList<>(entities.keySet());
	}

	public TileRoof getRoof() {
		return roof;
	}

	public void setRoof(TileRoof roof) {
		this.roof = roof;
	}

	public boolean hasWall() {
		return this.wall != null;
	}

	public boolean hasWallConstruction() {
		return getConstruction() != null && getConstruction().getConstructionType() == ConstructionType.WALL_CONSTRUCTION;
	}

	public boolean isNavigable(Entity requestingEntity) {
		return isNavigable(requestingEntity, null);
	}

	public boolean isNavigable(Entity requestingEntity, MapTile startingPoint) {
		if (this.equals(startingPoint)) {
			// Can always navigate if this tile is the starting point
			return true;
		} else if (getFloor().isRiverTile() && !getFloor().isBridgeNavigable()) {
			if (startingPoint != null && startingPoint.getFloor().isRiverTile() && !startingPoint.getFloor().hasBridge()) {
				return true; // Can navigate from a river tile to another river tile
			} else {
				return false; // Otherwise rivers are not navigable
			}
		} else if (hasChannel() && !getFloor().isBridgeNavigable()) {
			if (startingPoint != null && startingPoint.hasChannel()) {
				return true; // Can navigate from a channel tile to another channel tile
			} else {
				return false; // Otherwise channels are not navigable
			}
		} else if (!hasWall() && !hasTree()) {
			if (getFloor().hasBridge() && !getFloor().isBridgeNavigable()) {
				return false;
			}
			if (hasDoorway() && requestingEntity != null && requestingEntity.getPhysicalEntityComponent().getAttributes() instanceof CreatureEntityAttributes creatureAttributes) {
				if (!creatureAttributes.getRace().getBehaviour().getIsSapient()) {
					return false;
				}
			}
			for (Entity entity : getEntities()) {
				if (entity.getType().equals(EntityType.FURNITURE)) {
					if (entity.getPhysicalEntityComponent().getAttributes() instanceof DoorwayEntityAttributes) {
						continue;
					}
					FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
					if (attributes.getFurnitureType().isBlocksMovement()) {
						// This piece of furniture blocks movement but if it is also in the startingPoint, ignore it
						if (startingPoint != null) {
							boolean startingPointHasSameEntity = startingPoint.getEntity(entity.getId()) != null;
							if (startingPointHasSameEntity) {
								continue; // go on to next entity
							} else {
								// Not also in starting point, so this blocks movement
								return false;
							}
						} else {
							return false;
						}
					}
				} else if (entity.getType().equals(EntityType.ITEM)) {
					ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
					if (attributes.getItemType().blocksMovement()) {
						return false;
					}
				}
			}
			return true;
		} else if (startingPoint != null && startingPoint.hasWall() && this.hasWall()) {
			// FIXME Possibly bug-prone hack allowing navigation through walls if starting inside one
			return true;
		} else {
			return false;
		}
	}

	public boolean hasFloor() {
		return this.wall == null;
	}

	public Wall getWall() {
		return wall;
	}

	public void setWall(Wall wall, TileRoof roof) {
		this.wall = wall;
		this.roof = roof;
	}

	public void addWall(TileNeighbours neighbours, GameMaterial material, WallType wallType) {
		this.wall = new Wall(new WallLayout(neighbours), wallType, material);
	}

	public long getSeed() {
		return seed;
	}

	public int getTilePercentile() {
		return tilePercentile;
	}

	public TileFloor getActualFloor() {
		return floors.peek();
	}

	public TileFloor getFloor() {
		if (transitoryFloor != null) {
			return transitoryFloor;
		}

		return getActualFloor();
	}

	public int getTileX() {
		return tilePosition.x;
	}

	public int getTileY() {
		return tilePosition.y;
	}

	/**
	 * This method returns a world position representing the center of a tile
	 */
	public Vector2 getWorldPositionOfCenter() {
		return new Vector2(0.5f + tilePosition.x, 0.5f + tilePosition.y);
	}

	public Entity removeEntity(long entityId) {
		if (hasPipe()) {
			Entity pipeEntity = underTile.getPipeEntity();
			if (pipeEntity.getId() == entityId) {
				underTile.setPipeEntity(null);
				return pipeEntity;
			}
		}
		return entities.remove(entityId);
	}

	public void addEntity(Entity entity) {
		entities.put(entity.getId(), entity);
	}

	public boolean hasPlant() {
		for (Entity entity : entities.values()) {
			if (entity.getType().equals(EntityType.PLANT)) {
				return true;
			}
		}
		return false;
	}

	public Entity getPlant() {
		for (Entity entity : entities.values()) {
			if (entity.getType().equals(EntityType.PLANT)) {
				return entity;
			}
		}
		return null;
	}

	public boolean hasTree() {
		// Currently stuff can go behind trees, to disable this, check for trees in tiles to south as well
		for (Entity entity : entities.values()) {
			if (entity.getType().equals(EntityType.PLANT)) {
				PlantEntityAttributes attributes = (PlantEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				if (attributes.isTree()) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean hasShrub() {
		for (Entity entity : entities.values()) {
			if (entity.getType().equals(EntityType.PLANT)) {
				PlantEntityAttributes attributes = (PlantEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				if (attributes.getSpecies().getPlantType().equals(PlantSpeciesType.SHRUB)) {
					return true;
				}
			}
		}
		return false;
	}

	public Designation getDesignation() {
		return designation;
	}

	public void setDesignation(Designation designation) {
		this.designation = designation;
	}

	public boolean hasRoom() {
		return roomTile != null;
	}

	public RoomTile getRoomTile() {
		return roomTile;
	}

	public void setRoomTile(RoomTile roomTile) {
		this.roomTile = roomTile;
	}

	public GridPoint2 getTilePosition() {
		return tilePosition;
	}

	public boolean hasItem() {
		for (Entity entity : entities.values()) {
			if (entity.getType().equals(EntityType.ITEM)) {
				return true;
			}
		}
		return false;
	}

	public Entity getItemMatching(ItemEntityAttributes attributesToMatch) {
		for (Entity entity : getEntities()) {
			if (entity.getType().equals(EntityType.ITEM)) {
				ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				if (attributes.getItemType().equals(attributesToMatch.getItemType())) {
					for (GameMaterialType gameMaterialType : attributes.getItemType().getMaterialTypes()) {
						if (attributes.getMaterial(gameMaterialType) != null && attributes.getMaterial(gameMaterialType).equals(attributesToMatch.getMaterial(gameMaterialType))) {
							return entity;
						}
					}
				}
			}
		}
		return null;
	}

	/**
	 * Empty of entities such as items and plants
	 */
	public boolean isEmpty() {
		if (!this.isEmptyExceptEntities()) {
			return false;
		}
		for (Entity entity : this.entities.values()) {
			if (entity.getType().equals(EntityType.ITEM) || entity.getType().equals(EntityType.PLANT) || entity.getType().equals(EntityType.FURNITURE)) {
				return false;
			}
			if (entity.getType().equals(EntityType.CREATURE) && entity.getBehaviourComponent() instanceof CorpseBehaviour) {
				return false;
			}
		}
		return true;
	}

	public boolean isEmptyExceptItemsAndPlants() {
		if (!this.isEmptyExceptEntities()) {
			return false;
		}
		for (Entity entity : this.entities.values()) {
			if (entity.getType().equals(EntityType.FURNITURE)) {
				return false;
			}
		}
		return true;
	}

	public boolean isEmptyExceptEntities() {
		return !(this.hasWall() || this.hasDoorway() || this.hasConstruction() || this.getFloor().isRiverTile() || this.hasChannel());
	}

	public Entity getEntity(long entityId) {
		return entities.get(entityId);
	}

	public Entity getFirstItem() {
		for (Entity entity : entities.values()) {
			if (entity.getType().equals(EntityType.ITEM)) {
				return entity;
			}
		}
		return null;
	}

	public Entity getFirstCorpse() {
		for (Entity entity : entities.values()) {
			if (entity.getType().equals(EntityType.CREATURE) && entity.getBehaviourComponent() instanceof CorpseBehaviour) {
				return entity;
			}
		}
		return null;
	}

	public boolean hasDoorway() {
		return doorway != null;
	}

	public boolean hasDoorwayConstruction() {
		return construction != null && construction.getConstructionType() == ConstructionType.DOORWAY_CONSTRUCTION;
	}

	public Doorway getDoorway() {
		return doorway;
	}

	public void setDoorway(Doorway doorway) {
		this.doorway = doorway;
	}

	public boolean hasConstruction() {
		return construction != null;
	}

	public Construction getConstruction() {
		return construction;
	}

	public void setConstruction(Construction construction) {
		this.construction = construction;
	}

	public boolean isWaterSource() {
		return getFloor().getRiverTile() != null;
	}

	public int getRegionId() {
		return regionId;
	}

	public void setRegionId(int regionId) {
		this.regionId = regionId;
	}

	public void addToZone(Zone zone) {
		this.zones.add(zone);
	}

	public void removeFromZone(Zone zone) {
		this.zones.remove(zone);
	}

	public boolean hasMovementBlockingEntity() {
		for (Entity entity : entities.values()) {
			if (entity.getPhysicalEntityComponent().getAttributes() instanceof FurnitureEntityAttributes attributes
					&& attributes.getFurnitureType().isBlocksMovement()) {
				return true;
			} else if (entity.getPhysicalEntityComponent().getAttributes() instanceof  PlantEntityAttributes attributes
					&& attributes.isTree()) {
				return true;
			}
		}
		return false;
	}

	public RegionType getRegionType() {
		if (getFloor().isRiverTile()) {
			return RegionType.RIVER;
		} else if (hasWall()) {
			return RegionType.WALL;
		} else if (hasChannel()) {
			return RegionType.CHANNEL;
		} else if (hasMovementBlockingEntity()) {
			return RegionType.MOVEMENT_BLOCKING_ENTITY;
		} else {
			return RegionType.GENERIC;
		}
	}

	public Set<Zone> getZones() {
		return zones;
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		// Don't need to check if already in state holder?
		JSONObject asJson = new JSONObject(true);

		asJson.put("regionId", regionId);

		if (!entities.isEmpty()) {
			JSONArray entities = new JSONArray();
			entities.addAll(this.entities.keySet());
			asJson.put("entities", entities);
		}

		JSONObject roofJson = new JSONObject(true);
		roof.writeTo(roofJson, savedGameStateHolder);
		asJson.put("roof", roofJson);

		if (transitoryFloor != null) {
			JSONObject floorJson = new JSONObject(true);
			transitoryFloor.writeTo(floorJson, savedGameStateHolder);
			asJson.put("transitoryFloor", floorJson);
		}

		if (!floors.isEmpty()) {
			JSONArray floorsArray = new JSONArray();
			Iterator<TileFloor> descendingIterator = floors.descendingIterator();
			while (descendingIterator.hasNext()) {
				TileFloor floor = descendingIterator.next();
				JSONObject floorJson = new JSONObject(true);
				floor.writeTo(floorJson, savedGameStateHolder);
				floorsArray.add(floorJson);
			}
			asJson.put("floors", floorsArray);
		}

		if (wall != null) {
			JSONObject wallJson = new JSONObject(true);
			wall.writeTo(wallJson, savedGameStateHolder);
			asJson.put("wall", wallJson);
		}

		if (doorway != null) {
			JSONObject doorwayJson = new JSONObject(true);
			doorway.writeTo(doorwayJson, savedGameStateHolder);
			asJson.put("door", doorwayJson);
		}

		if (underTile != null) {
			JSONObject undertileJson = new JSONObject(true);
			underTile.writeTo(undertileJson, savedGameStateHolder);
			asJson.put("underTile", undertileJson);
		}

		if (designation != null) {
			asJson.put("designation", designation.getDesignationName());
		}

		if (roomTile != null) {
			JSONObject roomTileJson = new JSONObject(true);
			roomTile.writeTo(roomTileJson, savedGameStateHolder);
			asJson.put("roomTile", roomTileJson);
		}

		if (!zones.isEmpty()) {
			JSONArray zonesJson = new JSONArray();
			for (Zone zone : zones) {
				zone.writeTo(savedGameStateHolder);
				zonesJson.add(zone.getZoneId());
			}
			asJson.put("zones", zonesJson);
		}

		if (construction != null) {
			construction.writeTo(savedGameStateHolder);
			asJson.put("construction", construction.getId());
		}

		if (!exploration.equals(TileExploration.EXPLORED)) {
			asJson.put("exploration", exploration.name());
		}

		savedGameStateHolder.tiles.put(tilePosition, this);
		savedGameStateHolder.tileJson.add(asJson);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.regionId = asJson.getIntValue("regionId");

		JSONArray entityIds = asJson.getJSONArray("entities");
		if (entityIds != null) {
			for (int cursor = 0; cursor < entityIds.size(); cursor++) {
				this.entities.put(entityIds.getLongValue(cursor), Entity.NULL_ENTITY); // Placing null for now for entities to be added later
			}
		}

		Object roofJson = asJson.get("roof");
		if (roofJson instanceof JSONObject) {
			this.roof.readFrom((JSONObject) roofJson, savedGameStateHolder, relatedStores);
		} else {
			// Old save version
			throw new InvalidSaveException("Map tile roof is old version");
		}

		JSONObject transitoryFloorJson = asJson.getJSONObject("transitoryFloor");
		if (transitoryFloorJson != null) {
			TileFloor floor = new TileFloor();
			floor.readFrom(transitoryFloorJson, savedGameStateHolder, relatedStores);
			transitoryFloor = floor;
		}

		JSONArray floorsJson = asJson.getJSONArray("floors");
		this.floors.clear();
		if (floorsJson != null) {
			for (int cursor = 0; cursor < floorsJson.size(); cursor++) {
				JSONObject floorJson = floorsJson.getJSONObject(cursor);
				TileFloor floor = new TileFloor();
				floor.readFrom(floorJson, savedGameStateHolder, relatedStores);
				this.floors.push(floor);
			}
		}

		JSONObject wallJson = asJson.getJSONObject("wall");
		if (wallJson != null) {
			this.wall = new Wall();
			this.wall.readFrom(wallJson, savedGameStateHolder, relatedStores);
		}

		JSONObject doorJson = asJson.getJSONObject("door");
		if (doorJson != null) {
			this.doorway = new Doorway();
			this.doorway.readFrom(doorJson, savedGameStateHolder, relatedStores);
		}

		JSONObject underTileJson = asJson.getJSONObject("underTile");
		if (underTileJson != null) {
			this.underTile = new UnderTile();
			this.underTile.readFrom(underTileJson, savedGameStateHolder, relatedStores);
		}

		String designationName = asJson.getString("designation");
		if (designationName != null) {
			this.designation = relatedStores.designationDictionary.getByName(designationName);
			if (this.designation == null) {
				throw new InvalidSaveException("Could not find tile designation by name " + designationName);
			}
		}

		JSONObject roomTileJson = asJson.getJSONObject("roomTile");
		if (roomTileJson != null) {
			roomTile = new RoomTile();
			roomTile.readFrom(roomTileJson, savedGameStateHolder, relatedStores);
			roomTile.setTile(this);
		}

		JSONArray zones = asJson.getJSONArray("zones");
		if (zones != null) {
			for (int cursor = 0; cursor < zones.size(); cursor++) {
				long zoneId = zones.getLongValue(cursor);
				Zone zone = savedGameStateHolder.zones.get(zoneId);
				if (zone == null) {
					throw new InvalidSaveException("Could not find zone by ID " + zoneId);
				} else {
					this.zones.add(zone);
				}
			}
		}

		Long constructionId = asJson.getLong("construction");
		if (constructionId != null) {
			this.construction = savedGameStateHolder.constructions.get(constructionId);
			if (this.construction == null) {
				throw new InvalidSaveException("Could not find construction by ID " + constructionId);
			}
		}

		this.exploration = EnumParser.getEnumValue(asJson, "exploration", TileExploration.class, TileExploration.EXPLORED);

		savedGameStateHolder.tiles.put(tilePosition, this);
	}

	public TileExploration getExploration() {
		return exploration;
	}

	public void setExploration(TileExploration exploration) {
		this.exploration = exploration;
	}

	public Map<Long, ParticleEffectInstance> getParticleEffects() {
		return particleEffects;
	}

	public void replaceFloor(TileFloor newFloor) {
		this.floors.push(newFloor);
	}

	public void popFloor() {
		this.floors.pop();
	}

	public TileFloor getTransitoryFloor() {
		return transitoryFloor;
	}

	public void setTransitoryFloor(TileFloor transitoryFloor) {
		this.transitoryFloor = transitoryFloor;
	}

	public void removeTransitoryFloor() {
		this.transitoryFloor = null;
	}

	public float getTransitoryFloorAlpha(double val) {
//		Mike: I'm a math newbie, but this is `y = mx + c`
		float x1 = tilePercentile / 100.0f;
		float x2 = 1.0f;
		float y1 = 0;
		float y2 = 1.0f;

		float m = (y2 - y1) / (x2 - x1);
		float c = y2 - (m * x2);

		return (m * (float) val) + c;
	}

	public UnderTile getUnderTile() {
		return underTile;
	}

	public UnderTile getOrCreateUnderTile() {
		if (underTile == null) {
			underTile = new UnderTile();
		}
		return underTile;
	}

	public void setUnderTile(UnderTile underTile) {
		this.underTile = underTile;
	}

	public boolean hasChannel() {
		return underTile != null && underTile.getChannelLayout() != null;
	}

	public boolean hasPipe() {
		return underTile != null && underTile.getPipeEntity() != null;
	}

	public Deque<TileFloor> getAllFloors() {
		return floors;
	}

	public boolean hasPowerMechanism() {
		return underTile != null && underTile.getPowerMechanismEntity() != null;
	}
	
	public enum RegionType {
		RIVER, WALL, CHANNEL, MOVEMENT_BLOCKING_ENTITY, GENERIC
	}
}
