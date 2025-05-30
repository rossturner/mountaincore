package technology.rocketjump.mountaincore.entities.components.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.assets.entities.furniture.model.DoorState;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.mountaincore.mapping.model.TiledMap;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.mapping.tile.NullMapTile;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.persistence.JSONUtils;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.ChildPersistable;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.mountaincore.entities.model.physical.creature.Consciousness.AWAKE;

public class SteeringComponent implements ChildPersistable {

	private static final float ROTATION_MULTIPLIER = 1.5f; // for quicker turning speed
	private static final float KNOCKBACK_DISTANCE_PER_SECOND = 8f;
	private static final float MAX_DISTANCE_WITHIN_TILE_TO_ARRIVE = 0.08f;
	private MessageDispatcher messageDispatcher;
	private Entity parentEntity;
	private TiledMap areaMap;

	private Vector2 destination;
	private Vector2 nextWaypoint;

	private float timeSinceLastPauseCheck = 0;
	private static float TIME_BETWEEN_PAUSE_CHECKS = 1.2f;
	private float pauseTime = 0;
	private static float DEFAULT_PAUSE_TIME = 0.9f;
	private boolean isSlowed;
	private boolean movementImpaired;
	private boolean immobilised;

	public SteeringComponent() {

	}

	public void init(Entity parentEntity, TiledMap map, MessageDispatcher messageDispatcher) {
		this.parentEntity = parentEntity;
		this.areaMap = map;
		this.messageDispatcher = messageDispatcher;
		// Somewhat randomise when this first occurs
		this.timeSinceLastPauseCheck = -((float)(parentEntity.getId() % 2400L))/1000f;
	}

	public void destinationReached() {
		this.nextWaypoint = null;
		this.destination = null;
	}

