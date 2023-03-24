package technology.rocketjump.mountaincore.assets.entities.model;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.mountaincore.entities.model.EntityType;

/**
 * This represents the different visual orientations for sprite rendering, mapped from a normalised 2D vector
 */
public enum EntityAssetOrientation {

	DOWN(null, new Vector2(0, -1)),
	UP(DOWN, new Vector2(0, 1)),
	// The following are used for 4-directional entities
	LEFT(DOWN, new Vector2(-1, 0)),
	RIGHT(DOWN, new Vector2(1, 0)),
	// The following are used for 6-directional entities
	DOWN_LEFT(LEFT, new Vector2(-1, -1)),
	DOWN_RIGHT(RIGHT, new Vector2(1, -1)),
	UP_LEFT(LEFT, new Vector2(-1, 1)),
	UP_RIGHT(RIGHT, new Vector2(1, 1));

	private EntityAssetOrientation fallback;
	private Vector2 asVector;
	public final Vector2 asOriginalVector;

	EntityAssetOrientation(EntityAssetOrientation fallback, Vector2 vector) {
		this.fallback = fallback;
		this.asVector = vector.cpy().nor();
		this.asOriginalVector = vector.cpy();
	}

	public boolean hasFallback() {
		return fallback != null;
	}

	/**
	 * This method turns a normalised vector into an orientation
	 * There are 8 possible directions but this splits them into 6 for now, each given ~60 degrees
	 *
	 * @param facing a normalised vector representing a direction
	 * @param type
	 * @return which Orientation instance is represented by the vector
	 */
	public static EntityAssetOrientation fromFacing(Vector2 facing, EntityType type) {
		if (EntityType.VEHICLE.equals(type)) {
			// Vehicles are only rendered in 4 directions
			return fromFacingTo4Directions(facing);
		} else {
			if (facing.x > 0) {
				// Facing is to east/right side
				if (facing.y > 0.866f) {
					return UP;
				} else if (facing.y > 0.1f) {
					return UP_RIGHT;
				} else if (facing.y > -0.866f) {
					return DOWN_RIGHT;
				} else {
					return DOWN;
				}
			} else {
				// Facing is to west/left side
				if (facing.y > 0.866f) {
					return UP;
				} else if (facing.y > 0.1f) {
					return UP_LEFT;
				} else if (facing.y > -0.866f) {
					return DOWN_LEFT;
				} else {
					return DOWN;
				}
			}
		}
	}

	public static EntityAssetOrientation fromFacingTo4Directions(Vector2 facing) {
		if (facing.x > 0) {
			// Facing is to east/right side
			if (facing.y > 0.8f) {
				return UP;
			} else if (facing.y > -0.8f) {
				return RIGHT;
			} else {
				return DOWN;
			}
		} else {
			// Facing is to west/left side
			if (facing.y > 0.8f) {
				return UP;
			} else if (facing.y > -0.8f) {
				return LEFT;
			} else {
				return DOWN;
			}
		}
	}

	public static EntityAssetOrientation fromFacingTo8Directions(Vector2 facing) {
		if (facing.x > 0) {
			// Facing is to east/right side
			if (facing.y > 0.853f) {
				return UP;
			} else if (facing.y > 0.354f) {
				return UP_RIGHT;
			} else if (facing.y > -0.354f) {
				return RIGHT;
			} else if (facing.y > -0.853f) {
				return DOWN_RIGHT;
			} else {
				return DOWN;
			}
		} else {
			// Facing is to west/left side
			if (facing.y > 0.853f) {
				return UP;
			} else if (facing.y > 0.354f) {
				return UP_LEFT;
			} else if (facing.y > -0.354f) {
				return LEFT;
			} else if (facing.y > -0.853f) {
				return DOWN_LEFT;
			} else {
				return DOWN;
			}
		}
	}

	public Vector2 toVector2() {
		return asVector;
	}

	public EntityAssetOrientation toOrthogonal() {
		switch (this) {
			case UP_RIGHT:
			case DOWN_RIGHT:
				return RIGHT;
			case UP_LEFT:
			case DOWN_LEFT:
				return LEFT;
			default:
				return this;
		}
	}
}
