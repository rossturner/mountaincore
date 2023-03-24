package technology.rocketjump.mountaincore.mapping.factories;

import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.mountaincore.mapping.model.TiledMap;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.mapping.tile.MapVertex;
import technology.rocketjump.mountaincore.mapping.tile.underground.TileLiquidFlow;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WaterFlowVertexApplicator {

	public void applyFlowToVertices(TiledMap tiledMap, List<GridPoint2> riverTileLocations) {
		if (riverTileLocations == null || riverTileLocations.isEmpty()) {
			return;
		}
		Set<MapVertex> verticesToUpdate = new HashSet<>();

		for (GridPoint2 riverTileLocation : riverTileLocations) {
			verticesToUpdate.addAll(Arrays.asList(tiledMap.getVertices(riverTileLocation.x, riverTileLocation.y)));
		}

		for (MapVertex vertexToUpdate : verticesToUpdate) {
			applyFlowToVertex(vertexToUpdate, tiledMap);
		}
	}

	private void applyFlowToVertex(MapVertex vertexToUpdate, TiledMap tiledMap) {
		Vector2 flowDirection = new Vector2();

		// Add each of up to 4 neighbour tiles
		for (int xOffset = -1; xOffset <= 0; xOffset++) {
			for (int yOffset = -1; yOffset <= 0; yOffset++) {
				MapTile tileAtOffset = tiledMap.getTile(vertexToUpdate.getVertexX() + xOffset, vertexToUpdate.getVertexY() + yOffset);
				if (tileAtOffset != null && tileAtOffset.getFloor().isRiverTile()) {
					flowDirection.add(tileAtOffset.getFloor().getRiverTile().getFlowDirection());
				}
			}
		}

		// Divide by 4, even if less than 4 flow tiles, so flow is slower at river edges
		flowDirection.scl(0.25f);

		vertexToUpdate.setWaterFlowDirection(flowDirection);
		vertexToUpdate.setAverageWaterDepth(TileLiquidFlow.MAX_LIQUID_FLOW_PER_TILE);
	}


}
