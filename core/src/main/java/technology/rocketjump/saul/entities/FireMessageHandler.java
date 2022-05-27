package technology.rocketjump.saul.entities;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.FloorTypeDictionary;
import technology.rocketjump.saul.assets.model.FloorType;
import technology.rocketjump.saul.entities.behaviour.BurnedEntityBehaviour;
import technology.rocketjump.saul.entities.behaviour.creature.CorpseBehaviour;
import technology.rocketjump.saul.entities.behaviour.effects.FireEffectBehaviour;
import technology.rocketjump.saul.entities.components.AttachedEntitiesComponent;
import technology.rocketjump.saul.entities.components.AttachedLightSourceComponent;
import technology.rocketjump.saul.entities.components.LiquidContainerComponent;
import technology.rocketjump.saul.entities.components.humanoid.StatusComponent;
import technology.rocketjump.saul.entities.factories.OngoingEffectAttributesFactory;
import technology.rocketjump.saul.entities.factories.OngoingEffectEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.DeathReason;
import technology.rocketjump.saul.entities.model.physical.creature.status.OnFireStatus;
import technology.rocketjump.saul.entities.model.physical.effect.OngoingEffectAttributes;
import technology.rocketjump.saul.entities.model.physical.furniture.EntityDestructionCause;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.mapping.tile.CompassDirection;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.mapping.tile.designation.Designation;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.*;
import technology.rocketjump.saul.rendering.utils.ColorMixer;
import technology.rocketjump.saul.rendering.utils.HexColors;
import technology.rocketjump.saul.settlement.FurnitureTracker;
import technology.rocketjump.saul.ui.GameInteractionMode;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
import static technology.rocketjump.saul.entities.model.EntityType.FURNITURE;
import static technology.rocketjump.saul.entities.model.EntityType.STATIC_ENTITY_TYPES;
import static technology.rocketjump.saul.messaging.MessageType.*;
import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.saul.misc.VectorUtils.toVector;
import static technology.rocketjump.saul.ui.GameInteractionMode.DEFAULT;

@Singleton
public class FireMessageHandler implements GameContextAware, Telegraph {

	private static final int NUM_DIRECTIONS_TO_SPREAD_FIRE_IN = 3;
	private static final int MAX_DISTANCE_TO_SPREAD_FIRE_IN = 3;
	private final MessageDispatcher messageDispatcher;
	private final FloorType ashFloor;
	private final GameMaterial ashMaterial;
	private final ItemType ashesItemType;
	private final OngoingEffectAttributesFactory ongoingEffectAttributesFactory;
	private final OngoingEffectEntityFactory ongoingEffectEntityFactory;
	private final EntityStore entityStore;
	private final FurnitureTracker furnitureTracker;

	private GameContext gameContext;
	private GameMaterial boneMaterial;

