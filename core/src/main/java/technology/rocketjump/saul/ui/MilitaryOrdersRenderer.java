package technology.rocketjump.saul.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.military.model.Squad;
import technology.rocketjump.saul.military.model.SquadOrderType;
import technology.rocketjump.saul.rendering.RenderMode;
import technology.rocketjump.saul.rendering.camera.TileBoundingBox;
import technology.rocketjump.saul.rendering.custom_libgdx.ShaderLoader;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;
import technology.rocketjump.saul.rendering.utils.HexColors;

import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.saul.misc.VectorUtils.toVector;

@Singleton
public class MilitaryOrdersRenderer {

	private static final Color SLIGHT_TRANSPARENCY = new Color(1, 1, 1, 0.7f);
	public static final Color LIME = HexColors.get("#86DB00");
	public static final Color LIGHT_BLUE = HexColors.get("#99F8FF");
	private final GameInteractionStateContainer interactionStateContainer;
	private final EntityRenderer entityRenderer;
	private final SpriteBatch greyscaleSpriteBatch = new SpriteBatch();

	@Inject
	public MilitaryOrdersRenderer(GameInteractionStateContainer interactionStateContainer, EntityRenderer entityRenderer) {
		this.interactionStateContainer = interactionStateContainer;
		this.entityRenderer = entityRenderer;

		FileHandle vertexShaderFile = Gdx.files.classpath("shaders/default_vertex_shader.glsl");
		FileHandle fragmentShaderFile = Gdx.files.classpath("shaders/greyscale_texture_fragment_shader.glsl");
		greyscaleSpriteBatch.setShader(ShaderLoader.createShader(vertexShaderFile, fragmentShaderFile));
	}

	public void render(GameContext gameContext, OrthographicCamera camera) {
		TileBoundingBox boundingBox = new TileBoundingBox(camera, gameContext.getAreaMap());

		greyscaleSpriteBatch.setProjectionMatrix(camera.combined);
		greyscaleSpriteBatch.begin();

		for (Squad squad : gameContext.getSquads().values()) {
			if (!squad.getMemberEntityIds().isEmpty() && squad.getCurrentOrderType().equals(SquadOrderType.GUARDING) &&
				squad.getGuardingLocation() != null) {
				renderSquadPositions(squad, squad.getGuardingLocation(), boundingBox, gameContext, LIME);
			}
		}

		if (interactionStateContainer.getInteractionMode().equals(GameInteractionMode.SQUAD_MOVE_TO_LOCATION)) {
			Vector3 worldPosition = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
			Vector2 worldPosition2 = new Vector2(worldPosition.x, worldPosition.y);
			GridPoint2 cursorTilePosition = toGridPoint(worldPosition2);

			MapTile currentTile = gameContext.getAreaMap().getTile(cursorTilePosition);
			if (currentTile != null && currentTile.isNavigable(null)) {
				Squad selectedSquad = interactionStateContainer.getSelectable().getSquad();
				if (selectedSquad != null) {
					greyscaleSpriteBatch.setColor(HexColors.POSITIVE_COLOR);
					renderSquadPositions(selectedSquad, cursorTilePosition, boundingBox, gameContext, LIGHT_BLUE);
				}
			}
		}

		greyscaleSpriteBatch.end();
	}

	private void renderSquadPositions(Squad squad, GridPoint2 centralLocation, TileBoundingBox boundingBox, GameContext gameContext, Color overrideColor) {
		for (int cursor = 0; cursor < squad.getMemberEntityIds().size(); cursor++) {
			GridPoint2 formationPosition = squad.getFormation().getFormationPosition(cursor, centralLocation, gameContext, squad.getMemberEntityIds().size());
			if (formationPosition != null && boundingBox.contains(formationPosition)) {
				Entity squadMember = gameContext.getEntities().get(squad.getMemberEntityIds().get(cursor));
				Vector2 currentWorldPosition = squadMember.getLocationComponent().getWorldPosition();
				Vector2 currentFacing = squadMember.getLocationComponent().getFacing();
				float currentRotation = squadMember.getLocationComponent().getRotation();

				if (currentWorldPosition != null && !sameTile(formationPosition, currentWorldPosition)) {
					squadMember.getLocationComponent().setWorldPosition(toVector(formationPosition), false, false);
					squadMember.getLocationComponent().setFacing(EntityAssetOrientation.DOWN.toVector2());
					squadMember.getLocationComponent().setRotation(0);

					MapTile formationTile = gameContext.getAreaMap().getTile(formationPosition);
					overrideColor = formationTile.isNavigable(squadMember) ? overrideColor : HexColors.NEGATIVE_COLOR;

					entityRenderer.render(squadMember, greyscaleSpriteBatch, RenderMode.DIFFUSE, null, overrideColor, SLIGHT_TRANSPARENCY);

					squadMember.getLocationComponent().setRotation(currentRotation);
					squadMember.getLocationComponent().setFacing(currentFacing);
					squadMember.getLocationComponent().setWorldPosition(currentWorldPosition, false, false);
				}
			}
		}
	}

	private boolean sameTile(GridPoint2 gridPoint, Vector2 worldPosition) {
		return gridPoint.x == Math.floor(worldPosition.x) && gridPoint.y == Math.floor(worldPosition.y);
	}
}
