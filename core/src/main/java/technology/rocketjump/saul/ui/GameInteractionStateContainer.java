package technology.rocketjump.saul.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.entities.tags.WorkspaceLocationsRestrictionTag;
import technology.rocketjump.saul.assets.model.FloorType;
import technology.rocketjump.saul.assets.model.WallType;
import technology.rocketjump.saul.audio.model.SoundAsset;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.doors.DoorwayOrientation;
import technology.rocketjump.saul.doors.DoorwaySize;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.saul.entities.model.physical.mechanism.MechanismType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.jobs.model.JobPriority;
import technology.rocketjump.saul.jobs.model.Skill;
import technology.rocketjump.saul.mapping.MapMessageHandler;
import technology.rocketjump.saul.mapping.model.TiledMap;
import technology.rocketjump.saul.mapping.model.WallPlacementMode;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.mapping.tile.TileExploration;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.materials.model.GameMaterialType;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.DoorwayPlacementMessage;
import technology.rocketjump.saul.messaging.types.MaterialSelectionMessage;
import technology.rocketjump.saul.messaging.types.RequestSoundMessage;
import technology.rocketjump.saul.production.StockpileGroup;
import technology.rocketjump.saul.rendering.ScreenWriter;
import technology.rocketjump.saul.rendering.camera.PrimaryCameraWrapper;
import technology.rocketjump.saul.rooms.*;
import technology.rocketjump.saul.rooms.constructions.BridgeConstruction;
import technology.rocketjump.saul.rooms.constructions.WallConstruction;
import technology.rocketjump.saul.settlement.SettlementItemTracker;
import technology.rocketjump.saul.sprites.model.BridgeOrientation;
import technology.rocketjump.saul.sprites.model.BridgeType;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;

import java.util.*;

import static technology.rocketjump.saul.gamecontext.GameState.SELECT_SPAWN_LOCATION;
import static technology.rocketjump.saul.gamecontext.GameState.STARTING_SPAWN;
import static technology.rocketjump.saul.materials.model.GameMaterial.NULL_MATERIAL;
import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.saul.misc.VectorUtils.toVector;
import static technology.rocketjump.saul.rooms.RoomTypeDictionary.VIRTUAL_PLACING_ROOM;
import static technology.rocketjump.saul.sprites.model.BridgeOrientation.EAST_WEST;
import static technology.rocketjump.saul.sprites.model.BridgeOrientation.NORTH_SOUTH;
import static technology.rocketjump.saul.ui.GameInteractionMode.isRiverEdge;

/**
 * This class keeps track of how the player is interacting with the game world - for example an input event not captured
 * by the GUI will use this to figure out if they're currently designating digging or similar
 * <p>
 * Note: This feels like it should be part of GameContext, but for now resetting it on new game start is acceptable
 * <p>
 * Seems to be conflated a bit with GuiMessageHandler and the set of classes could do with some refactoring
 */
@Singleton
public class GameInteractionStateContainer implements GameContextAware {

	private final PrimaryCameraWrapper primaryCameraWrapper;
	private final RoomFactory roomFactory;
	private final ScreenWriter screenWriter;
	private final I18nTranslator i18nTranslator;
	private final SettlementItemTracker settlementItemTracker;
	private final MessageDispatcher messageDispatcher;
	private final SoundAsset dragAreaSoundAsset;
	private GameContext gameContext;

	private boolean dragging;
	private Vector2 startPoint;
	private Vector2 currentPoint;
	private GameInteractionMode interactionMode;
	private GameViewMode gameViewMode = GameViewMode.DEFAULT;

	public Room virtualRoom;

	private JobPriority jobPriorityToApply = JobPriority.NORMAL;

	// Room placement Info
	private RoomType selectedRoomType;
	private StockpileGroup selectedStockpileGroup;

	// Furniture placement info
	private RoomType currentRoomType;
	private FurnitureType furnitureTypeToPlace;
	private Entity furnitureEntityToPlace;
	private boolean validFurniturePlacement;
	private DoorwayPlacementMessage virtualDoorPlacement;

	private Selectable selectable;
	private Skill professionToReplace;

	private MaterialSelectionMessage doorMaterialSelection = new MaterialSelectionMessage(GameMaterialType.STONE, NULL_MATERIAL, null);