	@Inject
	public FireMessageHandler(MessageDispatcher messageDispatcher, FloorTypeDictionary floorTypeDictionary,
							  GameMaterialDictionary gameMaterialDictionary, OngoingEffectAttributesFactory ongoingEffectAttributesFactory,
							  OngoingEffectEntityFactory ongoingEffectEntityFactory, ItemTypeDictionary itemTypeDictionary,
							  EntityStore entityStore, FurnitureTracker furnitureTracker) {
		this.messageDispatcher = messageDispatcher;
		this.ashFloor = floorTypeDictionary.getByFloorTypeName("ash");
		this.ashMaterial = gameMaterialDictionary.getByName("Ash");
		this.boneMaterial = gameMaterialDictionary.getByName("Bone");
		this.ashesItemType = itemTypeDictionary.getByName("Ashes");
		this.ongoingEffectAttributesFactory = ongoingEffectAttributesFactory;
		this.ongoingEffectEntityFactory = ongoingEffectEntityFactory;
		this.entityStore = entityStore;
		this.furnitureTracker = furnitureTracker;


		messageDispatcher.addListener(this, MessageType.SPREAD_FIRE_FROM_LOCATION);
		messageDispatcher.addListener(this, MessageType.SMALL_FIRE_STARTED);
		messageDispatcher.addListener(this, CONSUME_TILE_BY_FIRE);
		messageDispatcher.addListener(this, MessageType.CONSUME_ENTITY_BY_FIRE);
		messageDispatcher.addListener(this, MessageType.ADD_FIRE_TO_ENTITY);
		messageDispatcher.addListener(this, MessageType.FIRE_REMOVED);
		messageDispatcher.addListener(this, MessageType.START_FIRE_IN_TILE);

	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case SPREAD_FIRE_FROM_LOCATION:
				Vector2 location = (Vector2) msg.extraInfo;
				spreadFireFrom(location, emptySet(), 4, false, MAX_DISTANCE_TO_SPREAD_FIRE_IN);
				return true;
			case SMALL_FIRE_STARTED:
				StartSmallFireMessage message = (StartSmallFireMessage) msg.extraInfo;
				MapTile spreadFromTile = gameContext.getAreaMap().getTile(message.jobLocation);
				if (spreadFromTile != null) {
					Set<Long> entityIdsToIgnore = spreadFromTile.getEntities().stream().map(Entity::getId).collect(Collectors.toSet());
					entityIdsToIgnore.add(message.targetEntityId);
					spreadFireFrom(toVector(message.jobLocation), entityIdsToIgnore, 1, true, 2);
				}
				return true;
			case START_FIRE_IN_TILE:
				MapTile fireTile = (MapTile) msg.extraInfo;
				startFireInTile(fireTile.getWorldPositionOfCenter());
				return true;
			case CONSUME_TILE_BY_FIRE:
				MapTile tile = gameContext.getAreaMap().getTile((Vector2) msg.extraInfo);
				if (tile != null) {
					if (tile.hasWall()) {
						messageDispatcher.dispatchMessage(MessageType.REMOVE_WALL, tile.getTilePosition());
					}
					messageDispatcher.dispatchMessage(MessageType.REPLACE_FLOOR, new ReplaceFloorMessage(tile.getTilePosition(), ashFloor, ashMaterial));
				}
				return true;
			case ADD_FIRE_TO_ENTITY:
				Entity targetEntity = (Entity) msg.extraInfo;
				OngoingEffectAttributes attributes = ongoingEffectAttributesFactory.createByTypeName("Fire");
				Entity fireEntity = ongoingEffectEntityFactory.create(attributes, targetEntity.getLocationComponent().getWorldOrParentPosition(), gameContext);

				AttachedEntitiesComponent attachedEntitiesComponent = targetEntity.getOrCreateComponent(AttachedEntitiesComponent.class);
				attachedEntitiesComponent.init(targetEntity, messageDispatcher, gameContext);
				attachedEntitiesComponent.addAttachedEntity(fireEntity);

				return true;
			case CONSUME_ENTITY_BY_FIRE:
				Entity entity = (Entity) msg.extraInfo;
				consumeEntityByFire(entity);
				return true;
			case FIRE_REMOVED:
				GridPoint2 removalLocation = (GridPoint2) msg.extraInfo;
				if (removalLocation != null) {
					checkToRemoveExtinguishDesignation(removalLocation);
				}
				return true;
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private void startFireInTile(Vector2 location) {
		MapTile targetTile = gameContext.getAreaMap().getTile(location);
		if (targetTile == null) {
			return;
		}

		if (targetTile.hasWall()) {
			if (targetTile.getWall().getMaterial().isCombustible()) {
				createFireInTile(targetTile);
				return;
			}
		} else {
			Optional<Entity> combustibleEntity = targetTile.getEntities().stream()
					.filter(e -> e.getPhysicalEntityComponent().getAttributes()
							.getMaterials().values().stream().anyMatch(GameMaterial::isCombustible))
					.findFirst();

			if (combustibleEntity.isPresent()) {
				StatusComponent statusComponent = combustibleEntity.get().getOrCreateComponent(StatusComponent.class);
				statusComponent.init(combustibleEntity.get(), messageDispatcher, gameContext);
				statusComponent.apply(new OnFireStatus());
				return;
			} else if (targetTile.getFloor().getMaterial().isCombustible()) {
				createFireInTile(targetTile);
				return;
			}
		}
	}


	private void spreadFireFrom(Vector2 location, Set<Long> entityIdsToIgnore, int maxFiresToStart, boolean staticEntitiesOnly, int maxDistanceToSpreadFire) {
		if (location == null) {
			// Surprised this is happening, seems some ongoing effects have a null location
			return;
		}
		GridPoint2 centre = toGridPoint(location);
		int firesStarted = 0;
		MapTile centreTile = gameContext.getAreaMap().getTile(centre);
		Designation extinguishFlamesDesignation = null;
		if (centreTile.getDesignation() != null && centreTile.getDesignation().getDesignationName().equals("EXTINGUISH_FLAMES")) {
			extinguishFlamesDesignation = centreTile.getDesignation();
		}
		if (centreTile == null) {
			return;
		}
		ArrayList<CompassDirection> directions = new ArrayList<>(CompassDirection.DIAGONAL_DIRECTIONS);
		Collections.shuffle(directions, gameContext.getRandom());
		for (CompassDirection direction : directions) {
			MapTile nextTile = centreTile;
			for (int distance = 1; distance <= maxDistanceToSpreadFire; distance++) {
				nextTile = selectNextTile(nextTile, direction);
				if (nextTile == null) {
					break;
				}

				if (nextTile.hasWall()) {
					if (nextTile.getWall().getMaterial().isCombustible()) {
						createFireInTile(nextTile);
						applyDesignation(nextTile, extinguishFlamesDesignation);
						firesStarted++;
					}
					break;
				} else {

					Optional<Entity> combustibleEntity = nextTile.getEntities().stream()
							.filter(e -> staticEntitiesOnly ? STATIC_ENTITY_TYPES.contains(e.getType()) : true)
							.filter(e -> !entityIdsToIgnore.contains(e.getId()))
							.filter(e -> e.getPhysicalEntityComponent().getAttributes()
									.getMaterials().values().stream().anyMatch(GameMaterial::isCombustible))
							.findFirst();

					if (combustibleEntity.isPresent()) {
						StatusComponent statusComponent = combustibleEntity.get().getOrCreateComponent(StatusComponent.class);
						statusComponent.init(combustibleEntity.get(), messageDispatcher, gameContext);
						statusComponent.apply(new OnFireStatus());
						firesStarted++;
						if (STATIC_ENTITY_TYPES.contains(combustibleEntity.get().getType())) {
							applyDesignation(nextTile, extinguishFlamesDesignation);
						}
						break;
					} else if (nextTile.getFloor().getMaterial().isCombustible()) {
						createFireInTile(nextTile);
						applyDesignation(nextTile, extinguishFlamesDesignation);
						firesStarted++;
						break;
					}


				}

			}
			if (firesStarted >= maxFiresToStart) {
				return;
			}
		}
	}

	private void checkToRemoveExtinguishDesignation(GridPoint2 removalLocation) {
		MapTile tile = gameContext.getAreaMap().getTile(removalLocation);
		Designation designation = tile.getDesignation();
		if (designation != null) {
			GameInteractionMode interactionMode = GameInteractionMode.getByDesignationName(designation.getDesignationName());
			if (interactionMode != null) {
				if (!interactionMode.tileDesignationCheck.shouldDesignationApply(tile)) {
					// designation no longer applies
					messageDispatcher.dispatchMessage(REMOVE_DESIGNATION, new RemoveDesignationMessage(tile));

					gameContext.getAreaMap().getTile(removalLocation).getEntities().stream()
							.filter(e -> e.getType().equals(FURNITURE))
							.forEach(
									e -> {
										FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) e.getPhysicalEntityComponent().getAttributes();
										List<GridPoint2> extraTiles = attributes.getCurrentLayout().getExtraTiles();
										for (GridPoint2 extraTileOffset : extraTiles) {
											checkToRemoveExtinguishDesignation(removalLocation.cpy().add(extraTileOffset));
										}
									}
							);
				}
			}
		}
	}

