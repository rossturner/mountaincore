package technology.rocketjump.saul.mapping;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.GridPoint2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.assets.WallTypeDictionary;
import technology.rocketjump.saul.assets.model.WallType;
import technology.rocketjump.saul.entities.ai.pathfinding.Map2DCollection;
import technology.rocketjump.saul.entities.components.Faction;
import technology.rocketjump.saul.entities.factories.ItemEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.creature.DeathReason;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.entities.tags.SupportsRoofTag;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.Updatable;
import technology.rocketjump.saul.jobs.model.JobTarget;
import technology.rocketjump.saul.mapping.model.ImpendingMiningCollapse;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.mapping.tile.roof.TileRoofState;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.AddWallMessage;
import technology.rocketjump.saul.messaging.types.CreatureDeathMessage;
import technology.rocketjump.saul.messaging.types.ParticleRequestMessage;
import technology.rocketjump.saul.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.saul.particles.model.ParticleEffectType;
import technology.rocketjump.saul.rooms.constructions.Construction;
import technology.rocketjump.saul.settlement.notifications.Notification;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;

import static technology.rocketjump.saul.misc.VectorUtils.toVector;
import static technology.rocketjump.saul.settlement.notifications.NotificationType.MINING_COLLAPSE;

@Singleton
public class MiningCollapseManager implements Telegraph, Updatable {

	private static final int UNSUPPORTED_MINING_COLLAPSE_WIDTH = 7; // Max number of tiles in both directions before a cave-in will occur
	private static final double MAX_HOURS_BEFORE_COLLAPSE = 6.0;
	private static final double MIN_HOURS_BEFORE_COLLAPSE = 0.2;

	public static final float TIME_BETWEEN_UPDATE_SECONDS = 3.14159f;
	private final MessageDispatcher messageDispatcher;
	private final WallType roughStoneWallType;
	private final ItemType droppedStoneItemType;
	private final ItemEntityFactory itemEntityFactory;
	private final ParticleEffectType wallRemovedParticleEffectType;
	private float timeSinceLastUpdate = 0f;

	private GameContext gameContext;

