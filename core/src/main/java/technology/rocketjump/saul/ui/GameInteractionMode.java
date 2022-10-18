package technology.rocketjump.saul.ui;

import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.effect.OngoingEffectAttributes;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesGrowthStage;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.mapping.tile.designation.Designation;
import technology.rocketjump.saul.mapping.tile.designation.DesignationDictionary;
import technology.rocketjump.saul.mapping.tile.underground.PipeConstructionState;
import technology.rocketjump.saul.materials.model.GameMaterialType;
import technology.rocketjump.saul.rooms.RoomType;
import technology.rocketjump.saul.ui.cursor.GameCursor;

import java.util.HashMap;
import java.util.Map;

import static technology.rocketjump.saul.entities.model.EntityType.*;
import static technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesGrowthStage.PlantSpeciesHarvestType.FORAGING;
import static technology.rocketjump.saul.mapping.tile.TileExploration.EXPLORED;
import static technology.rocketjump.saul.mapping.tile.roof.RoofConstructionState.NONE;
import static technology.rocketjump.saul.mapping.tile.roof.TileRoofState.CONSTRUCTED;
import static technology.rocketjump.saul.mapping.tile.roof.TileRoofState.OPEN;

// MODDING extract this enum to data-driven set of behaviours (when we know how to)
public enum GameInteractionMode {