	private Set<GridPoint2> virtualRoofConstructions = new HashSet<>();
	// Wall placement info
	private MaterialSelectionMessage wallMaterialSelection = new MaterialSelectionMessage(GameMaterialType.STONE, NULL_MATERIAL, null);
	private WallPlacementMode wallPlacementMode = WallPlacementMode.L_SHAPE;
	private WallType wallTypeToPlace;
	private List<WallConstruction> virtualWallConstructions = new LinkedList<>();
	// Bridge placement info
	private boolean validBridgePlacement;
	private MaterialSelectionMessage bridgeMaterialSelection = new MaterialSelectionMessage(GameMaterialType.STONE, NULL_MATERIAL, null);
	private BridgeType bridgeTypeToPlace;
	private BridgeConstruction virtualBridgeConstruction;
	// Floor placement info
	private MaterialSelectionMessage floorMaterialSelection = new MaterialSelectionMessage(GameMaterialType.STONE, NULL_MATERIAL, null);
	private FloorType floorTypeToPlace;
	// Roof placement info
	private MaterialSelectionMessage roofMaterialSelection = new MaterialSelectionMessage(GameMaterialType.WOOD, NULL_MATERIAL, null);
	// Mechanism placement info
	private MechanismType mechanismTypeToPlace;


	@Inject
	public GameInteractionStateContainer(PrimaryCameraWrapper primaryCameraWrapper, RoomFactory roomFactory, ScreenWriter screenWriter,
										 I18nTranslator i18nTranslator, SettlementItemTracker settlementItemTracker, MessageDispatcher messageDispatcher,
										 SoundAssetDictionary soundAssetDictionary) {
		this.primaryCameraWrapper = primaryCameraWrapper;
		this.roomFactory = roomFactory;
		this.screenWriter = screenWriter;
		this.i18nTranslator = i18nTranslator;
		this.settlementItemTracker = settlementItemTracker;
		this.messageDispatcher = messageDispatcher;
		this.dragAreaSoundAsset = soundAssetDictionary.getByName("DragArea"); // MODDING expose this

		clearContextRelatedState();
	}

	@Override
	public void clearContextRelatedState() {
		// Most of this is probably not necessary
		dragging = false;
		startPoint = new Vector2();
		currentPoint = new Vector2();

		interactionMode = GameInteractionMode.DEFAULT;
		virtualRoom = roomFactory.create(VIRTUAL_PLACING_ROOM);
	}