	public void update(float deltaTime) {
		Entity vehicle = parentEntity.getContainingVehicle();
		if (vehicle != null && !parentEntity.isDrivingVehicle()) {
			return;
		}

		Vector2 steeringOutputForce = new Vector2();
		// Get current position and vector to target destination
		Vector2 currentPosition = parentEntity.getOwnOrVehicleLocationComponent().getWorldPosition();
		Vector2 currentVelocity = parentEntity.getOwnOrVehicleLocationComponent().getLinearVelocity();

		if (currentPosition == null) {
			Logger.error("Attempting to update null position in " + this.getClass().getSimpleName());
		} else {
			boolean updateFacing = true;

			if (nextWaypoint == null) {
				updateFacing = false;
				if (currentVelocity.len2() > 0.5f) {
					currentVelocity.mulAdd(currentVelocity.cpy().scl(-3f), deltaTime);
				} else {
					currentVelocity.setZero();
				}
			} else {
				MapTile nextTile = areaMap.getTile(nextWaypoint);
				boolean waitingForDoorToOpen = false;
				if (nextTile.hasDoorway()) {
					DoorState doorState = nextTile.getDoorway().getDoorState();
					if (!doorState.equals(DoorState.OPEN)) {
						messageDispatcher.dispatchMessage(MessageType.REQUEST_DOOR_OPEN, nextTile.getDoorway().getDoorEntity());
						waitingForDoorToOpen = true;
					}
				}

				Vector2 nextWaypointRelativeNormalised = nextWaypoint.cpy().sub(currentPosition).nor();
				if (!waitingForDoorToOpen) {
					if (nextWaypoint.equals(destination)) {
						// approach rather than full steam ahead
						// just directly approach middle
						currentVelocity.set(nextWaypointRelativeNormalised.cpy().scl(currentVelocity.len()));
						steeringOutputForce.add(nextWaypointRelativeNormalised.scl(1.5f));
					} else {
						steeringOutputForce.add(nextWaypointRelativeNormalised.scl(3f));
					}

				}
				rotateFacingAndApplyVelocity(deltaTime, currentVelocity, nextWaypointRelativeNormalised);
			}

			float maxSpeed = parentEntity.getOwnOrVehicleLocationComponent().getMaxLinearSpeed();
			isSlowed = false;
			Vector2 entityAvoidanceForce = new Vector2();
			Vector2 wallAvoidanceForce = new Vector2();
			// If we're colliding with another entity, slow down
			MapTile currentTile = areaMap.getTile(currentPosition);
			for (MapTile tileNearPosition : areaMap.getNearestTiles(currentPosition)) {
				// If it's a wall, only repel if we're not in it
				if ((tileNearPosition.hasWall() && !tileNearPosition.equals(currentTile)) || tileNearPosition instanceof NullMapTile) {
					Vector2 wallToEntity = currentPosition.cpy().sub(tileNearPosition.getWorldPositionOfCenter());
					if (wallToEntity.len2() < 0.5f) {
						wallAvoidanceForce.add(wallToEntity.nor());
					}
				}

				int repulsionBodyCount = 0;
				for (Entity otherEntity : tileNearPosition.getEntities()) {
					if (otherEntity.getId() != parentEntity.getId() && otherEntity.getType().equals(EntityType.CREATURE)) {
						if (!AWAKE.equals(((CreatureEntityAttributes) otherEntity.getPhysicalEntityComponent().getAttributes()).getConsciousness())) {
							continue;
						}
						Vector2 separation = currentPosition.cpy().sub(otherEntity.getOwnOrVehicleLocationComponent().getWorldPosition());
						float totalRadii = parentEntity.getOwnOrVehicleLocationComponent().getRadius() + otherEntity.getOwnOrVehicleLocationComponent().getRadius();
						float separationDistance = separation.len();
						if (separationDistance < totalRadii) {
							// Overlapping
							isSlowed = true;
						}
						if (separationDistance < totalRadii + parentEntity.getOwnOrVehicleLocationComponent().getRadius() && repulsionBodyCount < 2) {
							entityAvoidanceForce.add(separation.nor());
						}
						repulsionBodyCount++;
					}
				}
			}

			if (currentTile != null) {
				if (currentTile.getFloor().isRiverTile() && !currentTile.getFloor().hasBridge()) {
					maxSpeed *= 2;
					steeringOutputForce.add(currentTile.getFloor().getRiverTile().getFlowDirection().cpy().scl(20f));
				}

				maxSpeed *= currentTile.getFloor().getFloorType().getSpeedModifier();

				if (!currentTile.hasWall()) {
					steeringOutputForce.add(wallAvoidanceForce.limit(2f));
				}
			}

			steeringOutputForce.add(entityAvoidanceForce.limit(1f));

			if (isSlowed || movementImpaired) {
				maxSpeed *= 0.5f;
			}

			if (parentEntity.isOnFire()) {
				maxSpeed *= 2f;
			}

			if (immobilised) {
				maxSpeed = 0.01f;
			}

			if (pauseTime > 0) {
				pauseTime -= deltaTime;
				maxSpeed *= 0.4f;
				timeSinceLastPauseCheck = 0f;
			} else {
				timeSinceLastPauseCheck += deltaTime;
				if (timeSinceLastPauseCheck > TIME_BETWEEN_PAUSE_CHECKS) {
					timeSinceLastPauseCheck = 0f;
					checkToPauseForOtherEntities();
				}
			}
			Vector2 newVelocity = currentVelocity.cpy().mulAdd(steeringOutputForce, deltaTime).limit(maxSpeed);
			if (vehicle != null) {
				float animationProgress = vehicle.getPhysicalEntityComponent().getAnimationProgress();
				animationProgress += newVelocity.len() * deltaTime;
				while (animationProgress > 1f) {
					animationProgress -= 1f;
				}
				vehicle.getPhysicalEntityComponent().setAnimationProgress(animationProgress);
			}
			Vector2 newPosition = currentPosition.cpy().mulAdd(newVelocity, deltaTime);

			parentEntity.getOwnOrVehicleLocationComponent().setLinearVelocity(newVelocity);
			parentEntity.getOwnOrVehicleLocationComponent().setWorldPosition(newPosition, updateFacing);

			if (nextWaypoint != null && nextWaypoint.equals(destination)) {

				if (parentEntity.getOwnOrVehicleLocationComponent().getWorldPosition().dst(destination) < MAX_DISTANCE_WITHIN_TILE_TO_ARRIVE) {
					nextWaypoint = null;
					destination = null;
				}
			}

			// TODO Adjust position for nudges by other entities


			if (currentTile != null && !currentTile.hasWall()) {
				repelFromImpassableCollisions(deltaTime, currentTile);
			}
		}
	}

	private void rotateFacingAndApplyVelocity(float deltaTime, Vector2 currentVelocity, Vector2 target) {
		float angleToWaypoint = target.angleDeg();
		float angleToVelocity = currentVelocity.angleDeg();
		float difference = Math.abs(angleToVelocity - angleToWaypoint);
		// Don't try to apply very small rotations
		if (difference > 3f) {
			boolean positiveRotation = angleToVelocity - angleToWaypoint < 0;
			if (difference > 180f) {
				difference = 360f - difference;
				positiveRotation = !positiveRotation;
			}
			if (difference > 120f) {
				difference = 120f;
//				 Relatively large angle, so let's slow down velocity
//				currentVelocity.mulAdd(currentVelocity.cpy().scl(-3f), deltaTime);
			}

			if (!positiveRotation) {
				difference = -difference;
			}
			currentVelocity.rotateDeg(difference * deltaTime * ROTATION_MULTIPLIER);
		}
	}

