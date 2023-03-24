package technology.rocketjump.mountaincore.mapping.tile;

import com.badlogic.gdx.math.Vector2;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Arrays;
import java.util.List;

public enum CompassDirection {

    /**
     * The binary mask represents the 8 compass points in the following order:
     *
     *  0 | 1 | 2
     *  3 |   | 4
     *  5 | 6 | 7
	 *
	 *  The index is used for reference in order for String position and is as follows
	 *
	 *  0 | 1 | 2
	 *  3 |   | 5
	 *  6 | 7 | 8
	 *  (4 is the index of the central character in a String representation)
	 *
     */
    NORTH		(0, 1, 		1 << 1, 1),
    NORTH_EAST	(1, 1, 		1 << 2, 2),
    EAST		(1, 0, 		1 << 4, 5),
    SOUTH_EAST	(1, -1, 	1 << 7, 8),
    SOUTH		(0, -1, 	1 << 6, 7),
    SOUTH_WEST	(-1, -1, 	1 << 5, 6),
    WEST		(-1, 0, 	1 << 3, 3),
    NORTH_WEST	(-1, 1, 	1	  , 0);

    private final int xOffset, yOffset;
    private final int binaryMask;
	private final int index;
	private final float distance;

	public static final List<CompassDirection> CARDINAL_DIRECTIONS = Arrays.asList(NORTH, EAST, SOUTH, WEST);
	public static final List<CompassDirection> DIAGONAL_DIRECTIONS = Arrays.asList(NORTH_WEST, NORTH_EAST, SOUTH_WEST, SOUTH_EAST);

	CompassDirection(int xOffset, int yOffset, int binaryMask, int index) {
        this.xOffset = xOffset;
        this.yOffset = yOffset;
        this.binaryMask = binaryMask;
		this.index = index;
		// This will only be 1.0 or sqrt(2) but it's only calculated once, so it's not worth doing anything more complex
		this.distance = (float) Math.sqrt((xOffset * xOffset) + (yOffset * yOffset));
    }

	public static CompassDirection oppositeOf(CompassDirection direction) {
		switch (direction) {
			case NORTH:
				return SOUTH;
			case SOUTH:
				return NORTH;
			case EAST:
				return WEST;
			case WEST:
				return EAST;
			case NORTH_EAST:
				return SOUTH_WEST;
			case NORTH_WEST:
				return SOUTH_EAST;
			case SOUTH_EAST:
				return NORTH_WEST;
			case SOUTH_WEST:
				return NORTH_EAST;
			default:
				throw new NotImplementedException("Unexpected value of " + CompassDirection.class.getSimpleName());
		}
	}

	public List<CompassDirection> withNeighbours() {
		return switch (this) {
			case NORTH -> List.of(NORTH_WEST, NORTH, NORTH_EAST);
			case NORTH_EAST -> List.of(NORTH, NORTH_EAST, EAST);
			case EAST -> List.of(NORTH_EAST, EAST, SOUTH_EAST);
			case SOUTH_EAST -> List.of(EAST, SOUTH_EAST, SOUTH);
			case SOUTH -> List.of(SOUTH_EAST, SOUTH, SOUTH_WEST);
			case SOUTH_WEST -> List.of(SOUTH, SOUTH_WEST, WEST);
			case WEST -> List.of(SOUTH_WEST, WEST, NORTH_WEST);
			case NORTH_WEST -> List.of(WEST, NORTH_WEST, NORTH);
		};
	}

	public static CompassDirection fromNormalisedVector(Vector2 facing) {
		if (facing.x > 0) {
			// Facing is to east/right side
			if (facing.y > 0.853f) {
				return NORTH;
			} else if (facing.y > 0.354f) {
				return NORTH_EAST;
			} else if (facing.y > -0.354f) {
				return EAST;
			} else if (facing.y > -0.853f) {
				return SOUTH_EAST;
			} else {
				return SOUTH;
			}
		} else {
			// Facing is to west/left side
			if (facing.y > 0.853f) {
				return NORTH;
			} else if (facing.y > 0.354f) {
				return NORTH_WEST;
			} else if (facing.y > -0.354f) {
				return WEST;
			} else if (facing.y > -0.853f) {
				return SOUTH_WEST;
			} else {
				return SOUTH;
			}
		}
	}

    public int getXOffset() {
        return xOffset;
    }

    public int getYOffset() {
        return yOffset;
    }

    public Vector2 toVector() {
        return new Vector2(xOffset, yOffset);
    }

    public int getBinaryMask() {
        return binaryMask;
    }

    public int getIndex() {
		return index;
    }

	public float distance() {
		return distance;
	}

	public boolean isDiagonal() {
		return xOffset != 0 && yOffset != 0;
	}
}
