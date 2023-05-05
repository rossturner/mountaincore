package technology.rocketjump.mountaincore.entities.ai.goap.actions.location;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.pfa.DefaultGraphPath;
import com.badlogic.gdx.ai.pfa.GraphPath;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import org.apache.commons.lang3.EnumUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.components.creature.SteeringComponent;
import technology.rocketjump.mountaincore.entities.components.furniture.FurnitureStockpileComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.LocationComponent;
import technology.rocketjump.mountaincore.entities.model.physical.creature.HaulingComponent;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.mountaincore.entities.planning.PathfindingCallback;
import technology.rocketjump.mountaincore.entities.planning.PathfindingFlag;
import technology.rocketjump.mountaincore.entities.planning.PathfindingTask;
import technology.rocketjump.mountaincore.environment.GameClock;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.jobs.model.Job;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.PathfindingRequestMessage;
import technology.rocketjump.mountaincore.misc.VectorUtils;
import technology.rocketjump.mountaincore.persistence.JSONUtils;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;

import java.util.ArrayList;
import java.util.List;

public class GoToLocationAction extends Action implements PathfindingCallback {

	public static final float WAYPOINT_TOLERANCE = 0.5f;
	public static final float DESTINATION_TOLERANCE = 0.15f;
	public static final float MAX_TIME_TO_WAIT = 8f;
	public static final int ONE_HOUR = 1;

	protected boolean pathfindingRequested;
	protected GraphPath<Vector2> path;
	private float timeWaitingForPath;
	private int pathCursor = 0;
	private double startOfWaypointGameTime;

	protected Vector2 overrideLocation;
	private transient PathfindingTask pathfindingTask;
	protected List<PathfindingFlag> pathfindingFlags = List.of();

