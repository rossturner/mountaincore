package technology.rocketjump.mountaincore.ui;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.assets.FloorTypeDictionary;
import technology.rocketjump.mountaincore.assets.WallTypeDictionary;
import technology.rocketjump.mountaincore.assets.model.FloorType;
import technology.rocketjump.mountaincore.assets.model.WallType;
import technology.rocketjump.mountaincore.entities.components.creature.MilitaryComponent;
import technology.rocketjump.mountaincore.entities.factories.FurnitureEntityAttributesFactory;
import technology.rocketjump.mountaincore.entities.factories.FurnitureEntityFactory;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeWithMaterial;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.GameContextAware;
import technology.rocketjump.mountaincore.gamecontext.GameState;
import technology.rocketjump.mountaincore.jobs.model.JobPriority;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.mapping.tile.designation.DesignationDictionary;
import technology.rocketjump.mountaincore.mapping.tile.roof.RoofConstructionState;
import technology.rocketjump.mountaincore.mapping.tile.roof.TileRoofState;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.*;
import technology.rocketjump.mountaincore.military.model.Squad;
import technology.rocketjump.mountaincore.military.model.SquadOrderType;
import technology.rocketjump.mountaincore.misc.VectorUtils;
import technology.rocketjump.mountaincore.production.StockpileGroup;
import technology.rocketjump.mountaincore.rooms.Bridge;
import technology.rocketjump.mountaincore.rooms.Room;
import technology.rocketjump.mountaincore.rooms.RoomTile;
import technology.rocketjump.mountaincore.rooms.constructions.Construction;
import technology.rocketjump.mountaincore.sprites.BridgeTypeDictionary;
import technology.rocketjump.mountaincore.ui.views.GuiViewName;
import technology.rocketjump.mountaincore.ui.views.RoomEditingView;
import technology.rocketjump.mountaincore.ui.widgets.furniture.FurnitureRequirementsWidget;

import java.util.*;

import static technology.rocketjump.mountaincore.mapping.tile.TileExploration.EXPLORED;

@Singleton
public class GuiMessageHandler implements Telegraph, GameContextAware {

	private static final float ENTITY_SELECTION_RADIUS = 0.25f; // Small distance as this is added to radius
	private final MessageDispatcher messageDispatcher;
	private final GameInteractionStateContainer interactionStateContainer;

	private final FurnitureEntityAttributesFactory furnitureEntityAttributesFactory;
	private final FurnitureEntityFactory furnitureEntityFactory;
	private final FurnitureRequirementsWidget furnitureRequirementsWidget;
	private final RoomEditingView roomEditingView;
	private final BridgeTypeDictionary bridgeTypeDictionary;
	private GameContext gameContext;

	private Map<GameMaterialType, WallType> wallTypeMapping = new HashMap<>();
	private Map<GameMaterialType, FloorType> floorTypeMapping = new HashMap<>();

