package technology.rocketjump.mountaincore.ui;

import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.effect.OngoingEffectAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.mapping.tile.designation.Designation;
import technology.rocketjump.mountaincore.mapping.tile.designation.DesignationDictionary;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.rooms.RoomType;
import technology.rocketjump.mountaincore.ui.cursor.GameCursor;

import java.util.HashMap;
import java.util.Map;

import static technology.rocketjump.mountaincore.entities.model.EntityType.*;
import static technology.rocketjump.mountaincore.mapping.tile.TileExploration.EXPLORED;
import static technology.rocketjump.mountaincore.mapping.tile.roof.RoofConstructionState.NONE;
import static technology.rocketjump.mountaincore.mapping.tile.roof.TileRoofState.OPEN;

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
		return mapTile.hasFloor() && mapTile.getActualFloor().getMaterial().getMaterialType().equals(GameMaterialType.EARTH);
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

	CANCEL(GameCursor.CANCEL, null, mapTile -> mapTile.getExploration().equals(EXPLORED), true),
	CANCEL_ATTACK_CREATURE(GameCursor.CANCEL, null, mapTile -> mapTile.getExploration().equals(EXPLORED) &&
			mapTile.getUnderTile() != null && mapTile.getUnderTile().getQueuedMechanismType() != null, true),

	DECONSTRUCT(GameCursor.DECONSTRUCT, null, mapTile -> mapTile.getExploration().equals(EXPLORED), true),

	DESIGNATE_ROOFING(GameCursor.ROOFING, null, mapTile -> mapTile.getExploration().equals(EXPLORED) &&
			mapTile.getRoof().getState().equals(OPEN) && mapTile.getRoof().getConstructionState().equals(NONE), true),
	DESIGNATE_PIPING(GameCursor.SPLASH, null, mapTile ->  mapTile.getExploration().equals(EXPLORED) &&
			!isRiverEdge(mapTile) && !mapTile.getFloor().isRiverTile() && !mapTile.hasPipe(), true),
	DESIGNATE_POWER_LINES(GameCursor.GEARS, null, mapTile -> mapTile.getExploration().equals(EXPLORED) &&
			!mapTile.getFloor().isRiverTile(), true),

	PLACE_ROOM(GameCursor.ROOMS, null, mapTile -> mapTile.getExploration().equals(EXPLORED) && !mapTile.hasWall() &&
			!mapTile.hasRoom() && !mapTile.hasDoorway() && !mapTile.isWaterSource() && !mapTile.getFloor().hasBridge() && !mapTile.hasChannel(), true),
	PLACE_FURNITURE(GameCursor.ROOMS, null, null, false),
	PLACE_DOOR(GameCursor.DOOR, null, null, false),
	PLACE_WALLS(GameCursor.WALL, null, null, true),
	PLACE_BRIDGE(GameCursor.BRIDGE, null, mapTile -> mapTile.getExploration().equals(EXPLORED) && !mapTile.hasWall() &&
			!mapTile.hasDoorway() && !mapTile.hasRoom() && !mapTile.hasConstruction(), true),
	PLACE_FLOORING(GameCursor.FLOOR, "FLOORING", mapTile -> mapTile.hasFloor() && !mapTile.getFloor().isRiverTile(), true),
	REMOVE_ROOMS(GameCursor.CANCEL, "REMOVE_ROOMS", MapTile::hasRoom, true),
	SET_JOB_PRIORITY(GameCursor.PRIORITY, null, null, true),
	SQUAD_MOVE_TO_LOCATION(GameCursor.ATTACK, null, mapTile ->
			mapTile.getExploration().equals(EXPLORED) && mapTile.isNavigable(null), false),
	SQUAD_ATTACK_CREATURE(GameCursor.ATTACK, null, mapTile -> mapTile.getExploration().equals(EXPLORED), true);


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