	public GoToLocationAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (!pathfindingRequested) {
			pathfindingRequested = true;
			requestPathfinding(gameContext);
		} else if (path == null) {
			// Waiting for path
			timeWaitingForPath += Gdx.graphics.getDeltaTime(); // unmodified delta time
			if (timeWaitingForPath > MAX_TIME_TO_WAIT) {
				completionType = CompletionType.FAILURE;
			}
		} else {
			// Path found
			followPath(gameContext);

			checkForCompletion(gameContext);

			if (completionType != null) {
				parent.parentEntity.getBehaviourComponent().getSteeringComponent().destinationReached();
			}
		}
	}

	private void requestPathfinding(GameContext gameContext) {
		Vector2 destination = selectDestination(gameContext);
		if (destination == null) {
			// Might have already set completionType to success for some cases e.g. going to own entity inventory
			if (completionType == null) {
				completionType = CompletionType.FAILURE; // Nowhere to navigate to
			}
			return;
		}
		PathfindingRequestMessage pathfindingRequestMessage = new PathfindingRequestMessage(
				parent.parentEntity, parent.parentEntity.getLocationComponent().getWorldOrParentPosition(),
				destination, gameContext.getAreaMap(), this, parent.parentEntity.getId(), pathfindingFlags);

		parent.messageDispatcher.dispatchMessage(MessageType.PATHFINDING_REQUEST, pathfindingRequestMessage);

		startOfWaypointGameTime = gameContext.getGameClock().getCurrentGameTime();
		pathCursor = 0;
		timeWaitingForPath = 0;
		path = null;
	}

	private void followPath(GameContext gameContext) {
		GameClock gameClock = gameContext.getGameClock();
		SteeringComponent steeringComponent = parent.parentEntity.getBehaviourComponent().getSteeringComponent();
		LocationComponent locationComponent = parent.parentEntity.getOwnOrVehicleLocationComponent();

		if (pathCursor >= path.getCount()) {
			// Now out of bounds, this shouldn't happen but was reported in a crash log
			// Perhaps path has been re-assigned when pathfinding cancelled after some timeout?
			completionType = CompletionType.FAILURE;
			return;
		}
		Vector2 destination = path.get(path.getCount() - 1);
		Vector2 nextPathNode = path.get(pathCursor);
		MapTile currentTile = gameContext.getAreaMap().getTile(locationComponent.getWorldOrParentPosition());

		steeringComponent.setDestination(destination);
		steeringComponent.setNextWaypoint(nextPathNode);

		if (Math.abs(gameClock.getCurrentGameTime() - startOfWaypointGameTime) > ONE_HOUR) {
			//consider no progress made and request new path finding
			requestPathfinding(gameContext);
			return;
		}

		MapTile nextTile = gameContext.getAreaMap().getTile(nextPathNode);
		MapTile destinationTile = gameContext.getAreaMap().getTile(destination);
		if (!nextTile.isNavigable(parent.parentEntity, currentTile) || !destinationTile.isNavigable(parent.parentEntity, currentTile)) {
			// The next tile is no longer navigable - a wall or something must have been placed there
			completionType = CompletionType.FAILURE;
			return;
		}

		if (Math.abs(locationComponent.getWorldPosition().x - nextPathNode.x) < WAYPOINT_TOLERANCE &&
				Math.abs(locationComponent.getWorldPosition().y - nextPathNode.y) < WAYPOINT_TOLERANCE) {
			if (pathCursor + 1 < path.getCount()) {
				// Still more nodes to follow
				pathCursor++;
				startOfWaypointGameTime = gameClock.getCurrentGameTime();
			}
		}
	}

	protected void checkForCompletion(GameContext gameContext) {
		if (path == null) {
			return;
		}

		if (path.getCount() == 0) {
			completionType = CompletionType.FAILURE;
		} else {
			Vector2 destination = path.get(path.getCount() - 1);
			Vector2 worldPosition = parent.parentEntity.getLocationComponent().getWorldOrParentPosition();

			boolean arrivedAtDestination = (Math.abs(worldPosition.x - destination.x) < DESTINATION_TOLERANCE &&
					Math.abs(worldPosition.y - destination.y) < DESTINATION_TOLERANCE);
			if (arrivedAtDestination) {
				completionType = CompletionType.SUCCESS;
			}
		}
	}

	protected Vector2 selectDestination(GameContext gameContext) {
		if (overrideLocation != null) {
			return overrideLocation;
		} else if (parent.getAssignedHaulingAllocation() != null) {
			// This is a hauling-type job
			if (currentlyCarryingItemFromAllocation()) {
				return calculatePosition(parent.getAssignedHaulingAllocation().getTargetPositionType(),
						parent.getAssignedHaulingAllocation().getTargetPosition(),
						parent.getAssignedHaulingAllocation().getTargetId(), gameContext); // FIXME this should be targetContainerId rather than targetId, but its not yet implemented yet
			} else if (parent.getAssignedHaulingAllocation().getSourcePosition() != null) {
				if (parent.getAssignedHaulingAllocation().getHauledEntityType().equals(EntityType.FURNITURE)) {
					return calculatePosition(parent.getAssignedHaulingAllocation().getSourcePositionType(),
							parent.getAssignedHaulingAllocation().getSourcePosition(),
							parent.getAssignedHaulingAllocation().getHauledEntityId(), gameContext);
				} else {
					return calculatePosition(parent.getAssignedHaulingAllocation().getSourcePositionType(),
							parent.getAssignedHaulingAllocation().getSourcePosition(),
							parent.getAssignedHaulingAllocation().getSourceContainerId(), gameContext);
				}
			} else {
				// FIXME Is job always non-null here?
				return getJobLocation(gameContext);
			}
		} else if (parent.getAssignedJob() != null) {
			return getJobLocation(gameContext);
		} else if (parent.getAssignedFurnitureId() != null) {
			// Need to go to a workspace of assigned furniture
			Entity assignedFurniture = gameContext.getEntities().get(parent.getAssignedFurnitureId());
			if (assignedFurniture == null) {
				Logger.error("Could not find assigned furniture by ID " + parent.getAssignedFurnitureId());
				return null;
			}

			FurnitureEntityAttributes attributes = (FurnitureEntityAttributes)assignedFurniture.getPhysicalEntityComponent().getAttributes();
			if (attributes.getFurnitureType().isBlocksMovement()) {
				// Blocks movement
				if (attributes.getCurrentLayout().getWorkspaces().isEmpty()) {
					// No workspaces
					Logger.error("Not yet implemented: Going to adjacent tile to furniture");
					return null;
				} else {
					// Has workspaces
					FurnitureLayout.Workspace navigableWorkspace = FurnitureLayout.getNearestNavigableWorkspace(assignedFurniture, gameContext.getAreaMap(), VectorUtils.toGridPoint(parent.parentEntity.getLocationComponent().getWorldOrParentPosition()));
					if (navigableWorkspace == null) {
						// Could not navigate to any workspaces
						return null;
					} else {
						return VectorUtils.toVector(navigableWorkspace.getAccessedFrom());
					}
				}
			} else {
				// Does not block movement
				return assignedFurniture.getLocationComponent().getWorldOrParentPosition();
			}

		} else {
			return null;
		}
	}

	public void setOverrideLocation(Vector2 overrideLocation) {
		this.overrideLocation = overrideLocation;
	}

	protected Vector2 calculatePosition(HaulingAllocation.AllocationPositionType allocationPositionType,
										GridPoint2 allocationPosition, Long targetEntityId, GameContext gameContext) {
		if (HaulingAllocation.AllocationPositionType.FURNITURE.equals(allocationPositionType)) {
			Entity targetFurniture = gameContext.getAreaMap().getTile(allocationPosition)
					.getEntity(targetEntityId);
			if (targetFurniture != null) {
				FurnitureEntityAttributes furnitureEntityAttributes = (FurnitureEntityAttributes)targetFurniture.getPhysicalEntityComponent().getAttributes();
				FurnitureLayout.Workspace navigableWorkspace = FurnitureLayout.getAnyNavigableWorkspace(targetFurniture, gameContext.getAreaMap());
				FurnitureStockpileComponent stockpileComponent = targetFurniture.getComponent(FurnitureStockpileComponent.class);
				if (navigableWorkspace != null) {
					return VectorUtils.toVector(navigableWorkspace.getAccessedFrom());
				} else if (stockpileComponent != null) {
					return VectorUtils.toVector(allocationPosition);
				} else if (!furnitureEntityAttributes.getFurnitureType().isBlocksMovement()) {
					return VectorUtils.toVector(allocationPosition);
				} else {
					Logger.error("Could not navigate to any workspaces when picking destination");
					return null;
				}
			} else {
				Logger.error("Could not find furniture for destination of assigned item allocation");
				return null;
			}
		} else {
			return VectorUtils.toVector(allocationPosition);
		}
	}

	private boolean currentlyCarryingItemFromAllocation() {
		if (parent.getAssignedHaulingAllocation().getHauledEntityId() == null) {
			return false;
		}
		HaulingComponent haulingComponent = parent.parentEntity.getComponent(HaulingComponent.class);
		boolean haulingItem = haulingComponent != null && haulingComponent.getHauledEntity() != null;
		if (haulingItem) {
			return true;
		} else {
			InventoryComponent inventoryComponent = parent.parentEntity.getComponent(InventoryComponent.class);
			Entity carriedItem = inventoryComponent.getById(parent.getAssignedHaulingAllocation().getHauledEntityId());
			return carriedItem != null;
		}
	}

	protected Vector2 getJobLocation(GameContext gameContext) {
		Job assignedJob = parent.getAssignedJob();
		if (assignedJob == null) {
			return null;
		} else if (assignedJob.getType().isAccessedFromAdjacentTile()) {
			Array<GridPoint2> navigableTiles = new Array<>();

			for (MapTile neighbourTile : gameContext.getAreaMap().getOrthogonalNeighbours(assignedJob.getJobLocation().x, assignedJob.getJobLocation().y).values()) {
				if (neighbourTile.isNavigable(parent.parentEntity)) {
					navigableTiles.add(neighbourTile.getTilePosition());
				}
			}
			if (navigableTiles.size == 0) {
				return null;
			} else if (navigableTiles.size == 1) {
				return VectorUtils.toVector(navigableTiles.get(0));
			} else {
				// Slight hack - picking one at random should eventually pick one we can get to
				return VectorUtils.toVector(navigableTiles.get(gameContext.getRandom().nextInt(navigableTiles.size)));
			}
		} else {
			return VectorUtils.toVector(assignedJob.getJobLocation());
		}
	}

	@Override
	public void pathfindingComplete(GraphPath<Vector2> path, long relatedId) {
		this.path = path;
		if (path == null || path.getCount() == 0) {
			// No path found / possible
			completionType = CompletionType.FAILURE;
		}
	}

	@Override
	public void pathfindingStarted(PathfindingTask task) {
		this.pathfindingTask = task;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (path != null) {
			asJson.put("pathfindingRequested", true);
			JSONArray pathJson = new JSONArray();
			for (Vector2 pathNode : path) {
				pathJson.add(JSONUtils.toJSON(pathNode));
			}
			asJson.put("path", pathJson);
		}
		// if pathfindingRequested is true but we don't have a path yet, don't save it, so we'll ask for pathfinding again on load

		if (timeWaitingForPath > 0f) {
			asJson.put("timeWaitingForPath", timeWaitingForPath);
		}
		if (pathCursor != 0) {
			asJson.put("pathCursor", pathCursor);
		}
		if (overrideLocation != null) {
			asJson.put("overrideLocation", JSONUtils.toJSON(overrideLocation));
		}
		if (!pathfindingFlags.isEmpty()) {
			JSONArray pathfindingFlagsJson = new JSONArray();
			pathfindingFlags.forEach(flag -> pathfindingFlagsJson.add(flag.name()));
			asJson.put("pathfindingFlags", pathfindingFlagsJson);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.pathfindingRequested = asJson.getBooleanValue("pathfindingRequested");

		JSONArray pathJson = asJson.getJSONArray("path");
		if (pathJson != null) {
			path = new DefaultGraphPath<>();
			for (int cursor = 0; cursor < pathJson.size(); cursor++) {
				path.add(JSONUtils.vector2(pathJson.getJSONObject(cursor)));
			}
		}

		this.timeWaitingForPath = asJson.getFloatValue("timeWaitingForPath");
		this.pathCursor = asJson.getIntValue("pathCursor");
		this.overrideLocation = JSONUtils.vector2(asJson.getJSONObject("overrideLocation"));

		JSONArray pathfindingFlagsJson = asJson.getJSONArray("pathfindingFlags");
		if (pathfindingFlagsJson != null) {
			pathfindingFlags = new ArrayList<>();
			for (Object flagName : pathfindingFlagsJson) {
				pathfindingFlags.add(EnumUtils.getEnum(PathfindingFlag.class, flagName.toString()));
			}
		}
	}
}
