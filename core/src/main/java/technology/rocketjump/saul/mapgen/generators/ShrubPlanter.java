package technology.rocketjump.saul.mapgen.generators;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.utils.Array;
import technology.rocketjump.saul.mapgen.model.input.GameMapGenerationParams;
import technology.rocketjump.saul.mapgen.model.input.ShrubType;
import technology.rocketjump.saul.mapgen.model.output.GameMap;
import technology.rocketjump.saul.mapgen.model.output.GameMapTile;
import technology.rocketjump.saul.mapgen.model.output.MapSubRegion;
import technology.rocketjump.saul.mapgen.model.output.TileSubType;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class ShrubPlanter {

	public static final int MAX_SHRUB_NEIGHBOURS_ALLOWED = 4;

	// Used by randomPointNear()
	private Array<GridPoint2> outerOffsets = new Array<>(8);
	private Array<GridPoint2> innerOffsets = new Array<>(8);

	public ShrubPlanter() {
		outerOffsets.add(new GridPoint2(-2, 0)); // W
		outerOffsets.add(new GridPoint2(-2, 2)); // NW
		outerOffsets.add(new GridPoint2(0, 2)); // N
		outerOffsets.add(new GridPoint2(2, 2)); // NE
		outerOffsets.add(new GridPoint2(2, 0)); // E
		outerOffsets.add(new GridPoint2(2, -2)); // SE
		outerOffsets.add(new GridPoint2(0, -2)); // S
		outerOffsets.add(new GridPoint2(-2, -2)); // SW

		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				if (x != 0 || y != 0) {
					innerOffsets.add(new GridPoint2(x, y));
				}
			}
		}
	}

	private List<GridPoint2> globalShrubPositions = new LinkedList<>();

	public void placeShrubs(GameMap map, TileSubType targetType, Random random, GameMapGenerationParams generationParams) {
		for (MapSubRegion subRegion : map.getSubRegions().values()) {
			if (subRegion.getSubRegionType().equals(targetType)) {
				ShrubType shrubType = pickShrub(generationParams, random);

				placeShrubs(map, subRegion, random, shrubType);
			}
		}

		for (GridPoint2 globalShrubPosition : globalShrubPositions) {
			if (!isShrubAllowedAt(globalShrubPosition, map, null, true)) {
				map.get(globalShrubPosition).setShrubType(null);
			}
		}
	}

	private ShrubType pickShrub(GameMapGenerationParams generationParams, Random random) {
		List<ShrubType> shrubTypes = generationParams.getShrubTypes();
		return shrubTypes.get(random.nextInt(shrubTypes.size()));
	}

	private void placeShrubs(GameMap map, MapSubRegion subRegion, Random random, ShrubType shrubType) {
		LinkedList<GridPoint2> initialPositions = new LinkedList<>();

		int minX = subRegion.getMinX();
		int maxX = subRegion.getMaxX();
		int width = maxX - minX;
		int minY = subRegion.getMinY();
		int maxY = subRegion.getMaxY();
		int height = maxY - minY;

		for (int chunkX = minX; chunkX < maxX; chunkX += 10) {
			for (int chunkY = minY; chunkY < maxY; chunkY += 10) {
				GridPoint2 attemptPosition = null;
				for (int x = 0; x < 7; x++) {
					attemptPosition = new GridPoint2(chunkX + random.nextInt(10), chunkY + random.nextInt(10));
					if (subRegion.contains(map.get(attemptPosition))) {
						break;
					} else {
						attemptPosition = null;
					}
				}
				if (attemptPosition != null) {
					initialPositions.add(attemptPosition);
				}
			}
		}

		while (!initialPositions.isEmpty()) {
			GridPoint2 positionToSpawnFrom = initialPositions.removeFirst();
			addShrub(positionToSpawnFrom, map, subRegion, shrubType, random, 1);
		}

	}

	private void addShrub(GridPoint2 position, GameMap map, MapSubRegion subRegion, ShrubType shrubType, Random random, int depth) {
		if (depth > 4) {
			return;
		}

		if (isShrubAllowedAt(position, map, subRegion, false)) {
			map.get(position).setShrubType(shrubType);
			globalShrubPositions.add(position);

			for (int i = 0; i < 4; i++) {
				GridPoint2 neighbour = position.cpy().add(innerOffsets.get(random.nextInt(innerOffsets.size)));
				addShrub(neighbour, map, subRegion, shrubType, random, depth + 1);
			}
		}
	}

	private ShrubType pickShrubType(Random random, ShrubType nonFruitShrub, ShrubType fruitShrub, float ratioOfFruitingShrubs) {
		boolean hasFruit = random.nextFloat() < ratioOfFruitingShrubs;
		return hasFruit ? fruitShrub : nonFruitShrub;
	}

	/**
	 * This method returns a gridpoint near X
	 *
	 * See this diagram
	 *
	 * 1121211
	 * 1121211
	 * 2231322
	 * 111X111
	 * 2232322
	 * 1121211
	 * 1121211
	 *
	 */
	public GridPoint2 randomPointNear(GridPoint2 origin, Random random) {
		return origin.cpy()
				.add(outerOffsets.get(random.nextInt(outerOffsets.size)))
				.add(innerOffsets.get(random.nextInt(innerOffsets.size)));
	}

	/**
	 * Shrubs are allowed as long as there are at most 3 other shrubs or trees nearby
	 */
	public boolean isShrubAllowedAt(GridPoint2 position, GameMap map, MapSubRegion subRegionToMatch, boolean allowExistingShrub) {
		GameMapTile tileAtPosition = map.get(position);
		if (tileAtPosition == null || tileAtPosition.hasTree() || tileAtPosition.hasRiver()) {
			return false;
		}
		if (!allowExistingShrub && tileAtPosition.hasShrub()) {
			return false;
		}

		if (subRegionToMatch != null && tileAtPosition.getSubRegion().getSubRegionId() != subRegionToMatch.getSubRegionId()) {
			// Keep to same sub-region for now
			return false;
		}

		int numShrubNeighbours = 0;
		for (int x = position.x - 1; x <= position.x + 1; x++) {
			for (int y = position.y - 1; y <= position.y + 1; y++) {
				if (x == position.x && y == position.y) {
					continue;
				}
				GameMapTile tile = map.get(x, y);
				if (tile == null) {
					continue; // If bottom of map is below tree, don't care too much
				}
				if (tile.hasTree() || tile.hasShrub()) {
					numShrubNeighbours++;
				}
			}
		}

		return numShrubNeighbours <= MAX_SHRUB_NEIGHBOURS_ALLOWED;
	}

}