	public void update() {
		if (gameContext == null || gameContext.getSettlementState().getGameState().equals(SELECT_SPAWN_LOCATION) ||
				gameContext.getSettlementState().getGameState().equals(STARTING_SPAWN)) {
			return;
		}
		TiledMap map = gameContext.getAreaMap();
		if (!virtualRoom.isEmpty()) {
			virtualRoom.clearTiles();
		}
		if (!virtualWallConstructions.isEmpty()) {
			for (WallConstruction virtualWallConstruction : virtualWallConstructions) {
				MapTile tile = map.getTile(virtualWallConstruction.getPrimaryLocation());
				tile.setConstruction(null);
				MapMessageHandler.updateTile(tile, gameContext, messageDispatcher);
			}
			virtualWallConstructions.clear();
		}
		if (!virtualRoofConstructions.isEmpty()) {
			virtualRoofConstructions.clear();
		}
		validBridgePlacement = true;
		virtualBridgeConstruction = null;
		Vector3 worldPosition = primaryCameraWrapper.getCamera().unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
		Vector2 worldPosition2 = new Vector2(worldPosition.x, worldPosition.y);
		GridPoint2 tilePosition = toGridPoint(worldPosition2);
		int minX = (int) Math.floor(Math.min(startPoint.x, currentPoint.x));
		int maxX = (int) Math.floor(Math.max(startPoint.x, currentPoint.x));
		int minY = (int) Math.floor(Math.min(startPoint.y, currentPoint.y));
		int maxY = (int) Math.floor(Math.max(startPoint.y, currentPoint.y));

		int previousDragWidth = screenWriter.getCurrentTileWidth();
		int previousDragHeight = screenWriter.getCurrentTileHeight();
		int currentDragWidth = maxX - minX + 1;
		int currentDragHeight = maxY - minY + 1;
		boolean tileSelected = false;

		screenWriter.setDragging(dragging, currentDragWidth, currentDragHeight);


		if (interactionMode.equals(GameInteractionMode.PLACE_ROOM) && dragging) {
			List<RoomTile> newRoomTiles = new LinkedList<>();
			for (int x = minX; x <= maxX; x++) {
				for (int y = minY; y <= maxY; y++) {
					MapTile tile = map.getTile(x, y);
					if (tile == null) {
						continue;
					}
					if (interactionMode.tileDesignationCheck.shouldDesignationApply(tile)) {
						GameMaterialType requiredFloorMaterialType = interactionMode.getRoomType().getRequiredFloorMaterialType();
						if (requiredFloorMaterialType == null || tile.getFloor().getMaterial().getMaterialType().equals(requiredFloorMaterialType)) {
							GridPoint2 position = new GridPoint2(x, y);
							RoomTile newRoomTile = new RoomTile();
							newRoomTile.setRoom(virtualRoom);
							newRoomTile.setTilePosition(position);
							newRoomTile.setTile(tile);
							tile.setRoomTile(newRoomTile);
							virtualRoom.addTile(newRoomTile);
							newRoomTiles.add(newRoomTile);
							tileSelected = true;
						}
					}
				}
			}

			virtualRoom.updateLayout(map);
		} else if (interactionMode.equals(GameInteractionMode.PLACE_FURNITURE)) {
			if (furnitureEntityToPlace != null) {
				FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) furnitureEntityToPlace.getPhysicalEntityComponent().getAttributes();
				if (attributes.getCurrentLayout().getRotatesTo() == null) {
					screenWriter.printLine(i18nTranslator.getTranslatedString("GUI.NON_ROTATABLE_FURNITURE.HINT").toString());
				} else {
					screenWriter.printLine(i18nTranslator.getTranslatedString("GUI.ROTATE_FURNITURE.HINT").toString());
				}
				GameMaterialType requiredFloorMaterialType = attributes.getFurnitureType().getRequiredFloorMaterialType();
				if (requiredFloorMaterialType != null) {
					I18nText materialTypeHint = i18nTranslator.getTranslatedWordWithReplacements("GUI.FURNITURE_REQUIRES_FLOOR_MATERIALTYPE",
							ImmutableMap.of("materialType", i18nTranslator.getTranslatedString(requiredFloorMaterialType.getI18nKey())));
					screenWriter.printLine(materialTypeHint.toString());
				}

				furnitureEntityToPlace.getLocationComponent().setWorldPosition(toVector(tilePosition), false);

				validFurniturePlacement = isFurniturePlacementValid(map, tilePosition, attributes);
			}

		} else if (interactionMode.equals(GameInteractionMode.DESIGNATE_MECHANISMS)) {
		} else if (interactionMode.equals(GameInteractionMode.PLACE_WALLS)) {
			GameMaterial selectedMaterial = wallMaterialSelection.selectedMaterial;

			if (dragging) {
				Set<GridPoint2> potentialLocations;
				if (WallPlacementMode.ROOM.equals(wallPlacementMode)) {
					potentialLocations = getPotentialWallLocationsForRoomPlacement(startPoint, currentPoint);
					virtualRoofConstructions = getPotentialRoofLocationsForRoomPlacement(startPoint, currentPoint);
				} else if (WallPlacementMode.L_SHAPE.equals(wallPlacementMode)) {
					potentialLocations = getPotentialWallLocationsForLShapePlacement(startPoint, currentPoint);
				} else {
					potentialLocations = getPotentialWallLocationsForLinePlacement(startPoint, currentPoint);
				}


				for (GridPoint2 potentialLocation : potentialLocations) {
					MapTile tile = map.getTile(potentialLocation);
					if (tile == null) {
						continue;
					}
					if (tile.isNavigable(null) && tile.isEmptyExceptEntities()) {
						// Can place virtual wall construction here
						WallConstruction wallConstruction = new WallConstruction(potentialLocation, wallTypeToPlace, selectedMaterial);
						virtualWallConstructions.add(wallConstruction);
						tile.setConstruction(wallConstruction); // do NOT add to construction store, else virtual walls will attempt to be built
						MapMessageHandler.updateTile(tile, gameContext, messageDispatcher);
						tileSelected = true;
					}
				}
			} else { // Not dragging
				MapTile tile = map.getTile(tilePosition);
				if (tile != null && tile.isEmptyExceptEntities()) {
					WallConstruction wallConstruction = new WallConstruction(tilePosition, wallTypeToPlace, selectedMaterial);
					virtualWallConstructions.add(wallConstruction);
					tile.setConstruction(wallConstruction); // do NOT add to construction store, else virtual walls will attempt to be built
					MapMessageHandler.updateTile(tile, gameContext, messageDispatcher);
				}
			}
		} else if (interactionMode.equals(GameInteractionMode.PLACE_BRIDGE)) {
			screenWriter.printLine(i18nTranslator.getTranslatedString("GUI.PLACE_BRIDGE.HINT").toString());
			if (dragging) {
				List<MapTile> bridgeTiles = new LinkedList<>();
				for (int x = minX; x <= maxX; x++) {
					for (int y = minY; y <= maxY; y++) {
						MapTile tile = map.getTile(x, y);
						if (tile == null) {
							continue;
						}

						bridgeTiles.add(tile);
						if (!interactionMode.tileDesignationCheck.shouldDesignationApply(tile)) {
							validBridgePlacement = false;
						}
					}
				}

				BridgeOrientation orientation = EAST_WEST;
				if (maxY - minY > maxX - minX) {
					orientation = NORTH_SOUTH;
				} else if (maxY - minY == maxX - minX) {
					// bridge is square
					boolean channelCrossesEastWestBridge = false;
					for (int y = minY + 1; y <= maxY - 1; y++) {
						MapTile tile = map.getTile(minX, y);
						if (tile != null && tile.hasChannel()) {
							channelCrossesEastWestBridge = true;
							break;
						}
					}
					if (channelCrossesEastWestBridge) {
						orientation = NORTH_SOUTH;
					}
				}

				if (validBridgePlacement) {
					if (maxX - minX < 2 || maxY - minY < 2) {
						validBridgePlacement = false;
					}
				}
				if (validBridgePlacement) {
					if (!(coversBothSidesRiver(bridgeTiles, minX, maxX, minY, maxY, orientation) ||
						crossesChannels(bridgeTiles, minX, maxX, minY, maxY))) {
						validBridgePlacement = false;
					}
				}
				if (validBridgePlacement) {
					if (!canFitRequiredResourcesOnCorrectSide(bridgeTiles, bridgeTypeToPlace)) {
						validBridgePlacement = false;
					}
				}
				if (validBridgePlacement) {
					if (channelIsUnderEndOfBridge(minX, maxX, minY, maxY, orientation)) {
						validBridgePlacement = false;
					}
				}

				GameMaterial bridgeMaterial = bridgeMaterialSelection.selectedMaterial;
				if (bridgeMaterial.equals(NULL_MATERIAL)) {
					bridgeMaterial = GameMaterial.nullMaterialWithType(bridgeMaterialSelection.selectedMaterialType);
				}
				Bridge bridge = new Bridge(bridgeTiles, bridgeMaterial, orientation, bridgeTypeToPlace);
				virtualBridgeConstruction = new BridgeConstruction(bridge);
				tileSelected = true;
			}

		} else if (interactionMode.equals(GameInteractionMode.PLACE_DOOR)) {
			virtualDoorPlacement = isDoorPlacementValid(map, tilePosition);
		} else {
			// Catch-all for other draggable interactions
			if (dragging && interactionMode.tileDesignationCheck != null) {
				for (int x = minX; x <= maxX; x++) {
					for (int y = minY; y <= maxY; y++) {
						MapTile tile = map.getTile(x, y);
						if (tile == null) {
							continue;
						}
						if (interactionMode.tileDesignationCheck.shouldDesignationApply(tile)) {
							tileSelected = true;
						}
					}
				}
			}
		}
		if (tileSelected && (previousDragWidth != currentDragWidth || previousDragHeight != currentDragHeight)) {
			messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(dragAreaSoundAsset));
		}
	}

	private boolean crossesChannels(List<MapTile> bridgeTiles, int minX, int maxX, int minY, int maxY) {
		boolean minCornerNavigable = true;
		boolean maxCornerNavigable = true;
		boolean crossesChannels = false;

		for (MapTile mapTile : bridgeTiles) {
			if (mapTile.getTileX() == minX && mapTile.getTileY() == minY) {
				minCornerNavigable = mapTile.isNavigable(null);
				if (!minCornerNavigable) {
					break;
				}
			} else if (mapTile.getTileX() == maxX && mapTile.getTileY() == maxY) {
				maxCornerNavigable = mapTile.isNavigable(null);
				if (!maxCornerNavigable) {
					break;
				}
			} else if (mapTile.hasChannel()) {
				crossesChannels = true;
			}
		}

		return minCornerNavigable && maxCornerNavigable && crossesChannels;
	}

	private boolean canFitRequiredResourcesOnCorrectSide(List<MapTile> bridgeTiles, BridgeType bridgeTypeToPlace) {
		int resourcesRequired = bridgeTypeToPlace.getBuildingRequirement().getQuantity() * bridgeTiles.size();
		int resourcesPerTile = bridgeTypeToPlace.getBuildingRequirement().getItemType().getMaxStackSize();
		int tilesRequired = (int) Math.ceil((double) resourcesRequired / (double) resourcesPerTile);

		Map<Integer, List<MapTile>> tilesByLandRegion = new HashMap<>();
		for (MapTile bridgeTile : bridgeTiles) {
			if (!bridgeTile.getFloor().isRiverTile()) {
				List<MapTile> tilesForRegion = tilesByLandRegion.computeIfAbsent(bridgeTile.getRegionId(), (a) -> new LinkedList<>());
				tilesForRegion.add(bridgeTile);
			}
		}

		List<Entity> unallocatedItems = settlementItemTracker.getItemsByType(bridgeTypeToPlace.getBuildingRequirement().getItemType(), false);
		for (Entity unallocatedItem : unallocatedItems) {
			MapTile itemTile = gameContext.getAreaMap().getTile(unallocatedItem.getLocationComponent().getWorldOrParentPosition());
			if (itemTile != null) {
				int itemRegion = itemTile.getRegionId();
				List<MapTile> bridgeTilesInRegion = tilesByLandRegion.get(itemRegion);
				if (bridgeTilesInRegion != null && bridgeTilesInRegion.size() >= tilesRequired) {
					return true;
				}
			}
		}
		return false;
	}

	private Set<GridPoint2> getPotentialWallLocationsForRoomPlacement(Vector2 startPoint, Vector2 currentPoint) {
		int minX = (int) Math.floor(Math.min(startPoint.x, currentPoint.x));
		int maxX = (int) Math.floor(Math.max(startPoint.x, currentPoint.x));
		int minY = (int) Math.floor(Math.min(startPoint.y, currentPoint.y));
		int maxY = (int) Math.floor(Math.max(startPoint.y, currentPoint.y));

		Set<GridPoint2> locations = new HashSet<>();
		for (int x = minX; x <= maxX; x++) {
			locations.add(new GridPoint2(x, minY));
			locations.add(new GridPoint2(x, maxY));
		}
		for (int y = minY; y <= maxY; y++) {
			locations.add(new GridPoint2(minX, y));
			locations.add(new GridPoint2(maxX, y));
		}
		return locations;
	}

	private Set<GridPoint2> getPotentialRoofLocationsForRoomPlacement(Vector2 startPoint, Vector2 currentPoint) {
		int minX = (int) Math.floor(Math.min(startPoint.x, currentPoint.x));
		int maxX = (int) Math.floor(Math.max(startPoint.x, currentPoint.x));
		int minY = (int) Math.floor(Math.min(startPoint.y, currentPoint.y));
		int maxY = (int) Math.floor(Math.max(startPoint.y, currentPoint.y));

		Set<GridPoint2> locations = new HashSet<>();
		for (int x = minX + 1; x < maxX; x++) {
			for (int y = minY + 1; y < maxY; y++) {
				locations.add(new GridPoint2(x, y));
			}
		}
		return locations;
	}

	private Set<GridPoint2> getPotentialWallLocationsForLShapePlacement(Vector2 startPoint, Vector2 currentPoint) {
		int minX = (int) Math.floor(Math.min(startPoint.x, currentPoint.x));
		int maxX = (int) Math.floor(Math.max(startPoint.x, currentPoint.x));
		int minY = (int) Math.floor(Math.min(startPoint.y, currentPoint.y));
		int maxY = (int) Math.floor(Math.max(startPoint.y, currentPoint.y));

		float xDiff = Math.abs(startPoint.x - currentPoint.x);
		float yDiff = Math.abs(startPoint.y - currentPoint.y);
		Set<GridPoint2> locations = new HashSet<>();

		if (xDiff > yDiff) {
			// Difference in x is greater
			for (int x = minX; x <= maxX; x++) {
				locations.add(new GridPoint2(x, (int) Math.floor(startPoint.y)));
			}
			for (int y = minY; y <= maxY; y++) {
				locations.add(new GridPoint2((int) Math.floor(currentPoint.x), y));
			}
		} else {
			for (int y = minY; y <= maxY; y++) {
				locations.add(new GridPoint2((int) Math.floor(startPoint.x), y));
			}
			for (int x = minX; x <= maxX; x++) {
				locations.add(new GridPoint2(x, (int) Math.floor(currentPoint.y)));
			}
		}

		return locations;
	}

	private Set<GridPoint2> getPotentialWallLocationsForLinePlacement(Vector2 startPoint, Vector2 currentPoint) {
		int minX = (int) Math.floor(Math.min(startPoint.x, currentPoint.x));
		int maxX = (int) Math.floor(Math.max(startPoint.x, currentPoint.x));
		int minY = (int) Math.floor(Math.min(startPoint.y, currentPoint.y));
		int maxY = (int) Math.floor(Math.max(startPoint.y, currentPoint.y));

		float xDiff = Math.abs(startPoint.x - currentPoint.x);
		float yDiff = Math.abs(startPoint.y - currentPoint.y);
		Set<GridPoint2> locations = new HashSet<>();

		if (xDiff > yDiff) {
			// Difference in x is greater
			for (int x = minX; x <= maxX; x++) {
				locations.add(new GridPoint2(x, (int) Math.floor(startPoint.y)));
			}
		} else {
			for (int y = minY; y <= maxY; y++) {
				locations.add(new GridPoint2((int) Math.floor(startPoint.x), y));
			}
		}

		return locations;
	}

	private DoorwayPlacementMessage isDoorPlacementValid(TiledMap map, GridPoint2 tilePosition) {
		MapTile targetTile = map.getTile(tilePosition);
		if (targetTile == null || !targetTile.isEmptyExceptItemsAndPlants()) {
			return null;
		}

		// Try single door placements
		MapTile north = map.getTile(tilePosition.cpy().add(0, 1));
		MapTile south = map.getTile(tilePosition.cpy().add(0, -1));
		MapTile west = map.getTile(tilePosition.cpy().add(-1, 0));
		MapTile east = map.getTile(tilePosition.cpy().add(1, 0));
		if (north == null || south == null || west == null || east == null) {
			return null;
		}

		if (north.hasWall() && south.hasWall() && east.isEmptyExceptEntities() && west.isEmptyExceptEntities()) {
			return new DoorwayPlacementMessage(DoorwaySize.SINGLE, DoorwayOrientation.NORTH_SOUTH,
					doorMaterialSelection.selectedMaterial, tilePosition);
		}


		if (west.hasWall() && east.hasWall() && north.isEmptyExceptEntities() && south.isEmptyExceptEntities()) {
			return new DoorwayPlacementMessage(DoorwaySize.SINGLE, DoorwayOrientation.EAST_WEST,
					doorMaterialSelection.selectedMaterial, tilePosition);
		}

		// FIXME #75 Try double door placements when double doors implemented

		return new DoorwayPlacementMessage(null, null, doorMaterialSelection.selectedMaterial, tilePosition);
	}

	private boolean coversBothSidesRiver(List<MapTile> bridgeTiles, int minX, int maxX, int minY, int maxY, BridgeOrientation orientation) {
		Set<Integer> floorRegions = new HashSet<>();
		Set<Integer> riverRegions = new HashSet<>();
		for (MapTile bridgeTile : bridgeTiles) {
			if (bridgeTile.getFloor().isRiverTile()) {
				riverRegions.add(bridgeTile.getRegionId());
			} else {
				floorRegions.add(bridgeTile.getRegionId());
			}

			if (orientation.equals(NORTH_SOUTH)) {
				// minY and maxY tiles must be on ground
				if (bridgeTile.getTileY() == minY || bridgeTile.getTileY() == maxY) {
					if (bridgeTile.getFloor().isRiverTile()) {
						return false;
					}
				}
			} else {
				// minX and maxX tiles must be on ground
				if (bridgeTile.getTileX() == minX || bridgeTile.getTileX() == maxX) {
					if (bridgeTile.getFloor().isRiverTile()) {
						return false;
					}
				}
			}
		}

		return riverRegions.size() > 0 && floorRegions.size() > 0;
	}

	public static boolean isFurniturePlacementValid(TiledMap map, GridPoint2 tilePosition, FurnitureEntityAttributes attributes) {

		List<GridPoint2> positionsToCheck = new LinkedList<>();
		positionsToCheck.add(tilePosition);
		for (GridPoint2 extraTileOffset : attributes.getCurrentLayout().getExtraTiles()) {
			positionsToCheck.add(tilePosition.cpy().add(extraTileOffset));
		}
		if (attributes.getFurnitureType().hasTag(WorkspaceLocationsRestrictionTag.class)) {
			for (FurnitureLayout.Workspace workspace : attributes.getCurrentLayout().getWorkspaces()) {
				positionsToCheck.add(tilePosition.cpy().add(workspace.getAccessedFrom()));
			}
		}

		boolean oneTileNotRiverEdge = false;
		for (GridPoint2 positionToCheck : positionsToCheck) {
			MapTile tileToCheck = map.getTile(positionToCheck);
			if (tileToCheck == null || !tileToCheck.isEmptyExceptItemsAndPlants() || !tileToCheck.getExploration().equals(TileExploration.EXPLORED)) {
				return false;
			}
			if (attributes.getFurnitureType().getRequiredFloorMaterialType() != null &&
					!tileToCheck.getAllFloors().getLast().getMaterial().getMaterialType().equals(attributes.getFurnitureType().getRequiredFloorMaterialType())) {
				return false;
			}
			if (!attributes.getFurnitureType().isPlaceAnywhere()) {
				// If not place anywhere, check that every tile is part of a valid room type
				RoomTile roomTile = tileToCheck.getRoomTile();
				if (roomTile == null || !attributes.getFurnitureType().getValidRoomTypes().contains(roomTile.getRoom().getRoomType())) {
					return false;
				}
			}

			if (!isRiverEdge(tileToCheck)) {
				oneTileNotRiverEdge = true;
			}
		}

		if (!oneTileNotRiverEdge && !attributes.getFurnitureType().getName().equals("WATERWHEEL")) {
			return false;
		}

		if (attributes.getCurrentLayout().getWorkspaces().size() > 0) {
			// Also check one workspace is accessible
			boolean oneWorkspaceAccessible = false;
			for (FurnitureLayout.Workspace workspace : attributes.getCurrentLayout().getWorkspaces()) {
				GridPoint2 workspaceAccessedFrom = tilePosition.cpy().add(workspace.getAccessedFrom());
				MapTile tile = map.getTile(workspaceAccessedFrom);
				if (tile != null && tile.isNavigable(null)) {
					oneWorkspaceAccessible = true;
					break;
				}
			}

			if (!oneWorkspaceAccessible) {
				return false;
			}
		}

		for (FurnitureLayout.SpecialTile specialTile : attributes.getCurrentLayout().getSpecialTiles()) {
			MapTile tileToCheck = map.getTile(tilePosition.cpy().add(specialTile.getLocation()));
			if (tileToCheck == null) {
				return false;
			}

			switch (specialTile.getRequirement()) {
				case IS_RIVER:
					if (!tileToCheck.getFloor().isRiverTile() || tileToCheck.getFloor().hasBridge()) {
						return false;
					}
					break;
				default:
					Logger.warn("Not yet implemented, check for furniture special location of type " + specialTile.getRequirement());
					return false;
			}
		}

		return true;
	}

	public Vector2 getMinPoint() {
		Vector2 minPoint = new Vector2();
		minPoint.x = Math.min(startPoint.x, currentPoint.x);
		minPoint.y = Math.min(startPoint.y, currentPoint.y);
		return minPoint;
	}

	public Vector2 getMaxPoint() {
		Vector2 maxPoint = new Vector2();
		maxPoint.x = Math.max(startPoint.x, currentPoint.x);
		maxPoint.y = Math.max(startPoint.y, currentPoint.y);
		return maxPoint;
	}

	public boolean isDragging() {
		return dragging;
	}

	public void setDragging(boolean dragging) {
		this.dragging = dragging;
	}

	public Vector2 getStartPoint() {
		return startPoint;
	}

	public void setStartPoint(Vector2 startPoint) {
		this.startPoint = startPoint;
	}

	public Vector2 getCurrentPoint() {
		return currentPoint;
	}

	public void setCurrentPoint(Vector2 currentPoint) {
		this.currentPoint = currentPoint;
	}

	public GameInteractionMode getInteractionMode() {
		return interactionMode;
	}

	public void setInteractionMode(GameInteractionMode interactionMode) {
		this.interactionMode = interactionMode;
	}

	public GameViewMode getGameViewMode() {
		return gameViewMode;
	}

	public void setGameViewMode(GameViewMode gameViewMode) {
		this.gameViewMode = gameViewMode;
	}

	public JobPriority getJobPriorityToApply() {
		return jobPriorityToApply;
	}

	public void setJobPriorityToApply(JobPriority jobPriorityToApply) {
		this.jobPriorityToApply = jobPriorityToApply;
	}

	public Entity getFurnitureEntityToPlace() {
		return furnitureEntityToPlace;
	}

	public void setFurnitureEntityToPlace(Entity furnitureEntityToPlace) {
		this.furnitureEntityToPlace = furnitureEntityToPlace;
	}

	public boolean isValidFurniturePlacement() {
		return validFurniturePlacement;
	}

	public boolean isValidDoorPlacement() {
		return virtualDoorPlacement != null && virtualDoorPlacement.getDoorwaySize() != null;
	}

	public boolean isValidBridgePlacement() {
		return validBridgePlacement;
	}

	public DoorwayPlacementMessage getVirtualDoorPlacement() {
		return virtualDoorPlacement;
	}

	public RoomType getCurrentRoomType() {
		return currentRoomType;
	}

	public void setCurrentRoomType(RoomType currentRoomType) {
		this.currentRoomType = currentRoomType;
	}

	public List<WallConstruction> getVirtualWallConstructions() {
		return virtualWallConstructions;
	}

	public void setWallMaterialSelection(MaterialSelectionMessage wallMaterialSelection) {
		this.wallMaterialSelection = wallMaterialSelection;
	}

	public void setWallPlacementMode(WallPlacementMode wallPlacementMode) {
		this.wallPlacementMode = wallPlacementMode;
	}

	public void setWallTypeToPlace(WallType wallTypeToPlace) {
		this.wallTypeToPlace = wallTypeToPlace;
	}

	public void setFloorTypeToPlace(FloorType floorTypeToPlace) {
		this.floorTypeToPlace = floorTypeToPlace;
	}

	public FloorType getFloorTypeToPlace() {
		return floorTypeToPlace;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	public Selectable getSelectable() {
		return selectable;
	}

	public void setSelectable(Selectable selectable) {
		this.selectable = selectable;
	}

	public WallType getWallTypeToPlace() {
		return wallTypeToPlace;
	}

	public void setDoorMaterialSelection(MaterialSelectionMessage materialSelectionMessage) {
		this.doorMaterialSelection = materialSelectionMessage;
	}

	public MaterialSelectionMessage getDoorMaterialSelection() {
		return doorMaterialSelection;
	}

	public StockpileGroup getSelectedStockpileGroup() {
		return selectedStockpileGroup;
	}

	public void setSelectedStockpileGroup(StockpileGroup selectedStockpileGroup) {
		this.selectedStockpileGroup = selectedStockpileGroup;
	}

	public MaterialSelectionMessage getBridgeMaterialSelection() {
		return bridgeMaterialSelection;
	}

	public void setBridgeMaterialSelection(MaterialSelectionMessage bridgeMaterialSelection) {
		this.bridgeMaterialSelection = bridgeMaterialSelection;
	}

	public BridgeType getBridgeTypeToPlace() {
		return bridgeTypeToPlace;
	}

	public void setBridgeTypeToPlace(BridgeType bridgeTypeToPlace) {
		this.bridgeTypeToPlace = bridgeTypeToPlace;
	}

	public BridgeConstruction getVirtualBridgeConstruction() {
		return virtualBridgeConstruction;
	}

	public void setVirtualBridgeConstruction(BridgeConstruction virtualBridgeConstruction) {
		this.virtualBridgeConstruction = virtualBridgeConstruction;
	}

	public Skill getProfessionToReplace() {
		return professionToReplace;
	}

	public void setProfessionToReplace(Skill professionToReplace) {
		this.professionToReplace = professionToReplace;
	}

	public Set<GridPoint2> getVirtualRoofConstructions() {
		return virtualRoofConstructions;
	}

	public void setFloorMaterialSelection(MaterialSelectionMessage wallMaterialSelection) {
		this.floorMaterialSelection = wallMaterialSelection;
	}

	public MaterialSelectionMessage getFloorMaterialSelection() {
		return floorMaterialSelection;
	}

	public MaterialSelectionMessage getRoofMaterialSelection() {
		return roofMaterialSelection;
	}

	public void setRoofMaterialSelection(MaterialSelectionMessage roofMaterialSelection) {
		this.roofMaterialSelection = roofMaterialSelection;
	}

	private boolean channelIsUnderEndOfBridge(int minX, int maxX, int minY, int maxY, BridgeOrientation orientation) {
		if (orientation.equals(NORTH_SOUTH)) {
			for (Integer y : List.of(minY, maxY)) {
				for (int x = minX; x <= maxX; x++) {
					MapTile tile = gameContext.getAreaMap().getTile(x, y);
					if (tile == null || tile.hasChannel()) {
						return true;
					}
				}
			}
		} else {
			for (Integer x : List.of(minX, maxX)) {
				for (int y = minY; y <= maxY; y++) {
					MapTile tile = gameContext.getAreaMap().getTile(x, y);
					if (tile == null || tile.hasChannel()) {
						return true;
					}
				}
			}

		}
		return false;
	}

	public MechanismType getMechanismTypeToPlace() {
		return mechanismTypeToPlace;
	}

	public void setMechanismTypeToPlace(MechanismType mechanismTypeToPlace) {
		this.mechanismTypeToPlace = mechanismTypeToPlace;
	}

	public void setSelectedRoomType(RoomType selectedRoomType) {
		this.selectedRoomType = selectedRoomType;
	}

	public RoomType getSelectedRoomType() {
		return selectedRoomType;
	}

	public MaterialSelectionMessage getWallMaterialSelection() {
		return wallMaterialSelection;
	}

	public FurnitureType getFurnitureTypeToPlace() {
		return furnitureTypeToPlace;
	}

	public void setFurnitureTypeToPlace(FurnitureType furnitureTypeToPlace) {
		this.furnitureTypeToPlace = furnitureTypeToPlace;
	}
}
