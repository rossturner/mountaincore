package technology.rocketjump.saul.rooms.constructions;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.GridPoint2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.model.WallType;
import technology.rocketjump.saul.audio.model.SoundAsset;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.entities.behaviour.furniture.Prioritisable;
import technology.rocketjump.saul.entities.components.ItemAllocationComponent;
import technology.rocketjump.saul.entities.factories.FurnitureEntityAttributesFactory;
import technology.rocketjump.saul.entities.factories.FurnitureEntityFactory;
import technology.rocketjump.saul.entities.factories.ItemEntityAttributesFactory;
import technology.rocketjump.saul.entities.factories.ItemEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.jobs.model.JobTarget;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.mapping.tile.floor.BridgeTile;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.materials.model.GameMaterialType;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.*;
import technology.rocketjump.saul.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.saul.particles.model.ParticleEffectType;
import technology.rocketjump.saul.rooms.Bridge;
import technology.rocketjump.saul.rooms.HaulingAllocation;
import technology.rocketjump.saul.rooms.Room;

import java.util.*;

import static technology.rocketjump.saul.entities.components.ItemAllocation.Purpose.PLACED_FOR_CONSTRUCTION;
import static technology.rocketjump.saul.entities.model.physical.item.QuantifiedItemTypeWithMaterial.convert;
import static technology.rocketjump.saul.mapping.MapMessageHandler.updateTile;
import static technology.rocketjump.saul.materials.model.GameMaterial.NULL_MATERIAL;
import static technology.rocketjump.saul.misc.VectorUtils.toVector;

@Singleton
public class ConstructionMessageHandler implements GameContextAware, Telegraph {

	private final MessageDispatcher messageDispatcher;
	private final ConstructionStore constructionStore;
	private final FurnitureEntityAttributesFactory furnitureEntityAttributesFactory;
	private final FurnitureEntityFactory furnitureEntityFactory;
	private final ItemEntityAttributesFactory itemEntityAttributesFactory;
	private final ItemEntityFactory itemEntityFactory;
	private final ParticleEffectType dustCloudParticleEffect;
	private final Map<GameMaterialType, SoundAsset> completionSoundMapping = new EnumMap<>(GameMaterialType.class);

	private GameContext gameContext;

