package technology.rocketjump.mountaincore.mapping;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.IntMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.assets.FloorTypeDictionary;
import technology.rocketjump.mountaincore.assets.model.FloorType;
import technology.rocketjump.mountaincore.assets.model.WallType;
import technology.rocketjump.mountaincore.audio.model.SoundAsset;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.entities.behaviour.DoNothingBehaviour;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.Prioritisable;
import technology.rocketjump.mountaincore.entities.components.Faction;
import technology.rocketjump.mountaincore.entities.components.FactionComponent;
import technology.rocketjump.mountaincore.entities.factories.MechanismEntityAttributesFactory;
import technology.rocketjump.mountaincore.entities.factories.MechanismEntityFactory;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Consciousness;
import technology.rocketjump.mountaincore.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.entities.model.physical.mechanism.MechanismEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.mechanism.MechanismType;
import technology.rocketjump.mountaincore.entities.model.physical.mechanism.MechanismTypeDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.GameContextAware;
import technology.rocketjump.mountaincore.jobs.JobStore;
import technology.rocketjump.mountaincore.jobs.model.Job;
import technology.rocketjump.mountaincore.jobs.model.JobPriority;
import technology.rocketjump.mountaincore.jobs.model.JobTarget;
import technology.rocketjump.mountaincore.mapping.model.TiledMap;
import technology.rocketjump.mountaincore.mapping.tile.*;
import technology.rocketjump.mountaincore.mapping.tile.designation.Designation;
import technology.rocketjump.mountaincore.mapping.tile.designation.DesignationDictionary;
import technology.rocketjump.mountaincore.mapping.tile.floor.TileFloor;
import technology.rocketjump.mountaincore.mapping.tile.layout.WallLayout;
import technology.rocketjump.mountaincore.mapping.tile.roof.TileRoof;
import technology.rocketjump.mountaincore.mapping.tile.roof.TileRoofState;
import technology.rocketjump.mountaincore.mapping.tile.underground.ChannelLayout;
import technology.rocketjump.mountaincore.mapping.tile.underground.PipeConstructionState;
import technology.rocketjump.mountaincore.mapping.tile.underground.UnderTile;
import technology.rocketjump.mountaincore.mapping.tile.wall.Wall;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.*;
import technology.rocketjump.mountaincore.military.model.Squad;
import technology.rocketjump.mountaincore.military.model.SquadOrderType;
import technology.rocketjump.mountaincore.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.mountaincore.particles.model.ParticleEffectType;
import technology.rocketjump.mountaincore.production.StockpileComponentUpdater;
import technology.rocketjump.mountaincore.rooms.Room;
import technology.rocketjump.mountaincore.rooms.RoomFactory;
import technology.rocketjump.mountaincore.rooms.RoomStore;
import technology.rocketjump.mountaincore.rooms.RoomTile;
import technology.rocketjump.mountaincore.rooms.components.StockpileRoomComponent;
import technology.rocketjump.mountaincore.ui.GameInteractionMode;
import technology.rocketjump.mountaincore.ui.GameInteractionStateContainer;
import technology.rocketjump.mountaincore.ui.Selectable;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;
import technology.rocketjump.mountaincore.zones.Zone;

import java.util.*;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toSet;
import static technology.rocketjump.mountaincore.entities.model.EntityType.FURNITURE;
import static technology.rocketjump.mountaincore.mapping.tile.TileExploration.EXPLORED;
import static technology.rocketjump.mountaincore.mapping.tile.roof.RoofConstructionState.NONE;
import static technology.rocketjump.mountaincore.mapping.tile.roof.TileRoofState.CONSTRUCTED;
import static technology.rocketjump.mountaincore.mapping.tile.roof.TileRoofState.OPEN;

@Singleton
public class MapMessageHandler implements Telegraph, GameContextAware {

	private final MessageDispatcher messageDispatcher;
	private final OutdoorLightProcessor outdoorLightProcessor;
	private final GameInteractionStateContainer interactionStateContainer;
	private final RoomFactory roomFactory;
	private final RoomStore roomStore;
	private final JobStore jobStore;
	private final StockpileComponentUpdater stockpileComponentUpdater;
	private final RoofConstructionManager roofConstructionManager;
	private final MechanismTypeDictionary mechanismTypeDictionary;
	private final MechanismEntityAttributesFactory mechanismEntityAttributesFactory;
	private final MechanismEntityFactory mechanismEntityFactory;
	private final MechanismType pipeMechanismType;
	private final I18nTranslator i18nTranslator;
	private final Designation deconstructDesignation;

	private GameContext gameContext;

	private ParticleEffectType wallRemovedParticleEffectType;
	private SoundAsset wallRemovedSoundAsset;
	private Map<ItemType, FloorType> floorTypesByInputRequirement = new HashMap<>();