	private void consumeEntityByFire(Entity entity) {
		AttachedLightSourceComponent lightSourceComponent = entity.getComponent(AttachedLightSourceComponent.class);
		if (lightSourceComponent != null) {
			lightSourceComponent.setEnabled(false);
		}

		LiquidContainerComponent liquidContainerComponent = entity.getComponent(LiquidContainerComponent.class);
		if (liquidContainerComponent != null) {
			// TODO explode with flammable contents or something
			liquidContainerComponent.setLiquidQuantity(0f);
		}

		switch (entity.getType()) {
			case CREATURE:
				messageDispatcher.dispatchMessage(MessageType.CREATURE_DEATH, new CreatureDeathMessage(entity, DeathReason.BURNING));
				if (entity.getBehaviourComponent() instanceof CorpseBehaviour) {
					CreatureEntityAttributes attributes = (CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
					CorpseBehaviour corpseBehaviour = (CorpseBehaviour) entity.getBehaviourComponent();
					corpseBehaviour.setToFullyDecayed(attributes);
					attributes.setBoneColor(blackenedColor(gameContext.getRandom()));
				}
				break;
			case ITEM:
				ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				attributes.setMaterial(ashMaterial);
				messageDispatcher.dispatchMessage(TRANSFORM_ITEM_TYPE, new TransformItemMessage(entity, ashesItemType));
				attributes.getMaterials().clear();
				attributes.setMaterial(ashMaterial);
				attributes.setDestroyed(EntityDestructionCause.BURNED);
				break;
			case PLANT:
				PlantEntityAttributes plantEntityAttributes = (PlantEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
				plantEntityAttributes.setBurned(ashMaterial, blackenedColor(gameContext.getRandom()));
				entityStore.changeBehaviour(entity, new BurnedEntityBehaviour(), messageDispatcher);
				messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, entity);
				break;
			case FURNITURE:
				Color blackenedColor = blackenedColor(gameContext.getRandom());
				messageDispatcher.dispatchMessage(MessageType.DAMAGE_FURNITURE, new FurnitureDamagedMessage(
						entity, EntityDestructionCause.BURNED, ashMaterial, blackenedColor, blackenedColor
				));
				break;
			default:
				Logger.error("Not yet implemented: Consuming entity of type " + entity.getType() + " by fire");
		}

		entity.setTags(emptySet());
	}

	/**
	 * This method is used to pick either the diagonal direction or one of the 2 adjacent orthogonal directions
	 */
	private MapTile selectNextTile(MapTile tile, CompassDirection diagonalDirection) {
		int xOffset = diagonalDirection.getXOffset();
		int yOffset = diagonalDirection.getYOffset();
		float roll = gameContext.getRandom().nextFloat();
		if (roll < 0.33f) {
			xOffset = 0;
		} else if (roll < 0.66f) {
			yOffset = 0;
		}

		return gameContext.getAreaMap().getTile(
				tile.getTileX() + xOffset,
				tile.getTileY() + yOffset
		);
	}

	private void createFireInTile(MapTile targetTile) {
		if (!targetTile.getEntities().stream().anyMatch(e -> e.getBehaviourComponent() instanceof FireEffectBehaviour)) {
			OngoingEffectAttributes attributes = ongoingEffectAttributesFactory.createByTypeName("Fire");
			ongoingEffectEntityFactory.create(attributes, targetTile.getWorldPositionOfCenter(), gameContext);
		}
	}

	private static final Array<Color> blackenedColors = new Array<>();
	static {
		blackenedColors.add(HexColors.get("#605f5f"));
		blackenedColors.add(HexColors.get("#45403e"));
		blackenedColors.add(HexColors.get("#343231"));
	}

	public static Color blackenedColor(Random random) {
		return ColorMixer.randomBlend(random, blackenedColors);
	}

	private void applyDesignation(MapTile targetTile, Designation extinguishFlamesDesignation) {
		if (extinguishFlamesDesignation != null) {
			if (targetTile.getDesignation() != null) {
				messageDispatcher.dispatchMessage(MessageType.REMOVE_DESIGNATION, new RemoveDesignationMessage(targetTile));
			}
			targetTile.setDesignation(extinguishFlamesDesignation);
			messageDispatcher.dispatchMessage(MessageType.DESIGNATION_APPLIED, new ApplyDesignationMessage(targetTile, extinguishFlamesDesignation, DEFAULT));
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
