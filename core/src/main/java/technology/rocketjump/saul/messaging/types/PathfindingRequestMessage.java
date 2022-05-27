package technology.rocketjump.saul.messaging.types;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.planning.PathfindingCallback;
import technology.rocketjump.saul.mapping.model.TiledMap;

public class PathfindingRequestMessage {

	private final Entity requestingEntity;
	private final Vector2 origin;
	private final Vector2 destination;
	private final TiledMap map;
	private final PathfindingCallback callback;
	private final long relatedId; // This is used to match pathfinding requests to a certain entity or job ID

	public PathfindingRequestMessage(Entity requestingEntity, Vector2 origin, Vector2 destination, TiledMap map, PathfindingCallback callback, long relatedId) {
		this.requestingEntity = requestingEntity;
		this.origin = origin;
		this.destination = destination;
		this.map = map;
		this.callback = callback;
		this.relatedId = relatedId;
	}

	public Vector2 getOrigin() {
		return origin;
	}

	public Vector2 getDestination() {
		return destination;
	}

	public TiledMap getMap() {
		return map;
	}

	public PathfindingCallback getCallback() {
		return callback;
	}

	public long getRelatedId() {
		return relatedId;
	}

	public Entity getRequestingEntity() {
		return requestingEntity;
	}
}