	@Inject
	public MapMessageHandler(MessageDispatcher messageDispatcher, OutdoorLightProcessor outdoorLightProcessor,
							 GameInteractionStateContainer interactionStateContainer, RoomFactory roomFactory,
							 RoomStore roomStore, JobStore jobStore, StockpileComponentUpdater stockpileComponentUpdater,
							 RoofConstructionManager roofConstructionManager, ParticleEffectTypeDictionary particleEffectTypeDictionary,
							 SoundAssetDictionary soundAssetDictionary, FloorTypeDictionary floorTypeDictionary,
							 MechanismTypeDictionary mechanismTypeDictionary, MechanismEntityAttributesFactory mechanismEntityAttributesFactory,
							 MechanismEntityFactory mechanismEntityFactory, I18nTranslator i18nTranslator,
							 DesignationDictionary designationDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.outdoorLightProcessor = outdoorLightProcessor;
		this.interactionStateContainer = interactionStateContainer;
		this.roomFactory = roomFactory;
		this.roomStore = roomStore;
		this.jobStore = jobStore;
		this.stockpileComponentUpdater = stockpileComponentUpdater;
		this.roofConstructionManager = roofConstructionManager;

		this.wallRemovedParticleEffectType = particleEffectTypeDictionary.getByName("Dust cloud"); // MODDING expose this
		this.wallRemovedSoundAsset = soundAssetDictionary.getByName("Mining Drop");
		this.mechanismTypeDictionary = mechanismTypeDictionary;
		this.mechanismEntityAttributesFactory = mechanismEntityAttributesFactory;
		this.mechanismEntityFactory = mechanismEntityFactory;
		this.pipeMechanismType = mechanismTypeDictionary.getByName("Pipe");
		this.i18nTranslator = i18nTranslator;
		this.deconstructDesignation = designationDictionary.getByName("DECONSTRUCT");

		for (FloorType floorType : floorTypeDictionary.getAllDefinitions()) {
			if (floorType.isConstructed()) {
				floorTypesByInputRequirement.put(floorType.getRequirements().get(floorType.getMaterialType()).get(0).getItemType(), floorType);
			}
		}

		messageDispatcher.addListener(this, MessageType.ENTITY_POSITION_CHANGED);
		messageDispatcher.addListener(this, MessageType.AREA_SELECTION);
		messageDispatcher.addListener(this, MessageType.ROOM_PLACEMENT);
		messageDispatcher.addListener(this, MessageType.ADD_WALL);
		messageDispatcher.addListener(this, MessageType.REMOVE_WALL);
		messageDispatcher.addListener(this, MessageType.REMOVE_ROOM);
		messageDispatcher.addListener(this, MessageType.REMOVE_ROOM_TILES);
		messageDispatcher.addListener(this, MessageType.REPLACE_FLOOR);
		messageDispatcher.addListener(this, MessageType.SET_TRANSITORY_FLOOR);
		messageDispatcher.addListener(this, MessageType.UNDO_REPLACE_FLOOR);
		messageDispatcher.addListener(this, MessageType.REMOVE_TRANSITORY_FLOOR);
		messageDispatcher.addListener(this, MessageType.REPLACE_REGION);
		messageDispatcher.addListener(this, MessageType.FLOORING_CONSTRUCTED);
		messageDispatcher.addListener(this, MessageType.ADD_CHANNEL);
		messageDispatcher.addListener(this, MessageType.REMOVE_CHANNEL);
		messageDispatcher.addListener(this, MessageType.ADD_PIPE);
		messageDispatcher.addListener(this, MessageType.REMOVE_PIPE);
		messageDispatcher.addListener(this, MessageType.ENTITY_CREATED_AND_REGISTERED);
		messageDispatcher.addListener(this, MessageType.ENTITY_DESTROYED_AND_UNREGISTERED);
		messageDispatcher.addListener(this, MessageType.UPDATE_REGIONS);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.AREA_SELECTION: {
				return handle((AreaSelectionMessage) msg.extraInfo);
			}
			case MessageType.ROOM_PLACEMENT: {
				return handle((RoomPlacementMessage) msg.extraInfo);
			}
			case MessageType.ENTITY_POSITION_CHANGED: {
				return handle((EntityPositionChangedMessage) msg.extraInfo);
			}
			case MessageType.ADD_WALL: {
				AddWallMessage message = (AddWallMessage) msg.extraInfo;
				return addWall(message.location, message.material, message.wallType);
			}
			case MessageType.REMOVE_WALL: {
				GridPoint2 location = (GridPoint2) msg.extraInfo;
				return handleRemoveWall(location);
			}
			case MessageType.ADD_CHANNEL: {
				GridPoint2 location = (GridPoint2) msg.extraInfo;
				return handleAddChannel(location);
			}
			case MessageType.REMOVE_CHANNEL: {
				GridPoint2 location = (GridPoint2) msg.extraInfo;
				return handleRemoveChannel(location);
			}
			case MessageType.ADD_PIPE: {
				PipeConstructionMessage message = (PipeConstructionMessage) msg.extraInfo;
				return handleAddPipe(message);
			}
			case MessageType.REMOVE_PIPE: {
				GridPoint2 location = (GridPoint2) msg.extraInfo;
				return handleRemovePipe(location);
			}
			case MessageType.ENTITY_CREATED_AND_REGISTERED, MessageType.ENTITY_DESTROYED_AND_UNREGISTERED: {
				Entity entity = (Entity) msg.extraInfo;
				if (entity != null) {
					updateRegions(entity);
				}
				return true;
			}
			case MessageType.REMOVE_ROOM: {
				Room roomToRemove = (Room) msg.extraInfo;
				this.removeRoomTiles(new HashSet<>(roomToRemove.getRoomTiles().keySet()));
				return true;
			}
			case MessageType.REMOVE_ROOM_TILES: {
				Set<GridPoint2> roomTilesToRemove = (Set) msg.extraInfo;
				this.removeRoomTiles(roomTilesToRemove);
				return true;
			}
			case MessageType.REPLACE_FLOOR: {
				ReplaceFloorMessage message = (ReplaceFloorMessage) msg.extraInfo;
				this.replaceFloor(message.targetLocation, message.newFloorType, message.newMaterial);
				return true;
			}
			case MessageType.SET_TRANSITORY_FLOOR: {
				ReplaceFloorMessage message = (ReplaceFloorMessage) msg.extraInfo;
				this.setTransitoryFloor(message.targetLocation, message.newFloorType, message.newMaterial);
				return true;
			}
			case MessageType.UNDO_REPLACE_FLOOR: {
				GridPoint2 location = (GridPoint2) msg.extraInfo;
				this.undoReplaceFloor(location);
				return true;
			}
			case MessageType.REMOVE_TRANSITORY_FLOOR: {
				GridPoint2 location = (GridPoint2) msg.extraInfo;
				this.removeTransitoryFloor(location);
				return true;
			}
			case MessageType.REPLACE_REGION: {
				ReplaceRegionMessage message = (ReplaceRegionMessage) msg.extraInfo;
				replaceRegion(message.tileToReplace, message.replacementRegionId);
				return true;
			}
			case MessageType.UPDATE_REGIONS: {
				Set<MapTile> tiles = (Set<MapTile>) msg.extraInfo;
				updateMultipleRegions(tiles);
				return true;
			}
			case MessageType.FLOORING_CONSTRUCTED: {
				FloorConstructionMessage message = (FloorConstructionMessage) msg.extraInfo;
				FloorType floorType = floorTypesByInputRequirement.get(message.constructionItem);
				if (floorType != null) {
					replaceFloor(message.location, floorType, message.constructionMaterial);
				} else {
					Logger.error("Could not look up floor type constructed by " + message.constructionItem.getItemTypeName());
				}
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private boolean handleAddPipe(PipeConstructionMessage message) {
		MapTile tile = gameContext.getAreaMap().getTile(message.tilePosition);
		if (tile != null) {
			UnderTile underTile = tile.getOrCreateUnderTile();
			if (underTile.getPipeEntity() == null) {
				MechanismEntityAttributes attributes = mechanismEntityAttributesFactory.byType(pipeMechanismType, message.material);
				Entity pipeEntity = mechanismEntityFactory.create(attributes, message.tilePosition, new DoNothingBehaviour(), gameContext);
				underTile.setPipeEntity(pipeEntity);
				updateTile(tile, gameContext, messageDispatcher);
				messageDispatcher.dispatchMessage(MessageType.PIPE_ADDED, message.tilePosition);
			}
		}
		return true;
	}

	private boolean handleRemovePipe(GridPoint2 location) {
		MapTile tile = gameContext.getAreaMap().getTile(location);
		if (tile != null && tile.hasPipe()) {
			messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, tile.getUnderTile().getPipeEntity());
			if (!tile.getUnderTile().liquidCanFlow()) {
				tile.getUnderTile().setLiquidFlow(null);
			}
			updateTile(tile, gameContext, messageDispatcher);
		}
		return true;
	}

	private void updateRegions(Entity entity) {
		TiledMap areaMap = gameContext.getAreaMap();
		Vector2 worldPosition = entity.getLocationComponent().getWorldPosition();
		MapTile tile = areaMap.getTile(worldPosition);
		if (tile == null) {
			return;
		}

		final Set<MapTile> tiles;
		PhysicalEntityComponent physicalEntityComponent = entity.getPhysicalEntityComponent();
		if (EntityType.FURNITURE == entity.getType() &&
				physicalEntityComponent.getAttributes() instanceof FurnitureEntityAttributes attributes) {
			List<GridPoint2> gridPoints = absoluteExtraTiles(tile.getTilePosition(), attributes.getCurrentLayout().getExtraTiles());
			tiles = gridPoints.stream().map(areaMap::getTile).filter(Objects::nonNull).collect(toSet());
		} else if (EntityType.PLANT == entity.getType() &&
				physicalEntityComponent.getAttributes() instanceof PlantEntityAttributes attributes &&
				attributes.isTree()) {
			tiles = Set.of(tile);
		} else {
			tiles = Collections.emptySet();
		}

		updateMultipleRegions(tiles);
	}

	private void updateMultipleRegions(Set<MapTile> tiles) {
		if (!tiles.isEmpty()) {
			// The set of tiles can potentially be in different regions e.g. a piece of furniture destroyed as part of a mining collapse
			Map<MapTile.RegionType, Set<MapTile>> byRegionType = new HashMap<>();
			for (MapTile mapTile : tiles) {
				MapTile.RegionType regionType = mapTile.getRegionType();
				byRegionType.computeIfAbsent(regionType, a -> new HashSet<>()).add(mapTile);
			}

			byRegionType.values().forEach(this::updateRegions);
		}
	}

	//todo: this is duplicated in few places
	private List<GridPoint2> absoluteExtraTiles(GridPoint2 position, List<GridPoint2> relativePositions) {
		List<GridPoint2> positions = new ArrayList<>();
		positions.add(position);

		for (GridPoint2 relative : relativePositions) {
			positions.add(position.cpy().add(relative));
		}
		return positions;
	}

	private boolean handle(RoomPlacementMessage roomPlacementMessage) {
		// Need to create separate rooms if across different areas
		Map<GridPoint2, RoomTile> roomTilesToPlace = roomPlacementMessage.getRoomTiles();

		List<Room> newRooms = new LinkedList<>();
		Room roomToSelect = null;

		while (!roomTilesToPlace.isEmpty()) {
			Room newRoom = roomFactory.create(roomPlacementMessage.getRoomType(), roomTilesToPlace);
			StockpileRoomComponent stockpileRoomComponent = newRoom.getComponent(StockpileRoomComponent.class);
			if (stockpileRoomComponent != null && roomPlacementMessage.stockpileGroup != null) {
				stockpileComponentUpdater.toggleGroup(stockpileRoomComponent.getStockpileSettings(), roomPlacementMessage.stockpileGroup, true, true);
				roomFactory.updateRoomNameForStockpileGroup(newRoom, roomPlacementMessage.stockpileGroup);
				stockpileRoomComponent.updateColor();
			}
			newRooms.add(newRoom);
			roomToSelect = newRoom;
		}

		for (Room newRoom : newRooms) {
			long thisRoomId = newRoom.getRoomId();
			Set<Long> roomsToMergeTo = new HashSet<>();
			for (Map.Entry<GridPoint2, RoomTile> entry : newRoom.entrySet()) {
				if (entry.getValue().isAtRoomEdge()) {
					for (MapTile neighbourTile : gameContext.getAreaMap().getOrthogonalNeighbours(entry.getKey().x, entry.getKey().y).values()) {
						if (neighbourTile.hasRoom()) {
							Room neighbourRoom = neighbourTile.getRoomTile().getRoom();
							if (neighbourRoom.getRoomId() != thisRoomId && neighbourRoom.getRoomType().equals(newRoom.getRoomType())) {
								// Different room of same type
								roomsToMergeTo.add(neighbourRoom.getRoomId());
							}
						}
					}
				}
			}

			if (!roomsToMergeTo.isEmpty()) {

				StockpileRoomComponent stockpileRoomComponent = newRoom.getComponent(StockpileRoomComponent.class);
				if (stockpileRoomComponent != null) {
					// new room has a stockpile component and we are merging into another room, so we remove all settings of the new room
					stockpileRoomComponent.getStockpileSettings().clearAll();
				}

				while (!roomsToMergeTo.isEmpty()) {
					long roomId = roomsToMergeTo.iterator().next();
					roomsToMergeTo.remove(roomId);
					Room roomToMergeTo = roomStore.getById(roomId);

					roomToMergeTo.mergeFrom(newRoom);
					roomStore.remove(newRoom);
					newRoom = roomToMergeTo;
					roomToSelect = newRoom;
				}

				// Update all tile layouts
				newRoom.updateLayout(gameContext.getAreaMap());
			}
		}

		if (roomToSelect != null) {
			Selectable selected = new Selectable(roomToSelect);
			messageDispatcher.dispatchMessage(MessageType.CHOOSE_SELECTABLE, selected);
		}

		return true;
	}

	private boolean handle(AreaSelectionMessage areaSelectionMessage) {
		GridPoint2 minTile = new GridPoint2(MathUtils.floor(areaSelectionMessage.getMinPoint().x), MathUtils.floor(areaSelectionMessage.getMinPoint().y));
		GridPoint2 maxTile = new GridPoint2(MathUtils.floor(areaSelectionMessage.getMaxPoint().x), MathUtils.floor(areaSelectionMessage.getMaxPoint().y));

		Set<GridPoint2> roomTilesToRemove = new HashSet<>();

		if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.SQUAD_ATTACK_CREATURE) ||
			interactionStateContainer.getInteractionMode().equals(GameInteractionMode.CANCEL_ATTACK_CREATURE)) {
			handleAttackOrCancelCreaturesInSelection(areaSelectionMessage);
			return true;
		}

