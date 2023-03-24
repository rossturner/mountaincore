package technology.rocketjump.mountaincore.messaging.types;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.planning.PathfindingCallback;
import technology.rocketjump.mountaincore.entities.planning.PathfindingFlag;
import technology.rocketjump.mountaincore.mapping.model.TiledMap;

import java.util.List;

public class PathfindingRequestMessage {

	private final Entity requestingEntity;
	private final Vector2 origin;
	private final Vector2 destination;
	private final TiledMap map;
	private final PathfindingCallback callback;
	private final long relatedId; // This is used to match pathfinding requests to a certain entity or job ID
	private final List<PathfindingFlag> flags;

	public PathfindingRequestMessage(Entity requestingEntity, Vector2 origin, Vector2 destination, TiledMap map,
									 PathfindingCallback callback, long relatedId, List<PathfindingFlag> flags) {
		this.requestingEntity = requestingEntity;
		this.origin = origin;
		this.destination = destination;
		this.map = map;
		this.callback = callback;
		this.relatedId = relatedId;
		this.flags = flags;
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

	public List<PathfindingFlag> getFlags() {
		return flags;
	}
}