	@Inject
	public GuiMessageHandler(MessageDispatcher messageDispatcher, GameInteractionStateContainer interactionStateContainer,
							 DesignationDictionary designationDictionary, WallTypeDictionary wallTypeDictionary,
							 FurnitureEntityAttributesFactory furnitureEntityAttributesFactory, FurnitureEntityFactory furnitureEntityFactory,
							 BridgeTypeDictionary bridgeTypeDictionary,
							 FloorTypeDictionary floorTypeDictionary, FurnitureRequirementsWidget furnitureRequirementsWidget, RoomEditingView roomEditingView) {
		this.messageDispatcher = messageDispatcher;
		this.interactionStateContainer = interactionStateContainer;
		this.furnitureEntityAttributesFactory = furnitureEntityAttributesFactory;
		this.furnitureEntityFactory = furnitureEntityFactory;
		this.bridgeTypeDictionary = bridgeTypeDictionary;
		this.furnitureRequirementsWidget = furnitureRequirementsWidget;
		this.roomEditingView = roomEditingView;

		messageDispatcher.addListener(this, MessageType.MOUSE_DOWN);
		messageDispatcher.addListener(this, MessageType.MOUSE_UP);
		messageDispatcher.addListener(this, MessageType.MOUSE_MOVED);
		messageDispatcher.addListener(this, MessageType.CAMERA_MOVED);
		messageDispatcher.addListener(this, MessageType.FURNITURE_MATERIAL_SELECTED);
		messageDispatcher.addListener(this, MessageType.GUI_FURNITURE_TYPE_SELECTED);
		messageDispatcher.addListener(this, MessageType.ROTATE_FURNITURE);
		messageDispatcher.addListener(this, MessageType.DESTROY_ENTITY_AND_ALL_INVENTORY);
		messageDispatcher.addListener(this, MessageType.DESTROY_ENTITY);
		messageDispatcher.addListener(this, MessageType.WALL_MATERIAL_SELECTED);
		messageDispatcher.addListener(this, MessageType.FLOOR_MATERIAL_SELECTED);
		messageDispatcher.addListener(this, MessageType.DOOR_MATERIAL_SELECTED);
		messageDispatcher.addListener(this, MessageType.BRIDGE_MATERIAL_SELECTED);
		messageDispatcher.addListener(this, MessageType.CONSTRUCTION_REMOVED);
		messageDispatcher.addListener(this, MessageType.CONSTRUCTION_COMPLETED);
		messageDispatcher.addListener(this, MessageType.REMOVE_ROOM);
		messageDispatcher.addListener(this, MessageType.DECONSTRUCT_BRIDGE);
		messageDispatcher.addListener(this, MessageType.CHOOSE_SELECTABLE);
		messageDispatcher.addListener(this, MessageType.REPLACE_JOB_PRIORITY);
		messageDispatcher.addListener(this, MessageType.GUI_STOCKPILE_GROUP_SELECTED);
		messageDispatcher.addListener(this, MessageType.CANCEL_SCREEN_OR_GO_TO_MAIN_MENU);
		messageDispatcher.addListener(this, MessageType.BEGIN_SPAWN_SETTLEMENT);
		messageDispatcher.addListener(this, MessageType.ROOF_MATERIAL_SELECTED);
		// FIXME Should these really live here?
		for (WallType wallType : wallTypeDictionary.getAllDefinitions()) {
			if (wallType.isConstructed()) {
				wallTypeMapping.put(wallType.getMaterialType(), wallType);
			}
		}
		for (FloorType floorType : floorTypeDictionary.getAllDefinitions()) {
			if (floorType.isConstructed()) {
				floorTypeMapping.put(floorType.getMaterialType(), floorType);
			}
		}


		designationDictionary.init();
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.MOUSE_DOWN: {
				MouseChangeMessage mouseChangeMessage = (MouseChangeMessage) msg.extraInfo;
				// If state is designating something
				if (mouseChangeMessage.getButtonType().equals(MouseChangeMessage.MouseButtonType.PRIMARY_BUTTON)) {
					interactionStateContainer.setStartPoint(mouseChangeMessage.getWorldPosition());
					interactionStateContainer.setCurrentPoint(mouseChangeMessage.getWorldPosition());
					if (interactionStateContainer.getInteractionMode().isDraggable) {
						interactionStateContainer.setDragging(true);
					}
				}
				return true;
			}
			case MessageType.MOUSE_UP: {
				MouseChangeMessage mouseChangeMessage = (MouseChangeMessage) msg.extraInfo;
				if (mouseChangeMessage.getButtonType().equals(MouseChangeMessage.MouseButtonType.PRIMARY_BUTTON)) {
					primaryButtonClicked(mouseChangeMessage);
				} else if (mouseChangeMessage.getButtonType().equals(MouseChangeMessage.MouseButtonType.CANCEL_BUTTON)) {
					cancelButtonClicked(false);
				} else {
					messageDispatcher.dispatchMessage(MessageType.DEBUG_MESSAGE, new DebugMessage(mouseChangeMessage.getWorldPosition()));
				}
				return true;
			}
			case MessageType.CANCEL_SCREEN_OR_GO_TO_MAIN_MENU: {
				cancelButtonClicked(true);
				return true;
			}
			case MessageType.MOUSE_MOVED: {
				MouseChangeMessage mouseChangeMessage = (MouseChangeMessage) msg.extraInfo;
				if (interactionStateContainer.isDragging()) {
					interactionStateContainer.setCurrentPoint(mouseChangeMessage.getWorldPosition());
				}
				return true;
			}
			case MessageType.CAMERA_MOVED: {
				if (interactionStateContainer.isDragging()) {
					CameraMovedMessage cameraMovedMessage = (CameraMovedMessage) msg.extraInfo;
					interactionStateContainer.setCurrentPoint(cameraMovedMessage.cursorWorldPosition);
				}
				return true;
			}
			case MessageType.FURNITURE_MATERIAL_SELECTED:
			case MessageType.GUI_FURNITURE_TYPE_SELECTED: {
				rebuildFurnitureEntity();
				return true;
			}
			case MessageType.ROTATE_FURNITURE: {
				if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.PLACE_FURNITURE)) {
					Entity furnitureEntity = interactionStateContainer.getFurnitureEntityToPlace();
					if (furnitureEntity != null) {
						FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) furnitureEntity.getPhysicalEntityComponent().getAttributes();
						if (attributes.getCurrentLayout().getRotatesTo() != null) {
							attributes.setCurrentLayout(attributes.getCurrentLayout().getRotatesTo());
							messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, furnitureEntity);
						}
					}
				}
				return true;
			}
			case MessageType.DESTROY_ENTITY_AND_ALL_INVENTORY: // fall through
			case MessageType.DESTROY_ENTITY: { // Need to stop showing destroyed entities
				if (interactionStateContainer.getSelectable() != null && interactionStateContainer.getSelectable().type.equals(Selectable.SelectableType.ENTITY)) {
					Entity entity = (Entity) msg.extraInfo;
					if (entity.getId() == interactionStateContainer.getSelectable().getId()) {
						clearSelectable();
					}
				}
				return false;
			}
			case MessageType.DECONSTRUCT_BRIDGE: { // Need to stop showing destroyed entities
				if (interactionStateContainer.getSelectable() != null && interactionStateContainer.getSelectable().type.equals(Selectable.SelectableType.BRIDGE)) {
					Bridge removedBridge = (Bridge) msg.extraInfo;
					if (removedBridge.getBridgeId() == interactionStateContainer.getSelectable().getId()) {
						clearSelectable();
					}
				}
				return false;
			}
			case MessageType.GUI_STOCKPILE_GROUP_SELECTED: {
				StockpileGroup stockpileGroup = (StockpileGroup) msg.extraInfo;
				interactionStateContainer.setSelectedStockpileGroup(stockpileGroup);
				return true;
			}
			case MessageType.DOOR_MATERIAL_SELECTED: {
				MaterialSelectionMessage materialSelectionMessage = (MaterialSelectionMessage) msg.extraInfo;
				interactionStateContainer.setDoorMaterialSelection(materialSelectionMessage);
				return true;
			}
			case MessageType.WALL_MATERIAL_SELECTED: {
				MaterialSelectionMessage materialSelectionMessage = (MaterialSelectionMessage) msg.extraInfo;
				interactionStateContainer.setWallMaterialSelection(materialSelectionMessage);
				interactionStateContainer.setWallTypeToPlace(wallTypeMapping.get(materialSelectionMessage.selectedMaterialType));
				return true;
			}
			case MessageType.ROOF_MATERIAL_SELECTED: {
				MaterialSelectionMessage materialSelectionMessage = (MaterialSelectionMessage) msg.extraInfo;
				interactionStateContainer.setRoofMaterialSelection(materialSelectionMessage);
				return true;
			}
			case MessageType.FLOOR_MATERIAL_SELECTED: {
				MaterialSelectionMessage materialSelectionMessage = (MaterialSelectionMessage) msg.extraInfo;
				interactionStateContainer.setFloorMaterialSelection(materialSelectionMessage);
				interactionStateContainer.setFloorTypeToPlace(floorTypeMapping.get(materialSelectionMessage.selectedMaterialType));
				return true;
			}
			case MessageType.BRIDGE_MATERIAL_SELECTED: {
				MaterialSelectionMessage materialSelectionMessage = (MaterialSelectionMessage) msg.extraInfo;
				interactionStateContainer.setBridgeMaterialSelection(materialSelectionMessage);
				interactionStateContainer.setBridgeTypeToPlace(bridgeTypeDictionary.getByMaterialType(materialSelectionMessage.selectedMaterialType));
				return true;
			}
			case MessageType.CONSTRUCTION_REMOVED:
			case MessageType.CONSTRUCTION_COMPLETED:
				Construction construction = (Construction) msg.extraInfo;
				if (interactionStateContainer.getSelectable() != null && interactionStateContainer.getSelectable().type.equals(Selectable.SelectableType.CONSTRUCTION)
						&& interactionStateContainer.getSelectable().getConstruction().equals(construction)) {
					clearSelectable();
				}
				return true;
			case MessageType.REMOVE_ROOM: {
				Room removedRoom = (Room) msg.extraInfo;
				if (interactionStateContainer.getSelectable() != null && interactionStateContainer.getSelectable().type.equals(Selectable.SelectableType.ROOM)
						&& interactionStateContainer.getSelectable().getRoom().equals(removedRoom)) {
					clearSelectable();
				}
				return false;
			}
			case MessageType.CHOOSE_SELECTABLE: {
				Selectable selectable = (Selectable) msg.extraInfo;
				chooseSelectable(selectable);
				return true;
			}
			case MessageType.REPLACE_JOB_PRIORITY: {
				JobPriority jobPriority = (JobPriority)msg.extraInfo;
				interactionStateContainer.setJobPriorityToApply(jobPriority);
				return true;
			}
			case MessageType.BEGIN_SPAWN_SETTLEMENT: {
				interactionStateContainer.virtualRoom.clearTiles();
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private void rebuildFurnitureEntity() {
		FurnitureType selectedFurnitureType = interactionStateContainer.getFurnitureTypeToPlace();

		GameMaterialType selectedMaterialType = furnitureRequirementsWidget.getSelectedMaterialType();
		List<ItemTypeWithMaterial> materialSelections = furnitureRequirementsWidget.getSelections();
		if (selectedFurnitureType == null) {
			return;
		}

		GameMaterial primaryMaterial = materialSelections.stream().map(ItemTypeWithMaterial::getMaterial)
				.filter(material -> selectedMaterialType.equals(material.getMaterialType()))
				.findAny().orElse(GameMaterial.nullMaterialWithType(selectedMaterialType));

		FurnitureEntityAttributes attributes = furnitureEntityAttributesFactory.byType(selectedFurnitureType, primaryMaterial);
		Entity furnitureEntity = furnitureEntityFactory.create(attributes, new GridPoint2(), null, gameContext);
		furnitureEntity.getLocationComponent().init(furnitureEntity, null, gameContext); // Remove messageDispatcher so position updates are not sent
		interactionStateContainer.setFurnitureEntityToPlace(furnitureEntity);
	}

	private void primaryButtonClicked(MouseChangeMessage mouseChangeMessage) {
		if (gameContext != null && gameContext.getSettlementState().getGameState().equals(GameState.SELECT_SPAWN_LOCATION)) {
			setPotentialEmbarkLocation(mouseChangeMessage);
		} else if (gameContext != null && gameContext.getSettlementState().getGameState().equals(GameState.STARTING_SPAWN)) {
			// Do nothing
		} else if (interactionStateContainer.isDragging()) {
			interactionStateContainer.setDragging(false);

			if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.PLACE_ROOM)) {
				RoomPlacementMessage roomPlacementMessage = new RoomPlacementMessage(interactionStateContainer.virtualRoom.getRoomTiles(),
						interactionStateContainer.getInteractionMode().getRoomType(), interactionStateContainer.getSelectedStockpileGroup());
				messageDispatcher.dispatchMessage(MessageType.ROOM_PLACEMENT, roomPlacementMessage);
			} else if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.PLACE_WALLS)) {
				WallsPlacementMessage message = new WallsPlacementMessage(new LinkedList<>(interactionStateContainer.getVirtualWallConstructions()));
				messageDispatcher.dispatchMessage(MessageType.WALL_PLACEMENT, message);
				interactionStateContainer.getVirtualWallConstructions().clear();

				for (GridPoint2 location : interactionStateContainer.getVirtualRoofConstructions()) {
					MapTile tile = gameContext.getAreaMap().getTile(location);
					if (tile != null && tile.getRoof().getState().equals(TileRoofState.OPEN) && tile.getRoof().getConstructionState().equals(RoofConstructionState.NONE)) {
						messageDispatcher.dispatchMessage(MessageType.ROOF_CONSTRUCTION_QUEUE_CHANGE,
								new TileConstructionQueueMessage(tile, true));
					}
				}

			} else if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.DESIGNATE_POWER_LINES)) {
				interactionStateContainer.getVirtualPowerMechanismPlacements().forEach(placement -> {
					messageDispatcher.dispatchMessage(MessageType.MECHANISM_CONSTRUCTION_ADDED, new MechanismPlacementMessage(
							gameContext.getAreaMap().getTile(placement.location), placement.mechanismType
					));
				});
				interactionStateContainer.getVirtualPowerMechanismPlacements().clear();
			} else if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.PLACE_BRIDGE)) {
				if (interactionStateContainer.isValidBridgePlacement() && interactionStateContainer.getVirtualBridgeConstruction() != null) {
					messageDispatcher.dispatchMessage(MessageType.BRIDGE_PLACEMENT, interactionStateContainer.getVirtualBridgeConstruction().getBridge());
				}
			} else {
				AreaSelectionMessage areaSelectionMessage = new AreaSelectionMessage(interactionStateContainer.getMinPoint(), interactionStateContainer.getMaxPoint());
				messageDispatcher.dispatchMessage(AreaSelectionMessage.MESSAGE_TYPE, areaSelectionMessage);
			}

		} else {
			// Not dragging
			if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.DEFAULT) && gameContext != null) {
				defaultWorldClick(mouseChangeMessage);
			} else if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.PLACE_FURNITURE)) {
				if (interactionStateContainer.isValidFurniturePlacement()) {
					messageDispatcher.dispatchMessage(MessageType.FURNITURE_PLACEMENT, interactionStateContainer.getFurnitureEntityToPlace());
				}
			} else if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.PLACE_DOOR)) {
				if (interactionStateContainer.isValidDoorPlacement()) {
					messageDispatcher.dispatchMessage(MessageType.DOOR_PLACEMENT, interactionStateContainer.getVirtualDoorPlacement());
				}
			} else if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.SQUAD_MOVE_TO_LOCATION)) {
				MapTile cursorTile = gameContext.getAreaMap().getTile(mouseChangeMessage.getWorldPosition());
				if (cursorTile != null && interactionStateContainer.getInteractionMode().tileDesignationCheck.shouldDesignationApply(cursorTile)) {
					Squad squad = interactionStateContainer.getSelectable().getSquad();
					if (squad == null) {
						Logger.error("Clicked " + interactionStateContainer.getInteractionMode().name() + " but no squad selected");
					} else {
						squad.setGuardingLocation(cursorTile.getTilePosition());
						messageDispatcher.dispatchMessage(MessageType.MILITARY_SQUAD_ORDERS_CHANGED, new SquadOrderChangeMessage(squad, SquadOrderType.GUARDING));
						// Cancel out of this interaction mode
						messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DEFAULT);
					}
				}
			}

		}
	}

	private void setPotentialEmbarkLocation(MouseChangeMessage mouseChangeMessage) {
		interactionStateContainer.virtualRoom.clearTiles();
		gameContext.getAreaMap().setEmbarkPoint(null);

		Vector2 worldClickPosition = mouseChangeMessage.getWorldPosition();
		MapTile centreTile = gameContext.getAreaMap().getTile(worldClickPosition);
		if (centreTile != null) {

			for (int x = centreTile.getTileX() - 1; x <= centreTile.getTileX() + 1; x++) {
				for (int y = centreTile.getTileY() - 1; y <= centreTile.getTileY() + 1; y++) {
					MapTile tile = gameContext.getAreaMap().getTile(x, y);
					if (tile == null) {
						continue;
					}
					if (GameInteractionMode.PLACE_ROOM.tileDesignationCheck.shouldDesignationApply(tile)) {
						GridPoint2 position = new GridPoint2(x, y);
						RoomTile newRoomTile = new RoomTile();
						newRoomTile.setRoom(interactionStateContainer.virtualRoom);
						newRoomTile.setTilePosition(position);
						newRoomTile.setTile(tile);
						tile.setRoomTile(newRoomTile);
						interactionStateContainer.virtualRoom.addTile(newRoomTile);
					}
				}
			}

			interactionStateContainer.virtualRoom.updateLayout(gameContext.getAreaMap());
			if (interactionStateContainer.virtualRoom.getRoomTiles().size() == 9) {
				gameContext.getAreaMap().setEmbarkPoint(VectorUtils.toGridPoint(worldClickPosition));
			}
		}

	}

	private void defaultWorldClick(MouseChangeMessage mouseChangeMessage) {
		List<Selectable> selectables = new ArrayList<>();

		// See if an entity has been clicked on
		Vector2 worldClickPosition = mouseChangeMessage.getWorldPosition();
		for (MapTile nearbyTile : gameContext.getAreaMap().getNearestTiles(worldClickPosition)) {
			if (nearbyTile.getExploration().equals(EXPLORED)) {
				for (Entity entity : nearbyTile.getEntities()) {
					float distanceToEntity = entity.getLocationComponent().getWorldPosition().dst2(worldClickPosition);
					if (distanceToEntity < ENTITY_SELECTION_RADIUS) {
						selectables.add(new Selectable(entity, distanceToEntity));
					}
				}
			}
		}

		MapTile clickedTile = gameContext.getAreaMap().getTile(worldClickPosition);
		if (clickedTile != null) {
			if (clickedTile.getExploration().equals(EXPLORED)) {
				// Adding all entities in clicked tile to cover multi-tile entities like furniture
				for (Entity entity : clickedTile.getEntities()) {
					float distanceToEntity = entity.getLocationComponent().getWorldPosition().dst2(worldClickPosition);
					Selectable selectableEntity = new Selectable(entity, distanceToEntity);
					if (!selectables.contains(selectableEntity)) {
						selectables.add(selectableEntity);
					}
					MilitaryComponent militaryComponent = entity.getComponent(MilitaryComponent.class);
					if (militaryComponent != null && militaryComponent.isInMilitary() && militaryComponent.getSquadId() != null) {
						Squad entitySquad = gameContext.getSquads().get(militaryComponent.getSquadId());
						if (entitySquad != null) {
							Selectable selectableSquad = new Selectable(entitySquad);
							if (!selectables.contains(selectableSquad)) {
								selectables.add(selectableSquad);
							}
						}
					}
				}

				if (clickedTile.hasConstruction()) {
					selectables.add(new Selectable(clickedTile.getConstruction()));
				}

				if (clickedTile.hasDoorway()) {
					selectables.add(new Selectable(clickedTile.getDoorway()));
				}

				if (clickedTile.hasRoom()) {
					selectables.add(new Selectable(clickedTile.getRoomTile().getRoom()));
				}

				if (clickedTile.getFloor().hasBridge()) {
					selectables.add(new Selectable(clickedTile.getFloor().getBridge()));
				}
			}
			selectables.add(new Selectable(clickedTile));
		}

		if (!selectables.isEmpty()) {
			Collections.sort(selectables);

			Selectable selected = null;
			if (interactionStateContainer.getSelectable() == null) {
				// Nothing yet selected
				selected = selectables.get(0);
			} else {
				for (int cursor = 0; cursor < selectables.size(); cursor++) {
					Selectable nextToTry = selectables.get(cursor);
					if (nextToTry.equals(interactionStateContainer.getSelectable())) {
						// This is the one already selected
						if (cursor + 1 < selectables.size()) {
							selected = selectables.get(cursor + 1);
						} else {
							selected = selectables.get(0);
						}
						break;
					}
				}
				if (selected == null) {
					selected = selectables.get(0);
				}
			}
			messageDispatcher.dispatchMessage(MessageType.CHOOSE_SELECTABLE, selected);
		}
	}

	private void chooseSelectable(Selectable selected) {
		interactionStateContainer.setSelectable(selected);
		messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, getGuiViewName(selected.type));
	}

	private GuiViewName getGuiViewName(Selectable.SelectableType type) {
		return switch (type) {
			case ENTITY -> GuiViewName.ENTITY_SELECTED;
			case DOORWAY -> GuiViewName.DOORWAY_SELECTED;
			case CONSTRUCTION -> GuiViewName.CONSTRUCTION_SELECTED;
			case TILE -> GuiViewName.TILE_SELECTED;
			case ROOM -> GuiViewName.ROOM_EDITING;
			case BRIDGE -> GuiViewName.BRIDGE_SELECTED;
			case SQUAD -> GuiViewName.SQUAD_SELECTED;
		};
	}

	private void cancelButtonClicked(boolean goToMainMenu) {
		if (interactionStateContainer.isDragging()) {
			interactionStateContainer.setDragging(false);
		} else {
			if (!interactionStateContainer.getInteractionMode().equals(GameInteractionMode.DEFAULT)) {
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DEFAULT);
			} else {
				// In default interaction mode
				// Try going back a menu level
				if (goToMainMenu) {
					messageDispatcher.dispatchMessage(MessageType.GUI_CANCEL_CURRENT_VIEW_OR_GO_TO_MAIN_MENU);
				} else {
					messageDispatcher.dispatchMessage(MessageType.GUI_CANCEL_CURRENT_VIEW);
				}
				interactionStateContainer.setSelectable(null);
			}
		}
	}

	@Override
	public void clearContextRelatedState() {
		clearSelectable();
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	private void clearSelectable() {
		interactionStateContainer.setSelectable(null);
		messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.DEFAULT_MENU);
		messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DEFAULT);
	}
}