	public void setDestination(Vector2 destination) {
		this.destination = destination;
	}

	public void setNextWaypoint(Vector2 nextWaypoint) {
		this.nextWaypoint = nextWaypoint;
	}

	private void repelFromImpassableCollisions(float deltaTime, MapTile currentTile) {
		Vector2 currentPosition = parentEntity.getOwnOrVehicleLocationComponent().getWorldPosition().cpy();
		Vector2 adjustmentForce = new Vector2();
		for (MapTile tileNearNewPosition : areaMap.getNearestTiles(currentPosition)) {
			if (!tileNearNewPosition.isNavigable(parentEntity, currentTile) && !tileNearNewPosition.equals(currentTile)) {
				// if overlapping wall
				Vector2 wallToPosition = currentPosition.cpy().sub(tileNearNewPosition.getWorldPositionOfCenter());

				if (Math.abs(wallToPosition.x) < 0.5f + parentEntity.getOwnOrVehicleLocationComponent().getRadius() &&
						Math.abs(wallToPosition.y) < 0.5f + parentEntity.getOwnOrVehicleLocationComponent().getRadius()) {
					// We are overlapping the wall
					adjustmentForce.add(wallToPosition.nor());
				}

			}
		}
		// Each force is a 1 tile/second speed, could do with being proportional to nearness of wall
		currentPosition.mulAdd(adjustmentForce, deltaTime);
		parentEntity.getOwnOrVehicleLocationComponent().setWorldPosition(currentPosition, false);
	}

	/**
	 * This checks to see if other moving entities are in front, and if so and moving in same direction, slow down a bit
	 */
	public void checkToPauseForOtherEntities() {
		if (pauseTime <= 0) {
			boolean forceBreak = false;
			Vector2 currentPosition = parentEntity.getOwnOrVehicleLocationComponent().getWorldPosition();
			MapTile currentTile = areaMap.getTile(currentPosition);
			for (MapTile tileNearPosition : areaMap.getNearestTiles(currentPosition)) {
				if (pauseTime > 0) {
					break;
				}

				for (Entity otherEntity : tileNearPosition.getEntities()) {
					if (otherEntity.getId() != parentEntity.getId() && otherEntity.getType().equals(EntityType.CREATURE)) {
						if (!AWAKE.equals(((CreatureEntityAttributes)otherEntity.getPhysicalEntityComponent().getAttributes()).getConsciousness())) {
							continue;
						}

						if (otherEntity.getBehaviourComponent().getSteeringComponent().pauseTime > 0) {
							break;
						}

						Vector2 thisToOther = currentPosition.cpy().sub(otherEntity.getOwnOrVehicleLocationComponent().getWorldPosition());
						float totalRadii = this.parentEntity.getOwnOrVehicleLocationComponent().getRadius() + otherEntity.getOwnOrVehicleLocationComponent().getRadius();
						float separationDistance = thisToOther.len();
						if (separationDistance < totalRadii * 2) {
							// Overlapping

							boolean similarFacing = this.parentEntity.getOwnOrVehicleLocationComponent().getLinearVelocity().cpy().dot(otherEntity.getOwnOrVehicleLocationComponent().getLinearVelocity()) > 0;
							boolean otherEntityInFront = thisToOther.cpy().dot(otherEntity.getOwnOrVehicleLocationComponent().getLinearVelocity()) < 0;
							if (similarFacing && otherEntityInFront) {
								pauseTime = DEFAULT_PAUSE_TIME;
								break;
							}
						}
					}
				}
			}
		}
	}

	public float getPauseTime() {
		return pauseTime;
	}

	public boolean isSlowed() {
		return isSlowed;
	}

	public void setMovementImpaired(boolean movementImpaired) {
		this.movementImpaired = movementImpaired;
	}

	public boolean getMovementImpaired() {
		return movementImpaired;
	}

	public void setImmobilised(boolean immobilised) {
		this.immobilised = immobilised;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (destination != null) {
			asJson.put("destination", JSONUtils.toJSON(destination));
		}
		if (nextWaypoint != null) {
			asJson.put("nextWaypoint", JSONUtils.toJSON(nextWaypoint));
		}
		if (movementImpaired) {
			asJson.put("movementImpaired", true);
		}
		if (immobilised) {
			asJson.put("immobilised", true);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.destination = JSONUtils.vector2(asJson.getJSONObject("destination"));
		this.nextWaypoint = JSONUtils.vector2(asJson.getJSONObject("destination"));
		this.movementImpaired = asJson.getBooleanValue("movementImpaired");
		this.immobilised = asJson.getBooleanValue("immobilised");
	}

}