	@Inject
	public ConstructionMessageHandler(MessageDispatcher messageDispatcher, ConstructionStore constructionStore,
									  FurnitureEntityAttributesFactory furnitureEntityAttributesFactory,
									  FurnitureEntityFactory furnitureEntityFactory,
									  ItemEntityAttributesFactory itemEntityAttributesFactory,
									  ItemEntityFactory itemEntityFactory, SoundAssetDictionary soundAssetDictionary,
									  ParticleEffectTypeDictionary particleEffectTypeDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.constructionStore = constructionStore;
		this.furnitureEntityAttributesFactory = furnitureEntityAttributesFactory;
		this.furnitureEntityFactory = furnitureEntityFactory;
		this.itemEntityAttributesFactory = itemEntityAttributesFactory;
		this.itemEntityFactory = itemEntityFactory;

		dustCloudParticleEffect = particleEffectTypeDictionary.getByName("Dust cloud above"); // MODDING expose this
		// FIXME this is also duplicated in FurnitureMessageHandler
		completionSoundMapping.put(GameMaterialType.WOOD, soundAssetDictionary.getByName("HeavyWoodItem")); // MODDING Expose this
		completionSoundMapping.put(GameMaterialType.STONE, soundAssetDictionary.getByName("HeavyStoneItem")); // MODDING Expose this

		messageDispatcher.addListener(this, MessageType.FURNITURE_PLACEMENT);
		messageDispatcher.addListener(this, MessageType.DOOR_PLACEMENT);
		messageDispatcher.addListener(this, MessageType.BRIDGE_PLACEMENT);
		messageDispatcher.addListener(this, MessageType.CANCEL_CONSTRUCTION);
		messageDispatcher.addListener(this, MessageType.CONSTRUCTION_COMPLETED);
		messageDispatcher.addListener(this, MessageType.WALL_PLACEMENT);
		messageDispatcher.addListener(this, MessageType.TRANSFORM_CONSTRUCTION);
		messageDispatcher.addListener(this, MessageType.DECONSTRUCT_BRIDGE);
		messageDispatcher.addListener(this, MessageType.CONSTRUCTION_PRIORITY_CHANGED);
	}


	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.FURNITURE_PLACEMENT: {
				return handleFurniturePlacement((Entity) msg.extraInfo);
			}
			case MessageType.DOOR_PLACEMENT: {
				return handle((DoorwayPlacementMessage) msg.extraInfo);
			}
			case MessageType.WALL_PLACEMENT: {
				return handle((WallsPlacementMessage) msg.extraInfo);
			}
			case MessageType.BRIDGE_PLACEMENT: {
				return handleBridgePlacement((Bridge) msg.extraInfo);
			}
			case MessageType.CANCEL_CONSTRUCTION: {
				Construction construction = (Construction) msg.extraInfo;
				// De-requestAllocation items in location
				for (GridPoint2 tileLocation : construction.getTileLocations()) {
					MapTile tileAtLocation = gameContext.getAreaMap().getTile(tileLocation);
					if (tileAtLocation != null) {
						for (Entity entity : tileAtLocation.getEntities()) {
							if (entity.getType().equals(EntityType.ITEM)) {
								if (construction.isItemUsedInConstruction(entity)) {
									entity.getOrCreateComponent(ItemAllocationComponent.class).cancelAll(PLACED_FOR_CONSTRUCTION);
								}
							}
						}
						tileAtLocation.setConstruction(null);
						updateTile(tileAtLocation, gameContext, messageDispatcher);
					}
					messageDispatcher.dispatchMessage(MessageType.REMOVE_HAULING_JOBS_TO_POSITION, tileLocation);
				}

				if (construction.getConstructionJob() != null) {
					messageDispatcher.dispatchMessage(MessageType.JOB_REMOVED, construction.getConstructionJob());
				}
				messageDispatcher.dispatchMessage(MessageType.CONSTRUCTION_REMOVED, construction);
				constructionStore.remove(construction);
				return true;
			}
			case MessageType.CONSTRUCTION_COMPLETED: {
				Construction construction = (Construction) msg.extraInfo;
				if (construction != null) {
					switch (construction.getConstructionType()) {
						case WALL_CONSTRUCTION:
							handleWallConstructionCompleted((WallConstruction) construction);
							break;
						case FURNITURE_CONSTRUCTION:
						case DOORWAY_CONSTRUCTION:
							handleFurnitureConstructionCompleted((FurnitureConstruction) construction);
							break;
						case BRIDGE_CONSTRUCTION:
							handleBridgeConstructionCompleted((BridgeConstruction) construction);
							break;
						default:
							Logger.error("Not yet implemented CONSTRUCTION_COMPLETED handler for " + construction.getConstructionType());
					}
				} else {
					Logger.error("Null construction received in CONSTRUCTION_COMPLETED");
				}
				return true;
			}
			case MessageType.TRANSFORM_CONSTRUCTION: {
				return handle((TransformConstructionMessage)msg.extraInfo);
			}
			case MessageType.DECONSTRUCT_BRIDGE: {
				Bridge bridgeToRemove = (Bridge)msg.extraInfo;
				deconstructBridge(bridgeToRemove);
				return true;
			}
			case MessageType.CONSTRUCTION_PRIORITY_CHANGED: {
				constructionStore.priorityChanged();
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private boolean handle(WallsPlacementMessage message) {
		for (WallConstruction wallConstruction : message.wallConstructions) {
			constructionStore.addExisting(wallConstruction);
			MapTile targetTile = gameContext.getAreaMap().getTile(wallConstruction.getPrimaryLocation());
			if (targetTile != null && targetTile.hasRoom()) {
				messageDispatcher.dispatchMessage(MessageType.REMOVE_ROOM_TILES, wallConstruction.getTileLocations());
			}
		}
		if (!message.wallConstructions.isEmpty()) {
			WallConstruction wallConstruction = message.wallConstructions.get(0);
			requestConstructionSoundAsset(wallConstruction);
		}

		return true;
	}

	private boolean handleBridgePlacement(Bridge bridge) {
		BridgeConstruction bridgeConstruction = new BridgeConstruction(bridge);
		requestConstructionSoundAsset(bridgeConstruction);
		constructionStore.create(bridgeConstruction);
		return true;
	}

	private boolean handleFurniturePlacement(Entity furnitureEntityToPlace) {
		FurnitureConstruction furnitureConstruction = new FurnitureConstruction(furnitureEntityToPlace.clone(messageDispatcher, gameContext));
		requestConstructionSoundAsset(furnitureConstruction);
		constructionStore.create(furnitureConstruction);
		return true;
	}

	private boolean handle(DoorwayPlacementMessage placeDoorwayMessage) {
		// Create entity to act as construction base
		FurnitureEntityAttributes attributes = furnitureEntityAttributesFactory.byName("SINGLE_DOOR_PLACEMENT", placeDoorwayMessage.getDoorwayMaterial());
		Entity placementDoorwayEntity = furnitureEntityFactory.create(attributes, placeDoorwayMessage.getTilePosition(), null, gameContext);
		DoorwayConstruction doorwayConstruction = new DoorwayConstruction(placementDoorwayEntity, placeDoorwayMessage);
		requestConstructionSoundAsset(doorwayConstruction);
		constructionStore.create(doorwayConstruction);
		return true;
	}

	private void handleFurnitureConstructionCompleted(FurnitureConstruction construction) {
		// Remove items in all covered tiles
		Map<Long, Entity> itemsRemovedFromConstruction = new HashMap<>();
		Room tempPlacedOnRoom = null;
		for (GridPoint2 tileLocation : construction.getTileLocations()) {
			MapTile tileAtLocation = gameContext.getAreaMap().getTile(tileLocation);
			if (tileAtLocation != null) {
				for (Entity entity : tileAtLocation.getEntities()) {
					if (entity.getType().equals(EntityType.ITEM)) {
						itemsRemovedFromConstruction.put(entity.getId(), entity);
						messageDispatcher.dispatchMessage(0.01f, MessageType.DESTROY_ENTITY, entity);
					}
				}
				tileAtLocation.setConstruction(null);

				if (!construction.isAutoCompleted()) {
					messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(dustCloudParticleEffect,
							Optional.empty(), Optional.of(new JobTarget(tileAtLocation)), (p) -> {}));
				}

				if (tileAtLocation.hasRoom()) {
					tempPlacedOnRoom = tileAtLocation.getRoomTile().getRoom();
				}
			}
		}
		final Room placedOnRoom = tempPlacedOnRoom;

		constructionStore.remove(construction);

		if (construction.getConstructionType().equals(ConstructionType.DOORWAY_CONSTRUCTION)) {
			DoorwayConstruction doorwayConstruction = (DoorwayConstruction) construction;
			GameMaterial originalMaterial = doorwayConstruction.getPlaceDoorwayMessage().getDoorwayMaterial();
			GameMaterial materialFromConstruction = findApplicableMaterial(itemsRemovedFromConstruction.values(), originalMaterial.getMaterialType());
			doorwayConstruction.getPlaceDoorwayMessage().setDoorwayMaterial(materialFromConstruction);
			messageDispatcher.dispatchMessage(MessageType.REMOVE_ROOM_TILES, construction.getTileLocations());
			messageDispatcher.dispatchMessage(MessageType.CREATE_DOORWAY, doorwayConstruction.getPlaceDoorwayMessage());
			return;
		}

		FurnitureEntityAttributes constructionAttributes = (FurnitureEntityAttributes) construction.getFurnitureEntityToBePlaced().getPhysicalEntityComponent().getAttributes();
		FurnitureEntityAttributes createdAttributes = furnitureEntityAttributesFactory.byType(
				constructionAttributes.getFurnitureType(), findApplicableMaterial(itemsRemovedFromConstruction.values(), constructionAttributes.getPrimaryMaterialType()));
		createdAttributes.setCurrentLayout(constructionAttributes.getCurrentLayout());

		messageDispatcher.dispatchMessage(MessageType.FURNITURE_CREATION_REQUEST, new FurnitureCreationRequestMessage(
				createdAttributes, itemsRemovedFromConstruction, construction.getPrimaryLocation(), construction.getTileLocations(),
				furnitureEntity -> {
					if (placedOnRoom != null && furnitureEntity.getBehaviourComponent() instanceof Prioritisable &&
							placedOnRoom.getBehaviourComponent() instanceof Prioritisable) {
						((Prioritisable)furnitureEntity.getBehaviourComponent()).setPriority(((Prioritisable)placedOnRoom.getBehaviourComponent()).getPriority());
					}
				}
		));
	}