	DEFAULT(null, null, null, false),
	DESIGNATE_MINING(GameCursor.MINING, "MINING", mapTile -> ((!mapTile.getExploration().equals(EXPLORED) && mapTile.getDesignation() == null) || (mapTile.hasWall() && mapTile.getDesignation() == null)), true),
	DESIGNATE_CHOP_WOOD(GameCursor.LOGGING, "CHOP_WOOD", mapTile -> (mapTile.getExploration().equals(EXPLORED) && mapTile.hasTree() && mapTile.getDesignation() == null), true),
	DESIGNATE_DIG_CHANNEL(GameCursor.SPADE, "DIG_CHANNEL", mapTile -> {
		if (!mapTile.getExploration().equals(EXPLORED) || mapTile.getDesignation() != null || mapTile.hasChannel() || isRiverEdge(mapTile)) {
			return false;
		}
		for (Entity entity : mapTile.getEntities()) {
			if (entity.getType().equals(FURNITURE)) {
				return false;
			}
		}
		return mapTile.hasFloor() && mapTile.getFloor().getMaterial().getMaterialType().equals(GameMaterialType.EARTH);
	}, true),
	DESIGNATE_CLEAR_GROUND(GameCursor.SPADE, "CLEAR_GROUND", mapTile -> {
		if (!mapTile.getExploration().equals(EXPLORED) || mapTile.getDesignation() != null) {
			return false;
		}
		for (Entity entity : mapTile.getEntities()) {
			if (entity.getType().equals(ITEM)) {
				ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				if (attributes.getItemType().getStockpileGroup() == null) {
					return true;
				}
			} else if (entity.getType().equals(PLANT)) {
				PlantEntityAttributes attributes = (PlantEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				if (attributes.getSpecies().getPlantType().removalJobTypeName.equals("CLEAR_GROUND")) {
					return true;
				}
			}
		}
		return false;
	}, true),
	DESIGNATE_HARVEST_PLANTS(GameCursor.SICKLE, "HARVEST", mapTile -> {
		Entity plant = mapTile.getPlant();
		if (plant != null && mapTile.getExploration().equals(EXPLORED) && mapTile.getDesignation() == null) {
			PlantEntityAttributes attributes = (PlantEntityAttributes) plant.getPhysicalEntityComponent().getAttributes();
			PlantSpeciesGrowthStage growthStage = attributes.getSpecies().getGrowthStages().get(attributes.getGrowthStageCursor());
			return FORAGING.equals(growthStage.getHarvestType());
		} else {
			return false;
		}
	}, true),
	DESIGNATE_EXTINGUISH_FLAMES(GameCursor.SPLASH, "EXTINGUISH_FLAMES", mapTile -> {
		if (!mapTile.getExploration().equals(EXPLORED) || mapTile.getDesignation() != null) {
			return false;
		}
		for (Entity entity : mapTile.getEntities()) {
			if (STATIC_ENTITY_TYPES.contains(entity.getType()) && entity.isOnFire()) {
				return true;
			} else if (entity.getType().equals(ONGOING_EFFECT)) {
				OngoingEffectAttributes attributes = (OngoingEffectAttributes) entity.getPhysicalEntityComponent().getAttributes();
				if (attributes.getType().isCanBeExtinguished()) {
					return true;
				}
			}
		}
		return false;
	}, true),

	DESIGNATE_ROOFING(GameCursor.ROOFING, null, mapTile -> mapTile.getExploration().equals(EXPLORED) &&
			mapTile.getRoof().getState().equals(OPEN) && mapTile.getRoof().getConstructionState().equals(NONE), true),
	CANCEL_ROOFING(GameCursor.CANCEL, null, mapTile -> mapTile.getExploration().equals(EXPLORED) &&
			mapTile.getRoof().getState().equals(OPEN) && !mapTile.getRoof().getConstructionState().equals(NONE), true),
	DECONSTRUCT_ROOFING(GameCursor.DECONSTRUCT, null, mapTile -> mapTile.getExploration().equals(EXPLORED) &&
			mapTile.getRoof().getState().equals(CONSTRUCTED) && mapTile.getRoof().getConstructionState().equals(NONE), true),

	DESIGNATE_PIPING(GameCursor.SPLASH, null, mapTile ->  mapTile.getExploration().equals(EXPLORED) &&
			!isRiverEdge(mapTile) && !mapTile.getFloor().isRiverTile() && !mapTile.hasPipe(), true),
	CANCEL_PIPING(GameCursor.CANCEL, null, mapTile -> mapTile.getExploration().equals(EXPLORED) &&
			mapTile.getUnderTile() != null && mapTile.getUnderTile().getPipeConstructionState().equals(PipeConstructionState.READY_FOR_CONSTRUCTION), true),
	DECONSTRUCT_PIPING(GameCursor.DECONSTRUCT, null, mapTile -> mapTile.getExploration().equals(EXPLORED) && mapTile.hasPipe(), true),

	DESIGNATE_MECHANISMS(GameCursor.GEARS, null, mapTile -> mapTile.getExploration().equals(EXPLORED) &&
			!mapTile.getFloor().isRiverTile() && !mapTile.hasPowerMechanism() && (mapTile.getUnderTile() == null || mapTile.getUnderTile().getQueuedMechanismType() == null), false),
	CANCEL_MECHANISMS(GameCursor.CANCEL, null, mapTile -> mapTile.getExploration().equals(EXPLORED) &&
			mapTile.getUnderTile() != null && mapTile.getUnderTile().getQueuedMechanismType() != null, true),
	DECONSTRUCT_MECHANISMS(GameCursor.DECONSTRUCT, null, mapTile -> mapTile.getExploration().equals(EXPLORED) && mapTile.hasPowerMechanism(), true),

	REMOVE_DESIGNATIONS(GameCursor.CANCEL, null, mapTile -> mapTile.getDesignation() != null, true),
	PLACE_ROOM(GameCursor.ROOMS, null, mapTile -> mapTile.getExploration().equals(EXPLORED) && !mapTile.hasWall() &&
			!mapTile.hasRoom() && !mapTile.hasDoorway() && !mapTile.isWaterSource() && !mapTile.getFloor().hasBridge(), true),
	PLACE_FURNITURE(GameCursor.ROOMS, null, null, false),
	PLACE_DOOR(GameCursor.DOOR, null, null, false),
	PLACE_WALLS(GameCursor.WALL, null, null, true),
	PLACE_BRIDGE(GameCursor.BRIDGE, null, mapTile -> mapTile.getExploration().equals(EXPLORED) && !mapTile.hasWall() &&
			!mapTile.hasDoorway() && !mapTile.hasRoom() && !mapTile.hasConstruction(), true),
	PLACE_FLOORING(GameCursor.FLOOR, "FLOORING", mapTile -> mapTile.hasFloor() && !mapTile.getFloor().isRiverTile(), true),
	REMOVE_ROOMS(GameCursor.CANCEL, "REMOVE_ROOMS", MapTile::hasRoom, true),
	SET_JOB_PRIORITY(GameCursor.PRIORITY, null, null, true),
	REMOVE_CONSTRUCTIONS(GameCursor.CANCEL, "REMOVE_CONSTRUCTIONS", tile -> tile.hasConstruction() || tile.getDesignation() != null, true),
	DECONSTRUCT(GameCursor.DECONSTRUCT, "DECONSTRUCT", mapTile -> {
		return mapTile.getFloor().hasBridge() || mapTile.hasDoorway() || mapTile.getEntities().stream().anyMatch(e -> e.getType().equals(FURNITURE)) ||
				mapTile.hasChannel() || (mapTile.hasFloor() && mapTile.getFloor().getFloorType().isConstructed()) ||
				(mapTile.hasWall() && mapTile.getWall().getWallType().isConstructed());
	}, true),
	SQUAD_MOVE_TO_LOCATION(GameCursor.ATTACK, null, mapTile ->
			mapTile.getExploration().equals(EXPLORED) && mapTile.isNavigable(null), false),
	SQUAD_ATTACK_CREATURE(GameCursor.ATTACK, null, mapTile -> mapTile.getExploration().equals(EXPLORED), true),
	CANCEL_ATTACK_CREATURE(GameCursor.CANCEL, null, mapTile -> mapTile.getExploration().equals(EXPLORED) &&
			mapTile.getUnderTile() != null && mapTile.getUnderTile().getQueuedMechanismType() != null, true);



	public final GameCursor cursor;

	public final String designationName;
	private Designation designationToApply;
	public final TileDesignationCheck tileDesignationCheck;
	private RoomType roomType;
	private FurnitureType furnitureType;
	public final boolean isDraggable;

	GameInteractionMode(GameCursor cursor, String designationName, TileDesignationCheck tileDesignationCheck, boolean isDraggable) {
		this.cursor = cursor;
		this.designationName = designationName;
		this.tileDesignationCheck = tileDesignationCheck;
		this.isDraggable = isDraggable;
	}

	public static void init(DesignationDictionary designationDictionary) {
		for (GameInteractionMode interactionMode : values()) {
			if (interactionMode.designationName != null) {
				Designation designation = designationDictionary.getByName(interactionMode.designationName);
				if (designation == null) {
					throw new RuntimeException("No designation found by name: " + interactionMode.designationName);
				}
				interactionMode.designationToApply = designation;
			}
		}

		PLACE_ROOM.roomType = new RoomType();
	}

	private static Map<String, GameInteractionMode> byDesignationName = new HashMap<>();

	static {
		for (GameInteractionMode interactionMode : GameInteractionMode.values()) {
			byDesignationName.put(interactionMode.designationName, interactionMode);
		}
	}
	public static GameInteractionMode getByDesignationName(String designationName) {
		return byDesignationName.get(designationName);
	}
	public Designation getDesignationToApply() {
		return designationToApply;
	}

	public boolean isDesignation() {
		return REMOVE_DESIGNATIONS.equals(this) || designationName != null;
	}

	public RoomType getRoomType() {
		return roomType;
	}

	public void setRoomType(RoomType roomType) {
		this.roomType = roomType;
	}

	public void setFurnitureType(FurnitureType furnitureType) {
		this.furnitureType = furnitureType;
	}

	public FurnitureType getFurnitureType() {
		return furnitureType;
	}

	public interface TileDesignationCheck {
		boolean shouldDesignationApply(MapTile mapTile);
	}

	public static boolean isRiverEdge(MapTile mapTile) {
		return mapTile.getAllFloors().stream().anyMatch(f -> f.getFloorType().getFloorTypeName().equals("river-edge-dirt"));
	}
}