	@Inject
	public MiningCollapseManager(MessageDispatcher messageDispatcher, WallTypeDictionary wallTypeDictionary,
								 ItemTypeDictionary itemTypeDictionary, ItemEntityFactory itemEntityFactory,
								 ParticleEffectTypeDictionary particleEffectTypeDictionary) {
		this.messageDispatcher = messageDispatcher;

		roughStoneWallType = wallTypeDictionary.getByWallTypeName("rough_stone_wall");
		droppedStoneItemType = itemTypeDictionary.getByName("Resource-Stone-Unrefined");
		this.wallRemovedParticleEffectType = particleEffectTypeDictionary.getByName("Dust cloud"); // MODDING expose this
		this.itemEntityFactory = itemEntityFactory;

		messageDispatcher.addListener(this, MessageType.WALL_REMOVED);
		messageDispatcher.addListener(this, MessageType.ROOF_SUPPORT_REMOVED);
	}


	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.ROOF_SUPPORT_REMOVED:
			case MessageType.WALL_REMOVED: {
				boolean collapseImmediately = msg.message == MessageType.ROOF_SUPPORT_REMOVED;
				GridPoint2 location = (GridPoint2) msg.extraInfo;
				checkForMiningCollapse(location, collapseImmediately);
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	@Override
	public void update(float deltaTime) {
		timeSinceLastUpdate += deltaTime;
		if (timeSinceLastUpdate > TIME_BETWEEN_UPDATE_SECONDS) {
			timeSinceLastUpdate = 0f;
			doUpdate();
		}
	}

	private void doUpdate() {
		if (gameContext == null || gameContext.getSettlementState().impendingMiningCollapses.isEmpty()) {
			return;
		}
		Iterator<ImpendingMiningCollapse> iterator = gameContext.getSettlementState().impendingMiningCollapses.iterator();
		while (iterator.hasNext()) {
			ImpendingMiningCollapse impendingMiningCollapse = iterator.next();
			if (impendingMiningCollapse.getCollapseGameTime() < gameContext.getGameClock().getCurrentGameTime()) {
				if (checkCollapseStillAppliesAround(impendingMiningCollapse.getEpicenter())) {
					triggerCollapse(impendingMiningCollapse.getEpicenter());
				} else {
					// No longer going to collapse
				}
				iterator.remove();
			}
		}
	}

	@Override
	public boolean runWhilePaused() {
		return false;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}

	private void checkForMiningCollapse(GridPoint2 initialLocation, boolean collapseImmediately) {
		GridPoint2 collapseEpicenter = findCollapseEpicenterAround(initialLocation);

		if (collapseEpicenter != null) {
			double collapseGameTime = gameContext.getGameClock().getCurrentGameTime();
			if (!collapseImmediately) {
				double delayInHours = MIN_HOURS_BEFORE_COLLAPSE + (gameContext.getRandom().nextDouble() * (MAX_HOURS_BEFORE_COLLAPSE - MIN_HOURS_BEFORE_COLLAPSE));
				collapseGameTime += delayInHours;
			}

			gameContext.getSettlementState().impendingMiningCollapses.add(
					new ImpendingMiningCollapse(collapseEpicenter, collapseGameTime)
			);
		}
	}

	private boolean checkCollapseStillAppliesAround(GridPoint2 epicenter) {
		return findCollapseEpicenterAround(epicenter) != null;
	}

	private boolean roofSupportedBy(MapTile tiletoCheck) {
		if (tiletoCheck == null) {
			return true;
		} else if (tiletoCheck.hasWall()) {
			return true;
		} else if (tiletoCheck.getRoof().getState().equals(TileRoofState.MINED)) {
			for (Entity entity : tiletoCheck.getEntities()) {
				SupportsRoofTag supportsRoofTag = entity.getTag(SupportsRoofTag.class);
				if (supportsRoofTag != null) {
					return true;
				}
			}
			// No entities support roof and roof type is mined
			return false;
		} else {
			// All other roof types protect against cave-in
			return true;
		}
	}

	private GridPoint2 findCollapseEpicenterAround(GridPoint2 initialLocation) {
		Map2DCollection<Boolean> supportData = new Map2DCollection<>(gameContext.getAreaMap().getWidth());

		for (int y = initialLocation.y - UNSUPPORTED_MINING_COLLAPSE_WIDTH + 1; y < initialLocation.y + UNSUPPORTED_MINING_COLLAPSE_WIDTH; y++) {
			for (int x = initialLocation.x - UNSUPPORTED_MINING_COLLAPSE_WIDTH + 1; x < initialLocation.x + UNSUPPORTED_MINING_COLLAPSE_WIDTH; x++) {
				MapTile tileToCheck = gameContext.getAreaMap().getTile(x, y);
				boolean supportsRoof = roofSupportedBy(tileToCheck);
				supportData.add(x, y, supportsRoof);
			}
		}

		GridPoint2 startingPoint = new GridPoint2(initialLocation.x - UNSUPPORTED_MINING_COLLAPSE_WIDTH + 1, initialLocation.y - UNSUPPORTED_MINING_COLLAPSE_WIDTH + 1);
		// Now try to find a contiguous 7x7 area of no supports (false values)
		while (startingPoint.y <= initialLocation.y) {
			startingPoint.x = initialLocation.x - UNSUPPORTED_MINING_COLLAPSE_WIDTH + 1;
			while (startingPoint.x <= initialLocation.x) {

				boolean supportFound = false;
				for (int yOffset = 0; yOffset < UNSUPPORTED_MINING_COLLAPSE_WIDTH; yOffset++) {
					for (int xOffset = 0; xOffset < UNSUPPORTED_MINING_COLLAPSE_WIDTH; xOffset++) {
						supportFound = supportData.get(startingPoint.x + xOffset, startingPoint.y + yOffset);
						if (supportFound) {
							break;
						}
					}
					if (supportFound) {
						break;
					}
				}

				if (!supportFound) {
					return new GridPoint2(startingPoint.x + (UNSUPPORTED_MINING_COLLAPSE_WIDTH / 2), startingPoint.y + (UNSUPPORTED_MINING_COLLAPSE_WIDTH / 2));
				}
				startingPoint.x++;
			}
			startingPoint.y++;
		}
		return null;
	}

	private void triggerCollapse(GridPoint2 epicenter) {
		messageDispatcher.dispatchMessage(MessageType.TRIGGER_SCREEN_SHAKE);
		Notification notification = new Notification(MINING_COLLAPSE, toVector(epicenter), null);
		messageDispatcher.dispatchMessage(MessageType.POST_NOTIFICATION, notification);
		// TODO null-location sound for collapse (so it can be heard anywhere on map)

		// Randomly fill in rough walls and boulders in area of collapse
		Set<Entity> entitiesStruckByCollapse = new HashSet<>();
		Set<Construction> constructionsStruckByCollapse = new HashSet<>();
		Set<GridPoint2> roomTilesToRemove = new HashSet<>();
		for (int y = epicenter.y - (UNSUPPORTED_MINING_COLLAPSE_WIDTH / 2); y < epicenter.y + (UNSUPPORTED_MINING_COLLAPSE_WIDTH / 2); y++) {
			for (int x = epicenter.x - (UNSUPPORTED_MINING_COLLAPSE_WIDTH / 2); x < epicenter.x + (UNSUPPORTED_MINING_COLLAPSE_WIDTH / 2); x++) {
				MapTile tile = gameContext.getAreaMap().getTile(x, y);
				if (tile == null) {
					continue;
				}

				messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(wallRemovedParticleEffectType,
						Optional.empty(), Optional.of(new JobTarget(tile)), (p) -> {}));

				if (gameContext.getRandom().nextBoolean()) {
					// add wall
					if (tile.getRoof().getRoofMaterial() != null) {
						entitiesStruckByCollapse.addAll(tile.getEntities());
						if (tile.hasConstruction()) {
							constructionsStruckByCollapse.add(tile.getConstruction());
						}
						AddWallMessage addWallMessage = new AddWallMessage(tile.getTilePosition(), tile.getRoof().getRoofMaterial(), roughStoneWallType);
						messageDispatcher.dispatchMessage(MessageType.ADD_WALL, addWallMessage);
						if (tile.hasRoom()) {
							roomTilesToRemove.add(tile.getTilePosition());
						}
					}
				} else if (gameContext.getRandom().nextBoolean()) {
					// drop boulder
					if (tile.getRoof().getRoofMaterial() != null) {
						entitiesStruckByCollapse.addAll(tile.getEntities());

						ItemEntityAttributes attributes = new ItemEntityAttributes(gameContext.getRandom().nextLong());
						attributes.setItemType(droppedStoneItemType);
						attributes.setMaterial(tile.getRoof().getRoofMaterial());
						attributes.setQuantity(1);

						itemEntityFactory.create(attributes, tile.getTilePosition(), true, gameContext, Faction.SETTLEMENT);
					}
				} else {
					// Do nothing
				}
			}
		}

		if (!roomTilesToRemove.isEmpty()) {
			messageDispatcher.dispatchMessage(MessageType.REMOVE_ROOM_TILES, roomTilesToRemove);
		}

		for (Entity entity : entitiesStruckByCollapse) {
			if (entity.getType().equals(EntityType.CREATURE)) {
				messageDispatcher.dispatchMessage(MessageType.CREATURE_DEATH, new CreatureDeathMessage(entity, DeathReason.CRUSHED_BY_FALLING_DEBRIS, null));
				MapTile deceasedTile = gameContext.getAreaMap().getTile(entity.getLocationComponent().getWorldOrParentPosition());
				if (deceasedTile == null || deceasedTile.hasWall()) {
					messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, entity);
				}
			} else {
				messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, entity);
			}
		}
		for (Construction construction : constructionsStruckByCollapse) {
			messageDispatcher.dispatchMessage(MessageType.CANCEL_CONSTRUCTION, construction);
		}

	}

}