	private void handleWallConstructionCompleted(WallConstruction construction) {
		GridPoint2 tileLocation = construction.getPrimaryLocation();
		MapTile tileAtLocation = gameContext.getAreaMap().getTile(tileLocation);
		tileAtLocation.setConstruction(null);
		constructionStore.remove(construction);
		messageDispatcher.dispatchMessage(MessageType.REMOVE_ROOM_TILES, construction.getTileLocations());

		if (tileAtLocation.hasWall()) {
			Logger.error("Wall already exists where trying to construct one at " + tileLocation);
			return;
		}

		ItemEntityAttributes itemAttributes = null;
		for (Entity entity : tileAtLocation.getEntities()) {
			if (entity.getType().equals(EntityType.ITEM)) {
				itemAttributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				messageDispatcher.dispatchMessage(0.01f, MessageType.DESTROY_ENTITY, entity);
			}
		}

		WallType wallType = construction.getWallTypeToConstruct();
		GameMaterial materialConstructed = NULL_MATERIAL;
		if (itemAttributes != null) {
			GameMaterial itemMaterial = itemAttributes.getMaterial(wallType.getMaterialType());
			if (itemMaterial == null) {
				Logger.error("Could not find correct type of material (" + wallType.getMaterialType() + ") to construct wall out of at " + tileAtLocation);
			} else {
				materialConstructed = itemMaterial;
			}
		} else {
			Logger.error("Could not find item to construct wall out of at " + tileAtLocation);
		}
		SoundAsset soundAsset = completionSoundMapping.get(materialConstructed.getMaterialType());
		if (soundAsset != null) {
			messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(soundAsset, null,
					tileAtLocation.getWorldPositionOfCenter(), null));
		}

		messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(dustCloudParticleEffect,
				Optional.empty(), Optional.of(new JobTarget(tileAtLocation)), (p) -> {}));

		messageDispatcher.dispatchMessage(MessageType.ADD_WALL, new AddWallMessage(tileLocation, materialConstructed, wallType));
	}

	private void handleBridgeConstructionCompleted(BridgeConstruction construction) {

		Map<Integer, MapTile> landTilesByRegion = new TreeMap<>();

		for (Map.Entry<GridPoint2, BridgeTile> bridgeEntry : construction.getBridge().entrySet()) {
			MapTile tile = gameContext.getAreaMap().getTile(bridgeEntry.getKey());
			tile.setConstruction(null);

			for (Entity entity : tile.getEntities()) {
				if (entity.getType().equals(EntityType.ITEM)) {
					ItemEntityAttributes itemAttributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
					if (itemAttributes.getItemType().equals(construction.getBridge().getBridgeType().getBuildingRequirement().getItemType())) {
						construction.getBridge().setMaterial(itemAttributes.getMaterial(construction.getPrimaryMaterialType()));
					}
					messageDispatcher.dispatchMessage(0.01f, MessageType.DESTROY_ENTITY, entity);
				}
			}

			messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(dustCloudParticleEffect,
					Optional.empty(), Optional.of(new JobTarget(tile)), (p) -> {}));

			tile.getFloor().setBridgeTile(construction.getBridge(), bridgeEntry.getValue());

			if (!tile.getFloor().isRiverTile()) {
				landTilesByRegion.put(tile.getRegionId(), tile);
			}
		}

		List<Integer> landRegionIdList = new ArrayList<>(landTilesByRegion.keySet());
		int firstRegionId = landRegionIdList.get(0);
		for (int cursor = 1; cursor < landRegionIdList.size(); cursor++) {
			int otherRegionId = landRegionIdList.get(cursor);
			MapTile otherRegionTile = landTilesByRegion.get(otherRegionId);

			messageDispatcher.dispatchMessage(MessageType.REPLACE_REGION, new ReplaceRegionMessage(otherRegionTile, firstRegionId));
		}

		for (GridPoint2 bridgeLocation : construction.getBridge().getLocations()) {
			MapTile tile = gameContext.getAreaMap().getTile(bridgeLocation);
			if (tile.getFloor().isBridgeNavigable()) {
				tile.setRegionId(firstRegionId);
			}
		}

		SoundAsset soundAsset = completionSoundMapping.get(construction.getPrimaryMaterialType());
		if (soundAsset != null) {
			messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(soundAsset, null, construction.getBridge().getAvgWorldPosition(), null));
		}

		constructionStore.remove(construction);
	}

	private boolean handle(TransformConstructionMessage transformConstructionMessage) {
		FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) transformConstructionMessage.construction.getEntity().getPhysicalEntityComponent().getAttributes();

		FurnitureLayout originalLayout = attributes.getCurrentLayout();
		attributes.setFurnitureType(transformConstructionMessage.transformToFurnitureType);
		if (!attributes.getCurrentLayout().equals(originalLayout)) {
			for (int i = 0; i < 4; i++) {
				attributes.setCurrentLayout(attributes.getCurrentLayout().getRotatesTo());
				if (attributes.getCurrentLayout().equals(originalLayout)) {
					break;
				}
			}
		}

		if (!transformConstructionMessage.transformToFurnitureType.getRequirements().containsKey(attributes.getPrimaryMaterialType())) {
			for (GameMaterialType materialType : transformConstructionMessage.transformToFurnitureType.getRequirements().keySet()) {
				if (attributes.getMaterials().get(materialType) != null) {
					attributes.setPrimaryMaterialType(materialType);
					break;
				}
			}
		}

		transformConstructionMessage.construction.requirements.clear();
		List<QuantifiedItemTypeWithMaterial> newRequirements = convert(transformConstructionMessage.transformToFurnitureType.getRequirements().get(attributes.getPrimaryMaterialType()));
		transformConstructionMessage.construction.requirements.addAll(newRequirements);

		for (HaulingAllocation allocation : new ArrayList<>(transformConstructionMessage.construction.getIncomingHaulingAllocations())) {
			messageDispatcher.dispatchMessage(MessageType.HAULING_ALLOCATION_CANCELLED, allocation);
		}


		return true;
	}

	/**
	 * This method picks out the majority material for the supplied type in the furniture's construction
	 */
	public static GameMaterial findApplicableMaterial(Collection<Entity> itemsUsed, GameMaterialType gameMaterialType) {
		Map<GameMaterial, Integer> materialCounter = new HashMap<>();

		for (Entity itemEntity : itemsUsed) {
			ItemEntityAttributes attributes = (ItemEntityAttributes)itemEntity.getPhysicalEntityComponent().getAttributes();
			GameMaterial matchingMaterial = attributes.getMaterial(gameMaterialType);
			if (matchingMaterial != null) {
				Integer quantitySoFar = materialCounter.get(matchingMaterial);
				if (quantitySoFar == null) {
					quantitySoFar = 0;
				}
				quantitySoFar += attributes.getQuantity();
				materialCounter.put(matchingMaterial, quantitySoFar);
			}
		}

		int highestCount = 0;
		GameMaterial majorityMaterial = GameMaterial.nullMaterialWithType(GameMaterialType.EARTH); // FIXME maybe infer from EXTRA MATERIALS / MATERIAL_TYPES tag

		for (Map.Entry<GameMaterial, Integer> entry : materialCounter.entrySet()) {
			if (entry.getValue() > highestCount) {
				highestCount = entry.getValue();
				majorityMaterial = entry.getKey();
			}
		}

		return majorityMaterial;
	}

	private void requestConstructionSoundAsset(Construction construction) {
		SoundAsset soundAsset = completionSoundMapping.get(construction.getPrimaryMaterialType());
		if (soundAsset != null) {
			messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(soundAsset, construction.getId(),
					toVector(construction.getPrimaryLocation()), null));
		}
	}

	private void deconstructBridge(Bridge bridgeToRemove) {
		Set<GridPoint2> assignedToGroup = new HashSet<>();
		List<List<MapTile>> tilesGroupedByGroundType = new ArrayList<>();

		Set<GridPoint2> bridgeLocations = bridgeToRemove.getLocations();
		for (GridPoint2 bridgeLocation : bridgeLocations) {
			MapTile bridgeTile = gameContext.getAreaMap().getTile(bridgeLocation);
			bridgeTile.setDesignation(null);
			bridgeTile.getFloor().setBridgeTile(null, null);

			messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(dustCloudParticleEffect,
					Optional.empty(), Optional.of(new JobTarget(bridgeTile)), (p) -> {}));

			if (!assignedToGroup.contains(bridgeLocation)) {
				List<MapTile> tilesInGroup = new ArrayList<>();
				tilesInGroup.add(bridgeTile);
				assignedToGroup.add(bridgeLocation);
				boolean riverGroup = bridgeTile.getFloor().isRiverTile();

				Deque<MapTile> frontier = new ArrayDeque<>();

				for (MapTile neighbourTile : gameContext.getAreaMap().getOrthogonalNeighbours(bridgeLocation.x, bridgeLocation.y).values()) {
					if (!assignedToGroup.contains(neighbourTile.getTilePosition()) && bridgeLocations.contains(neighbourTile.getTilePosition())) {
						frontier.add(neighbourTile);
					}
				}

				while (!frontier.isEmpty()) {
					MapTile frontierTile = frontier.pop();

					boolean matchesGroup;
					if (riverGroup) {
						matchesGroup = frontierTile.getFloor().isRiverTile();
					} else {
						matchesGroup = !frontierTile.getFloor().isRiverTile();
					}

					if (matchesGroup) {
						tilesInGroup.add(frontierTile);
						assignedToGroup.add(frontierTile.getTilePosition());

						for (MapTile frontierNeighbour : gameContext.getAreaMap().getOrthogonalNeighbours(frontierTile.getTilePosition().x, frontierTile.getTilePosition().y).values()) {
							if (!assignedToGroup.contains(frontierNeighbour.getTilePosition()) && bridgeLocations.contains(frontierNeighbour.getTilePosition())) {
								frontier.add(frontierNeighbour);
							}
						}
					}
				}

				tilesGroupedByGroundType.add(tilesInGroup);
			}
		}

		// Should now have 3 or more lists of tiles in different areas

		// First set each group to be a new region
		for (List<MapTile> tileGroup : tilesGroupedByGroundType) {
			int newRegionId = gameContext.getAreaMap().createNewRegionId();
			for (MapTile groupTile : tileGroup) {
				groupTile.setRegionId(newRegionId);
			}
		}

		// For river groups, take on the surrounding region
		// For ground regions, spread to surrounding regions
		// This should deal with the ground regions potentially being split up or not
		for (List<MapTile> tileGroup : tilesGroupedByGroundType) {
			boolean riverGroup = tileGroup.get(0).getFloor().isRiverTile();
			MapTile neighbourRegionTile = pickRandomNeighbourRegionOfSameType(tileGroup);
			if (neighbourRegionTile != null) {
				if (riverGroup) {
					messageDispatcher.dispatchMessage(MessageType.REPLACE_REGION, new ReplaceRegionMessage(tileGroup.get(0), neighbourRegionTile.getRegionId()));
				} else {
					messageDispatcher.dispatchMessage(MessageType.REPLACE_REGION, new ReplaceRegionMessage(neighbourRegionTile, tileGroup.get(0).getRegionId()));
				}
			} else {
				Logger.warn("Could not expand new region after removing bridge");
			}
		}

		// Regions reset, can now remove bridge
		int amountToRefund = bridgeToRemove.getBridgeType().getBuildingRequirement().getQuantity() / 2;
		ItemType refundItemType = bridgeToRemove.getBridgeType().getBuildingRequirement().getItemType();
		int maxPerTile = refundItemType.getMaxStackSize();

		for (GridPoint2 bridgeLocation : bridgeLocations) {
			MapTile bridgeTile = gameContext.getAreaMap().getTile(bridgeLocation);

			if (!bridgeTile.getFloor().isRiverTile() && amountToRefund > 0) {
				int refundThisTile = Math.min(amountToRefund, maxPerTile);
				amountToRefund -= refundThisTile;

				ItemEntityAttributes itemAttributes = itemEntityAttributesFactory.createItemAttributes(refundItemType, refundThisTile, bridgeToRemove.getMaterial());
				itemEntityFactory.create(itemAttributes, bridgeLocation, true, gameContext);
			}
		}

	}

	private MapTile pickRandomNeighbourRegionOfSameType(List<MapTile> tileGroup) {
		for (MapTile tileInGroup : tileGroup) {
			for (MapTile neighbour : gameContext.getAreaMap().getOrthogonalNeighbours(tileInGroup.getTileX(), tileInGroup.getTileY()).values()) {
				if (neighbour.getRegionId() != tileInGroup.getRegionId() && (
						(neighbour.getFloor().isRiverTile() && tileInGroup.getFloor().isRiverTile()) ||
								(!neighbour.getFloor().isRiverTile() && !tileInGroup.getFloor().isRiverTile())
				)) {
					return neighbour;
				}
			}
		}
		return null;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
