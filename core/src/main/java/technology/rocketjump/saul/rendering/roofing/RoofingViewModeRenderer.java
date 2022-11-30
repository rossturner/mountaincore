package technology.rocketjump.saul.rendering.roofing;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.TextureAtlasRepository;
import technology.rocketjump.saul.jobs.JobStore;
import technology.rocketjump.saul.jobs.model.Job;
import technology.rocketjump.saul.mapping.model.TiledMap;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.mapping.tile.roof.RoofConstructionState;
import technology.rocketjump.saul.ui.GameInteractionMode;
import technology.rocketjump.saul.ui.GameInteractionStateContainer;

import javax.inject.Inject;
import javax.inject.Singleton;

import static technology.rocketjump.saul.mapping.tile.TileExploration.UNEXPLORED;
import static technology.rocketjump.saul.rendering.camera.TileBoundingBox.*;
import static technology.rocketjump.saul.ui.InWorldUIRenderer.insideSelectionArea;

@Singleton
public class RoofingViewModeRenderer {

	private final GameInteractionStateContainer interactionStateContainer;
	private final JobStore jobStore;

	@Inject
	public RoofingViewModeRenderer(GameInteractionStateContainer interactionStateContainer, JobStore jobStore, TextureAtlasRepository textureAtlasRepository) {
		this.interactionStateContainer = interactionStateContainer;
		this.jobStore = jobStore;

		TextureAtlas guiAtlas = textureAtlasRepository.get(TextureAtlasRepository.TextureAtlasType.GUI_TEXTURE_ATLAS);

		for (RoofConstructionState roofConstructionState : RoofConstructionState.values()) {
			roofConstructionState.icon = guiAtlas.createSprite(roofConstructionState.iconName);
			if (roofConstructionState.icon == null) {
				Logger.error(String.format("Could not find icon with name %s for roof state %s", roofConstructionState.iconName, roofConstructionState.name()));
			}
		}
	}

	public void render(TiledMap map, OrthographicCamera camera, Batch spriteBatch, ShapeRenderer shapeRenderer, boolean blinkState) {
		int minX = getMinX(camera);
		int maxX = getMaxX(camera, map);
		int minY = getMinY(camera);
		int maxY = getMaxY(camera, map);
		Vector2 minDraggingPoint = interactionStateContainer.getMinPoint();
		Vector2 maxDraggingPoint = interactionStateContainer.getMaxPoint();
		GridPoint2 minDraggingTile = new GridPoint2(MathUtils.floor(minDraggingPoint.x), MathUtils.floor(minDraggingPoint.y));
		GridPoint2 maxDraggingTile = new GridPoint2(MathUtils.floor(maxDraggingPoint.x), MathUtils.floor(maxDraggingPoint.y));

		shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
		for (int x = minX; x <= maxX; x++) {
			for (int y = maxY; y >= minY; y--) {
				MapTile mapTile = map.getTile(x, y);
				if (mapTile != null) {
					if (mapTile.getExploration().equals(UNEXPLORED)) {
						continue;
					}

					// Show roofing tile colour
					shapeRenderer.setColor(mapTile.getRoof().getState().viewColor);
					shapeRenderer.rect(x, y, 1, 1);
				}
			}
		}
		shapeRenderer.end();


		spriteBatch.begin();
		for (int x = minX; x <= maxX; x++) {
			for (int y = maxY; y >= minY; y--) {
				MapTile mapTile = map.getTile(x, y);
				if (mapTile != null) {
					if (mapTile.getExploration().equals(UNEXPLORED)) {
						continue;
					}

					if (insideSelectionArea(minDraggingTile, maxDraggingTile, x, y, interactionStateContainer)) {
						if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.CANCEL)) {
							// Don't show designations
						} else if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.DESIGNATE_ROOFING)) {
							// This is within dragging area
							if (shouldHighlight(mapTile)) {
								spriteBatch.setColor(RoofConstructionState.PENDING.renderColor);
								spriteBatch.draw(RoofConstructionState.PENDING.icon, x, y, 1, 1);
							} else {
								renderExistingRoofConstruction(x, y, mapTile, spriteBatch, blinkState);
							}
						} else if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.DECONSTRUCT_ROOFING)) {
							if (shouldHighlight(mapTile)) {
								spriteBatch.setColor(RoofConstructionState.PENDING_DECONSTRUCTION.renderColor);
								spriteBatch.draw(RoofConstructionState.PENDING_DECONSTRUCTION.icon, x, y, 1, 1);
							} else {
								renderExistingRoofConstruction(x, y, mapTile, spriteBatch, blinkState);
							}
						} else {
							// Not a designation-type drag
							renderExistingRoofConstruction(x, y, mapTile, spriteBatch, blinkState);
						}
					} else {
						// Outside selection area
						renderExistingRoofConstruction(x, y, mapTile, spriteBatch, blinkState);
					}
				}
			}
		}
		spriteBatch.end();
	}

	private void renderExistingRoofConstruction(int x, int y, MapTile mapTile, Batch spriteBatch, boolean blinkState) {
		RoofConstructionState roofConstructionState = mapTile.getRoof().getConstructionState();
		if (!roofConstructionState.equals(RoofConstructionState.NONE)) {
			for (Job job : jobStore.getJobsAtLocation(mapTile.getTilePosition())) {
				if (job.getAssignedToEntityId() != null) {
					// There is an assigned job at the location of this designation, so lets skip rendering it if blink is off
					if (!blinkState) {
						return;
					}
				}
			}

			spriteBatch.setColor(roofConstructionState.renderColor);
			spriteBatch.draw(roofConstructionState.icon, x, y, 1, 1);
		}
	}

	private boolean shouldHighlight(MapTile mapTile) {
		if (interactionStateContainer.getInteractionMode().tileDesignationCheck != null) {
			return interactionStateContainer.getInteractionMode().tileDesignationCheck.shouldDesignationApply(mapTile);
		}
		return false;
	}

}