		for (int x = minTile.x; x <= maxTile.x; x++) {
			for (int y = minTile.y; y <= maxTile.y; y++) {
				MapTile tile = gameContext.getAreaMap().getTile(x, y);
				if (tile != null) {

					if (interactionStateContainer.getInteractionMode().tileDesignationCheck != null &&
							interactionStateContainer.getInteractionMode().getDesignationToApply() != null) {
						if (interactionStateContainer.getInteractionMode().tileDesignationCheck.shouldDesignationApply(tile)) {
							if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.REMOVE_ROOMS)) {
								roomTilesToRemove.add(tile.getTilePosition());
							} else {
								Designation designationToApply = interactionStateContainer.getInteractionMode().getDesignationToApply();
								if (tile.getDesignation() != null) {
									messageDispatcher.dispatchMessage(MessageType.REMOVE_DESIGNATION, new RemoveDesignationMessage(tile));
								}
								tile.setDesignation(designationToApply);
								messageDispatcher.dispatchMessage(MessageType.DESIGNATION_APPLIED, new ApplyDesignationMessage(tile, designationToApply, interactionStateContainer.getInteractionMode()));
							}
						}
					} else {
						switch (interactionStateContainer.getInteractionMode()) {
							case CANCEL -> {

								switch (interactionStateContainer.getGameViewMode()) {
									case DEFAULT -> {
										if (tile.getDesignation() != null) {
											messageDispatcher.dispatchMessage(MessageType.REMOVE_DESIGNATION, new RemoveDesignationMessage(tile));
										}
										if (tile.hasConstruction()) {
											if (tile.hasWallConstruction()) {
												for (MapTile neighbourTile : gameContext.getAreaMap().getOrthogonalNeighbours(x, y).values()) {
													if (neighbourTile.hasDoorway()) {
														messageDispatcher.dispatchMessage(MessageType.DECONSTRUCT_DOOR, neighbourTile.getDoorway());
													}
													if (neighbourTile.hasDoorwayConstruction()) {
														messageDispatcher.dispatchMessage(MessageType.CANCEL_CONSTRUCTION, neighbourTile.getConstruction());
													}
												}
											}

											messageDispatcher.dispatchMessage(MessageType.CANCEL_CONSTRUCTION, tile.getConstruction());
										}
									}
									case ROOFING_INFO -> {
										if (tile.getRoof().getState().equals(OPEN) && !tile.getRoof().getConstructionState().equals(NONE)) {
											messageDispatcher.dispatchMessage(MessageType.ROOF_CONSTRUCTION_QUEUE_CHANGE, new TileConstructionQueueMessage(tile, false));
											messageDispatcher.dispatchMessage(MessageType.ROOF_DECONSTRUCTION_QUEUE_CHANGE, new TileDeconstructionQueueMessage(tile, false));
										}
									}
									case PIPING -> {
										if (tile.getUnderTile() != null && tile.getUnderTile().getPipeConstructionState().equals(PipeConstructionState.READY_FOR_CONSTRUCTION)) {
											messageDispatcher.dispatchMessage(MessageType.PIPE_CONSTRUCTION_QUEUE_CHANGE, new TileConstructionQueueMessage(tile, false));
											messageDispatcher.dispatchMessage(MessageType.PIPE_DECONSTRUCTION_QUEUE_CHANGE, new TileDeconstructionQueueMessage(tile, false));
										}
									}
									case MECHANISMS -> {
										if (tile.getUnderTile() != null && tile.getUnderTile().getQueuedMechanismType() != null) {
											messageDispatcher.dispatchMessage(MessageType.MECHANISM_CONSTRUCTION_REMOVED, new TileConstructionQueueMessage(tile, false));
											messageDispatcher.dispatchMessage(MessageType.MECHANISM_DECONSTRUCTION_QUEUE_CHANGE, new TileDeconstructionQueueMessage(tile, false));
										}
									}
								}
							}
							case DECONSTRUCT -> {
								switch (interactionStateContainer.getGameViewMode()) {
									case DEFAULT -> {
										if (tile.getFloor().hasBridge() || tile.hasDoorway() || tile.getEntities().stream().anyMatch(e -> e.getType().equals(FURNITURE)) ||
												tile.hasChannel() || (tile.hasFloor() && tile.getFloor().getFloorType().isConstructed()) ||
												(tile.hasWall() && tile.getWall().getWallType().isConstructed())) {
											if (tile.getDesignation() == null) {

												tile.setDesignation(deconstructDesignation);
												messageDispatcher.dispatchMessage(MessageType.DESIGNATION_APPLIED, new ApplyDesignationMessage(tile, deconstructDesignation, interactionStateContainer.getInteractionMode()));
											}
										}
									}
									case MECHANISMS -> {
										if (tile.getExploration().equals(EXPLORED) && tile.hasPowerMechanism()) {
											messageDispatcher.dispatchMessage(MessageType.MECHANISM_DECONSTRUCTION_QUEUE_CHANGE, new TileDeconstructionQueueMessage(tile, true));
										}
									}
									case PIPING -> {
										if (tile.getExploration().equals(EXPLORED) && tile.hasPipe()) {
											messageDispatcher.dispatchMessage(MessageType.PIPE_DECONSTRUCTION_QUEUE_CHANGE, new TileDeconstructionQueueMessage(tile, true));
										}
									}
									case ROOFING_INFO -> {
										if (tile.getExploration().equals(EXPLORED) && tile.getRoof().getState().equals(CONSTRUCTED) &&
												tile.getRoof().getConstructionState().equals(NONE)) {
											messageDispatcher.dispatchMessage(MessageType.ROOF_DECONSTRUCTION_QUEUE_CHANGE, new TileDeconstructionQueueMessage(tile, true));
										}
									}
								}
							}
							case SET_JOB_PRIORITY -> {
								JobPriority priorityToApply = interactionStateContainer.getJobPriorityToApply();

								for (Job job : jobStore.getJobsAtLocation(tile.getTilePosition())) {
									job.setJobPriority(priorityToApply);
								}

								if (tile.hasConstruction()) {
									tile.getConstruction().setPriority(priorityToApply, messageDispatcher);
								}

								for (Entity entity : tile.getEntities()) {
									if (entity.getBehaviourComponent() instanceof Prioritisable) {
										Prioritisable prioritisableBehaviour = (Prioritisable) entity.getBehaviourComponent();
										prioritisableBehaviour.setPriority(priorityToApply);
									}
								}
							}
							case DESIGNATE_ROOFING -> {
								if (interactionStateContainer.getInteractionMode().tileDesignationCheck.shouldDesignationApply(tile)) {
									messageDispatcher.dispatchMessage(MessageType.ROOF_CONSTRUCTION_QUEUE_CHANGE, new TileConstructionQueueMessage(tile, true));
								}
							}
							case DESIGNATE_PIPING -> {
								if (interactionStateContainer.getInteractionMode().tileDesignationCheck.shouldDesignationApply(tile)) {
									messageDispatcher.dispatchMessage(MessageType.PIPE_CONSTRUCTION_QUEUE_CHANGE, new TileConstructionQueueMessage(tile, true));
								}
							}
							default -> Logger.warn(String.format("Unhandled area selection message %s in %s", interactionStateContainer.getInteractionMode().name(), getClass().getSimpleName()));
						}
					}

				}
			}
		}

		if (!roomTilesToRemove.isEmpty()) {
			removeRoomTiles(roomTilesToRemove);
		}
		return true;
	}

	private void handleAttackOrCancelCreaturesInSelection(AreaSelectionMessage areaSelectionMessage) {
		Squad squad = interactionStateContainer.getSelectable().getSquad();
		if (squad == null) {
			Logger.error("Trying to handle attack or cancel while squad is null");
			return;
		}

		Set<Long> currentlySelected = squad.getAttackEntityIds();
		Set<Long> newlySelected = getAttackableCreatures(areaSelectionMessage.getMinPoint(), areaSelectionMessage.getMaxPoint(), gameContext)
				.stream()
				.map(Entity::getId)
				.collect(toSet());

		if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.SQUAD_ATTACK_CREATURE)) {
			newlySelected.removeAll(currentlySelected);
			if (!newlySelected.isEmpty()) {
				squad.getAttackEntityIds().addAll(newlySelected);
				messageDispatcher.dispatchMessage(MessageType.MILITARY_SQUAD_ORDERS_CHANGED, new SquadOrderChangeMessage(squad, SquadOrderType.COMBAT));
			}
		} else {
			squad.getAttackEntityIds().removeAll(newlySelected);
			if (squad.getAttackEntityIds().isEmpty()) {
				messageDispatcher.dispatchMessage(MessageType.MILITARY_SQUAD_ORDERS_CHANGED, new SquadOrderChangeMessage(squad, SquadOrderType.TRAINING));
			}
		}
	}

	public static Set<Entity> getAttackableCreatures(Vector2 minPoint, Vector2 maxPoint, GameContext gameContext) {
		GridPoint2 minTile = new GridPoint2(MathUtils.floor(minPoint.x), MathUtils.floor(minPoint.y));
		GridPoint2 maxTile = new GridPoint2(MathUtils.floor(maxPoint.x), MathUtils.floor(maxPoint.y));

		Set<Entity> selected = new HashSet<>();

		for (int x = minTile.x; x <= maxTile.x; x++) {
			for (int y = minTile.y; y <= maxTile.y; y++) {
				MapTile tile = gameContext.getAreaMap().getTile(x, y);
				if (tile != null && tile.getExploration().equals(TileExploration.EXPLORED)) {
					tile.getEntities().stream()
							.filter(MapMessageHandler::isAttackableCreature)
							.filter(entity -> isWithinBounds(entity, minPoint, maxPoint))
							.forEach(selected::add);
				}
			}
		}
		return selected;
	}

	public static boolean isAttackableCreature(Entity entity) {
		if (entity.getPhysicalEntityComponent().getAttributes() instanceof CreatureEntityAttributes creatureEntityAttributes) {
			return !creatureEntityAttributes.getConsciousness().equals(Consciousness.DEAD) &&
					!entity.getOrCreateComponent(FactionComponent.class).getFaction().equals(Faction.SETTLEMENT);
		} else {
			return false;
		}
	}

	private static boolean isWithinBounds(Entity entity, Vector2 minPoint, Vector2 maxPoint) {
		Vector2 worldPosition = entity.getLocationComponent().getWorldPosition();
		if (worldPosition != null) {
			return minPoint.x <= worldPosition.x && worldPosition.x <= maxPoint.x &&
					minPoint.y <= worldPosition.y && worldPosition.y <= maxPoint.y;
		} else {
			return false;
		}
	}

	private void removeRoomTiles(Set<GridPoint2> roomTilesToRemove) {
		Set<Room> roomsWithRemovedTiles = new HashSet<>();

		for (GridPoint2 tileLocation : roomTilesToRemove) {
			MapTile tile = gameContext.getAreaMap().getTile(tileLocation);
			if (tile != null) {
				RoomTile roomTile = tile.getRoomTile();
				if (roomTile != null) {
					roomsWithRemovedTiles.add(roomTile.getRoom());
					roomTile.getRoom().removeTile(tileLocation);
					tile.setRoomTile(null);
				}
			}
		}

		for (Room modifiedRoom : roomsWithRemovedTiles) {
			if (modifiedRoom.isEmpty()) {
				roomStore.remove(modifiedRoom);
			} else {
				// Need to see if this room has been split into more than 1 section
				splitRoomIfNecessary(modifiedRoom);
			}

		}

	}

	private void splitRoomIfNecessary(Room modifiedRoom) {
		Set<GridPoint2> traversed = new HashSet<>();
		IntMap<Set<GridPoint2>> tileGroups = new IntMap<>();
		int cursor = 1;

		Set<GridPoint2> allRoomTiles = modifiedRoom.keySet();
		for (GridPoint2 roomTile : allRoomTiles) {
			if (!traversed.contains(roomTile)) {
				Set<GridPoint2> roomTileGroup = new HashSet<>();
				tileGroups.put(cursor, roomTileGroup);
				cursor++;

				addAdjacentTilesToGroup(roomTile, traversed, roomTileGroup, modifiedRoom);
			}
		}

		if (tileGroups.size > 1) {
			// Need to create new room for each other section
			for (int groupCursor = 1; groupCursor < tileGroups.size; groupCursor++) {
				Set<GridPoint2> newRoomGroup = tileGroups.get(groupCursor);

				Room newRoom = roomFactory.createBasedOn(modifiedRoom);

				for (GridPoint2 positionToMove : newRoomGroup) {
					RoomTile roomTileToMove = modifiedRoom.removeTile(positionToMove);
					roomTileToMove.setRoom(newRoom);
					newRoom.addTile(roomTileToMove);
				}
				newRoom.updateLayout(gameContext.getAreaMap());
			}
		}
		modifiedRoom.updateLayout(gameContext.getAreaMap());
	}

	private void addAdjacentTilesToGroup(GridPoint2 currentTile, Set<GridPoint2> traversed, Set<GridPoint2> roomTileGroup, Room currentRoom) {
		roomTileGroup.add(currentTile);
		traversed.add(currentTile);

		for (MapTile neighbourTile : gameContext.getAreaMap().getOrthogonalNeighbours(currentTile.x, currentTile.y).values()) {
			if (traversed.contains(neighbourTile.getTilePosition())) {
				continue;
			}
			if (neighbourTile.hasRoom() && neighbourTile.getRoomTile().getRoom().getRoomId() == currentRoom.getRoomId()) {
				addAdjacentTilesToGroup(neighbourTile.getTilePosition(), traversed, roomTileGroup, currentRoom);
			}
		}
	}

	private boolean handle(EntityPositionChangedMessage message) {
		Entity entity = message.movingEntity;
		if (entity == null) {
			Logger.error(message.getClass().getSimpleName() + " handled with null entity");
			return true;
		}
		if (message.oldPosition != null) {
			MapTile oldCell = gameContext.getAreaMap().getTile(message.oldPosition);
			if (oldCell != null) {
				Entity removed = oldCell.removeEntity(entity.getId());
				if (removed == null) {
					Logger.error("Could not find entity " + entity.toString() + " in tile at " + message.oldPosition);
				}

				for (GridPoint2 otherTilePosition : entity.calculateOtherTilePositions()) {
					MapTile otherTile = gameContext.getAreaMap().getTile(otherTilePosition);
					if (otherTile != null) {
						otherTile.removeEntity(entity.getId());
					}
				}
			}
		}

		if (message.newPosition != null) {
			MapTile newCell = gameContext.getAreaMap().getTile(message.newPosition);
			if (newCell == null) {
				Logger.error("Entity " + entity.toString() + " appears to have moved off the map and/or a tile has disappeared, needs investigating");
			} else {
				newCell.addEntity(entity);
				for (GridPoint2 otherTilePosition : entity.calculateOtherTilePositions(message.newPosition)) {
					MapTile otherTile = gameContext.getAreaMap().getTile(otherTilePosition);
					if (otherTile != null) {
						otherTile.addEntity(entity);
					}
				}
			}

		}
		return true;
	}

	private boolean addWall(GridPoint2 location, GameMaterial wallMaterial, WallType wallType) {
		MapTile tileToAddWallTo = gameContext.getAreaMap().getTile(location);

		TileNeighbours tileNeighbours = gameContext.getAreaMap().getNeighbours(location);
		WallLayout wallLayout = new WallLayout(tileNeighbours);
		TileRoofState newRoofState = TileRoofState.CONSTRUCTED;
		if (tileToAddWallTo.getRoof().getState().equals(TileRoofState.MOUNTAIN_ROOF)) {
			newRoofState = TileRoofState.MOUNTAIN_ROOF;
		} else if (tileToAddWallTo.getRoof().getState().equals(TileRoofState.MINED)) {
			newRoofState = TileRoofState.MINED;
		}

		tileToAddWallTo.setWall(new Wall(wallLayout, wallType, wallMaterial), new TileRoof(newRoofState, wallMaterial));
		roofConstructionManager.supportConstructed(tileToAddWallTo);
		roofConstructionManager.roofConstructed(tileToAddWallTo);
		updateTile(tileToAddWallTo, gameContext, messageDispatcher);
		messageDispatcher.dispatchMessage(MessageType.WALL_CREATED, location);

		propagateDarknessFromTile(tileToAddWallTo, gameContext, outdoorLightProcessor);

		updateRegions(Set.of(tileToAddWallTo));
		return true;
	}


	private boolean handleAddChannel(GridPoint2 location) {
		MapTile tileToAddChannelTo = gameContext.getAreaMap().getTile(location);

		TileNeighbours tileNeighbours = gameContext.getAreaMap().getNeighbours(location);
		ChannelLayout channelLayout = new ChannelLayout(tileNeighbours);

		if (tileToAddChannelTo.hasRoom()) {
			messageDispatcher.dispatchMessage(MessageType.REMOVE_ROOM_TILES, Set.of(location));
		}

		UnderTile underTile = tileToAddChannelTo.getOrCreateUnderTile();
		underTile.setChannelLayout(channelLayout);
		updateTile(tileToAddChannelTo, gameContext, messageDispatcher);

		updateRegions(tileToAddChannelTo, tileNeighbours);

		return true;
	}

	private void updateRegions(Set<MapTile> modifiedTiles) {
		TiledMap areaMap = gameContext.getAreaMap();
		MapTile.RegionType regionType = modifiedTiles.stream()
				.map(MapTile::getRegionType)
				.distinct()
				.reduce((a, b) -> {
					throw new IllegalArgumentException(String.format("Should only have one region type in the modified tile set: %s, %s", a, b));
				})
				.orElseThrow(() -> {
					throw new IllegalArgumentException("Should be one region type");
				});

		Set<MapTile> neighbourTiles = modifiedTiles.stream()
				.flatMap(t -> areaMap.getOrthogonalNeighbours(t.getTileX(), t.getTileY()).values().stream())
				.filter(Objects::nonNull)
				.filter(Predicate.not(modifiedTiles::contains))
				.collect(toSet());

		Integer regionIdToUse = null;
		for (MapTile neighbourTile : neighbourTiles) {
			if (neighbourTile.getRegionType() == regionType) {
				if (regionIdToUse == null) {
					regionIdToUse = neighbourTile.getRegionId();
				} else if (neighbourTile.getRegionId() != regionIdToUse) {
					replaceRegion(neighbourTile, regionIdToUse); //replace an existing neighbouring region with this
				}
			}
		}

		if (regionIdToUse == null) {
			regionIdToUse = areaMap.createNewRegionId();
		}

		for (MapTile modifiedTile : modifiedTiles) {
			modifiedTile.setRegionId(regionIdToUse);
		}

		//now check if this divides an already existing region
		MapTile[] neighbourTileArray = neighbourTiles.toArray(MapTile[]::new);
		for (int i = 0; i < neighbourTileArray.length; i++) {
			for (int j = i + 1; j < neighbourTileArray.length; j++) {
				MapTile left = neighbourTileArray[i];
				MapTile right = neighbourTileArray[j];

				if (left != right &&
					left.getRegionType() == right.getRegionType() &&
					left.getRegionId() == right.getRegionId() &&
					left.getRegionType() != regionType) {

					if (!canTraverseForSameRegionType(left, right)) {
						int newRegionId = gameContext.getAreaMap().createNewRegionId();
						replaceRegion(left, newRegionId);
					}
				}
			}
		}
	}

	private void updateRegions(MapTile modifiedTile, TileNeighbours tileNeighbours) {
		MapTile north = tileNeighbours.get(CompassDirection.NORTH);
		MapTile south = tileNeighbours.get(CompassDirection.SOUTH);
		MapTile east = tileNeighbours.get(CompassDirection.EAST);
		MapTile west = tileNeighbours.get(CompassDirection.WEST);

		// Change the tile's region to be neighbouring same region type, or else a new region
		MapTile.RegionType myRegionType = modifiedTile.getRegionType();
		Integer neighbourRegionId = null;
		for (MapTile neighbourTile : Arrays.asList(north, south, east, west)) {
			if (neighbourTile != null && neighbourTile.getRegionType().equals(myRegionType)) {
				if (neighbourRegionId == null) {
					neighbourRegionId = neighbourTile.getRegionId();
					modifiedTile.setRegionId(neighbourRegionId);
				} else if (neighbourTile.getRegionId() != neighbourRegionId) {
					// Encountered a different neighbour region ID, merge together
					replaceRegion(neighbourTile, neighbourRegionId);
				}
			} else if (neighbourTile != null && neighbourTile.hasRoom()) {
				neighbourTile.getRoomTile().getRoom().checkIfEnclosed(gameContext.getAreaMap());
			}
		}
		if (neighbourRegionId == null) {
			neighbourRegionId = gameContext.getAreaMap().createNewRegionId();
			modifiedTile.setRegionId(neighbourRegionId);
		}

		// Figure out if new channel has split region into two - if so, create new region on one side
		boolean emptyEitherSide = false;
		MapTile sideA = null;
		MapTile sideB = null;

		List<List<MapTile>> pairings = Arrays.asList(
				Arrays.asList(north, south),
				Arrays.asList(east, west),

				Arrays.asList(north, west),
				Arrays.asList(north, east),
				Arrays.asList(south, east),
				Arrays.asList(south, west)
		);


		for (List<MapTile> pair : pairings) {
			if (pair.get(0) != null && !pair.get(0).getRegionType().equals(myRegionType) && pair.get(1) != null && !pair.get(1).getRegionType().equals(myRegionType) &&
					pair.get(0).getRegionType().equals(pair.get(1).getRegionType())) {
				emptyEitherSide = true;
				sideA = pair.get(0);
				sideB = pair.get(1);
				break;
			}
		}

		if (emptyEitherSide) {
			boolean otherSideFound = canTraverseForSameRegionType(sideA, sideB);

			if (!otherSideFound) {
				int newRegionId = gameContext.getAreaMap().createNewRegionId();
				replaceRegion(sideA, newRegionId);
			}
		}
	}

	private boolean canTraverseForSameRegionType(MapTile start, MapTile end) {
		// Flood fill from one side until the other side is found, otherwise set all area of flood fill to new region
		Set<MapTile> explored = new HashSet<>();
		Deque<MapTile> frontier = new ArrayDeque<>();
		frontier.add(start);

		boolean otherSideFound = false;

		while (!frontier.isEmpty()) {
			MapTile current = frontier.pop();

			if (current.equals(end)) {
				otherSideFound = true;
				break;
			}

			explored.add(current);

			for (MapTile orthogonalNeighbour : gameContext.getAreaMap().getOrthogonalNeighbours(current.getTileX(), current.getTileY()).values()) {
				if (!explored.contains(orthogonalNeighbour) && !frontier.contains(orthogonalNeighbour)) {
					if (orthogonalNeighbour.getRegionType().equals(start.getRegionType())) {
						frontier.add(orthogonalNeighbour);
					}
				}
			}
		}
		return otherSideFound;
	}

	public static void propagateDarknessFromTile(MapTile tile, GameContext gameContext, OutdoorLightProcessor outdoorLightProcessor) {
		EnumMap<CompassDirection, MapVertex> cellVertices = gameContext.getAreaMap().getVertexNeighboursOfCell(tile);
		for (MapVertex cellVertex : cellVertices.values()) {
			TileNeighbours neighboursOfCellVertex = gameContext.getAreaMap().getTileNeighboursOfVertex(cellVertex);
			boolean vertexSurroundedByIndoorCells = true;
			for (MapTile vertexNeighbour : neighboursOfCellVertex.values()) {
				if (vertexNeighbour != null && vertexNeighbour.getRoof().getState().equals(TileRoofState.OPEN)) {
					vertexSurroundedByIndoorCells = false;
					break;
				}
			}
			if (vertexSurroundedByIndoorCells) {
				outdoorLightProcessor.propagateDarknessFromVertex(gameContext.getAreaMap(), cellVertex);
			}
		}
	}

	private boolean handleRemoveWall(GridPoint2 location) {
		MapTile tile = gameContext.getAreaMap().getTile(location);
		if (tile != null && tile.hasWall()) {
			if (tile.getRoof().getState().equals(TileRoofState.MOUNTAIN_ROOF)) {
				tile.getRoof().setState(TileRoofState.MINED);
			}

			tile.setWall(null, tile.getRoof());
			Designation designation = tile.getDesignation();
			if (designation != null) {
				messageDispatcher.dispatchMessage(MessageType.REMOVE_DESIGNATION, new RemoveDesignationMessage(tile));
			}
			for (MapVertex vertex : gameContext.getAreaMap().getVertexNeighboursOfCell(tile).values()) {
				outdoorLightProcessor.propagateLightFromMapVertex(gameContext.getAreaMap(), vertex, vertex.getOutsideLightAmount());
			}

			updateTile(tile, gameContext, messageDispatcher);

			messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(wallRemovedParticleEffectType,
					Optional.empty(), Optional.of(new JobTarget(tile)), (p) -> {
			}));
			messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(wallRemovedSoundAsset, -1L,
					tile.getWorldPositionOfCenter(), null));

			Integer neighbourRegionId = null;
			for (MapTile neighbourTile : gameContext.getAreaMap().getOrthogonalNeighbours(location.x, location.y).values()) {
				if (neighbourTile.hasFloor() && !neighbourTile.getFloor().isRiverTile()) {
					if (neighbourRegionId == null) {
						neighbourRegionId = neighbourTile.getRegionId();
						tile.setRegionId(neighbourRegionId);
					} else if (neighbourTile.getRegionId() != neighbourRegionId) {
						// Encountered a different neighbour region ID, merge together
						replaceRegion(neighbourTile, neighbourRegionId);
					}
				}
				if (neighbourTile.hasDoorway()) {
					messageDispatcher.dispatchMessage(MessageType.DECONSTRUCT_DOOR, neighbourTile.getDoorway());
				}
				if (neighbourTile.hasDoorwayConstruction()) {
					messageDispatcher.dispatchMessage(MessageType.CANCEL_CONSTRUCTION, neighbourTile.getConstruction());
				}
				if (neighbourTile.hasRoom()) {
					neighbourTile.getRoomTile().getRoom().checkIfEnclosed(gameContext.getAreaMap());
				}
			}

			if (neighbourRegionId == null) {
				neighbourRegionId = gameContext.getAreaMap().createNewRegionId();
				tile.setRegionId(neighbourRegionId);
			}
			messageDispatcher.dispatchMessage(MessageType.WALL_REMOVED, location);
		}
		return true;
	}


	private boolean handleRemoveChannel(GridPoint2 location) {
		MapTile tile = gameContext.getAreaMap().getTile(location);
		if (tile != null && tile.hasChannel()) {
			tile.getUnderTile().setChannelLayout(null);
			if (!tile.getUnderTile().liquidCanFlow()) {
				tile.getUnderTile().setLiquidFlow(null);
			}
			updateTile(tile, gameContext, messageDispatcher);

			updateRegions(tile, gameContext.getAreaMap().getNeighbours(location));

			Integer neighbourRegionId = null;
			MapTile unexploredTile = null;
			for (MapTile neighbourTile : gameContext.getAreaMap().getOrthogonalNeighbours(location.x, location.y).values()) {
				if (neighbourTile.hasFloor() && !neighbourTile.getFloor().isRiverTile()) {
					if (!neighbourTile.getExploration().equals(TileExploration.EXPLORED)) {
						unexploredTile = neighbourTile;
					}
					if (neighbourRegionId == null) {
						neighbourRegionId = neighbourTile.getRegionId();
						tile.setRegionId(neighbourRegionId);
					} else if (neighbourTile.getRegionId() != neighbourRegionId) {
						// Encountered a different neighbour region ID, merge together
						replaceRegion(neighbourTile, neighbourRegionId);
					}
				}
				if (neighbourTile.hasDoorway()) {
					messageDispatcher.dispatchMessage(MessageType.DECONSTRUCT_DOOR, neighbourTile.getDoorway());
				}
				if (neighbourTile.hasRoom()) {
					neighbourTile.getRoomTile().getRoom().checkIfEnclosed(gameContext.getAreaMap());
				}
			}
			if (neighbourRegionId == null) {
				neighbourRegionId = gameContext.getAreaMap().createNewRegionId();
				tile.setRegionId(neighbourRegionId);
			}
			messageDispatcher.dispatchMessage(MessageType.WALL_REMOVED, location);
		}
		return true;
	}

	/**
	 * This method flood-fills the region specified in targetTile with replacementRegionId
	 */
	private void replaceRegion(MapTile initialTargetTile, int replacementRegionId) {
		int regionToReplace = initialTargetTile.getRegionId();
		Set<MapTile> visited = new HashSet<>();
		Queue<MapTile> frontier = new LinkedList<>();
		Set<Zone> zonesEncountered = new HashSet<>();
		frontier.add(initialTargetTile);

		while (!frontier.isEmpty()) {
			MapTile currentTile = frontier.poll();
			if (visited.contains(currentTile)) {
				continue;
			}
			currentTile.setRegionId(replacementRegionId);
			zonesEncountered.addAll(currentTile.getZones());
			visited.add(currentTile);

			for (MapTile neighbourTile : gameContext.getAreaMap().getOrthogonalNeighbours(currentTile.getTileX(), currentTile.getTileY()).values()) {
				if (visited.contains(neighbourTile)) {
					continue;
				}
				if (neighbourTile.getRegionId() == regionToReplace) {
					frontier.add(neighbourTile);
				}
			}
		}

		for (Zone movedZone : zonesEncountered) {
			gameContext.getAreaMap().removeZone(movedZone);
			movedZone.recalculate(gameContext.getAreaMap());
			movedZone.setRegionId(replacementRegionId);
			if (!movedZone.isEmpty()) {
				gameContext.getAreaMap().addZone(movedZone);
			}
		}
	}

	public static void updateTile(MapTile tile, GameContext gameContext, MessageDispatcher messageDispatcher) {
		TileNeighbours neighbours = gameContext.getAreaMap().getNeighbours(tile.getTileX(), tile.getTileY());
		tile.update(neighbours, gameContext.getAreaMap().getVertices(tile.getTileX(), tile.getTileY()), messageDispatcher);

		for (MapTile cellNeighbour : neighbours.values()) {
			cellNeighbour.update(gameContext.getAreaMap().getNeighbours(cellNeighbour.getTileX(), cellNeighbour.getTileY()),
					gameContext.getAreaMap().getVertices(cellNeighbour.getTileX(), cellNeighbour.getTileY()), messageDispatcher);
		}

		for (Zone zone : new ArrayList<>(tile.getZones())) {
			zone.recalculate(gameContext.getAreaMap());
			if (zone.isEmpty()) {
				gameContext.getAreaMap().removeZone(zone);
			}
		}

	}

	public void replaceFloor(GridPoint2 location, FloorType floorType, GameMaterial material) {
		MapTile mapTile = gameContext.getAreaMap().getTile(location);

		TileFloor newFloor = new TileFloor(floorType, material);
		mapTile.replaceFloor(newFloor);

		updateTile(mapTile, gameContext, messageDispatcher);
	}

	public void undoReplaceFloor(GridPoint2 location) {
		MapTile mapTile = gameContext.getAreaMap().getTile(location);
		mapTile.popFloor();

		updateTile(mapTile, gameContext, messageDispatcher);
	}

	public void setTransitoryFloor(GridPoint2 location, FloorType floorType, GameMaterial material) {
		MapTile mapTile = gameContext.getAreaMap().getTile(location);

		TileFloor newFloor = new TileFloor(floorType, material);
		mapTile.setTransitoryFloor(newFloor);

		updateTile(mapTile, gameContext, messageDispatcher);
	}

	public void removeTransitoryFloor(GridPoint2 location) {
		MapTile mapTile = gameContext.getAreaMap().getTile(location);
		mapTile.removeTransitoryFloor();

		updateTile(mapTile, gameContext, messageDispatcher);
	}

	public static void markAsOutside(MapTile tile, GameContext gameContext, OutdoorLightProcessor outdoorLightProcessor) {
		tile.getRoof().setState(TileRoofState.OPEN);
		tile.getRoof().setRoofMaterial(GameMaterial.NULL_MATERIAL);

		for (MapVertex vertex : gameContext.getAreaMap().getVertexNeighboursOfCell(tile).values()) {
			vertex.setOutsideLightAmount(1.0f);
			outdoorLightProcessor.propagateLightFromMapVertex(gameContext.getAreaMap(), vertex, 1f);
		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
