package technology.rocketjump.mountaincore.mapgen.model.output;

import static technology.rocketjump.mountaincore.mapgen.model.output.TileType.MOUNTAIN;
import static technology.rocketjump.mountaincore.mapgen.model.output.TileType.OUTSIDE;

public enum TileSubType {

	LOAMY_FLOOR_CAVE(MOUNTAIN), STONE_FLOOR_CAVE(MOUNTAIN), MOUNTAIN_ROCK(MOUNTAIN),
	FOREST(OUTSIDE), GRASSLAND(OUTSIDE), PLAINS(OUTSIDE), TUNDRA(OUTSIDE);

	public final TileType parentType;

	TileSubType(TileType parentType) {
		this.parentType = parentType;
	}

}
