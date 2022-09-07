package technology.rocketjump.saul.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import technology.rocketjump.saul.assets.AssetDisposable;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.model.TiledMap;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.mapping.tile.TileExploration;
import technology.rocketjump.saul.particles.model.ParticleEffectInstance;
import technology.rocketjump.saul.rendering.lighting.CombinedLightingResultRenderer;
import technology.rocketjump.saul.rendering.lighting.LightProcessor;
import technology.rocketjump.saul.rendering.lighting.PointLight;
import technology.rocketjump.saul.rendering.lighting.WorldLightingRenderer;
import technology.rocketjump.saul.sprites.TerrainSpriteCache;
import technology.rocketjump.saul.ui.InWorldUIRenderer;
import technology.rocketjump.saul.ui.MilitaryOrdersRenderer;

import java.util.LinkedList;
import java.util.List;

@Singleton
public class GameRenderer implements AssetDisposable {

	private final List<PointLight> lightsToRenderThisFrame;
	private PointLight cursorLight;
	private boolean cursorLightEnabled = true;
	private final LightProcessor lightProcessor;

	private final SpriteBatch frameBufferSpriteBatch = new SpriteBatch();

	private FrameBuffer diffuseFrameBuffer;
	private TextureRegion diffuseTextureRegion;
	private FrameBuffer normalMapFrameBuffer;
	private TextureRegion bumpMapTextureRegion;
	private FrameBuffer lightingFrameBuffer;
	private TextureRegion lightingTextureRegion;
	private FrameBuffer combinedFrameBuffer;
	private TextureRegion combinedTextureRegion;

	private final OrthographicCamera viewportCamera;

	private final RenderingOptions renderingOptions;

	private final WorldRenderer worldRenderer;
	private final WorldLightingRenderer worldLightingRenderer;
	private final CombinedLightingResultRenderer combinedRenderer;
	private final DebugRenderer debugRenderer;
	private final InWorldUIRenderer inWorldUIRenderer;
	private final MilitaryOrdersRenderer militaryOrdersRenderer;

	private final TerrainSpriteCache diffuseSpriteCache;
	private final TerrainSpriteCache normalSpriteCache;

	private final List<ParticleEffectInstance> particlesToRenderAsUI = new LinkedList<>();
	private final ScreenWriter screenWriter;

	@Inject
	public GameRenderer(LightProcessor lightProcessor,
						RenderingOptions renderingOptions, WorldRenderer worldRenderer, WorldLightingRenderer worldLightingRenderer,
						CombinedLightingResultRenderer combinedRenderer, DebugRenderer debugRenderer,
						InWorldUIRenderer inWorldUIRenderer, MilitaryOrdersRenderer militaryOrdersRenderer,
						@Named("diffuse") TerrainSpriteCache diffuseSpriteCache,
						@Named("normal") TerrainSpriteCache normalSpriteCache,
						ScreenWriter screenWriter) {
		this.lightProcessor = lightProcessor;
		this.renderingOptions = renderingOptions;
		this.worldRenderer = worldRenderer;
		this.worldLightingRenderer = worldLightingRenderer;
		this.combinedRenderer = combinedRenderer;
		this.debugRenderer = debugRenderer;
		this.inWorldUIRenderer = inWorldUIRenderer;
		this.militaryOrdersRenderer = militaryOrdersRenderer;
		this.diffuseSpriteCache = diffuseSpriteCache;
		this.normalSpriteCache = normalSpriteCache;
		this.screenWriter = screenWriter;

		try {
			initFrameBuffers(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		} catch (IllegalStateException e) {
			initFrameBuffers(1920, 1080); // FIXME This hear to avoid IllegalStateException: frame buffer couldn't be constructed: unsupported combination of formats
		}

		viewportCamera = new OrthographicCamera();
		viewportCamera.setToOrtho(false);

		cursorLight = new PointLight();
		cursorLight.setColor(new Color(1.0f, 1.0f, 1.0f, 1.0f));
		lightsToRenderThisFrame = new LinkedList<>();
		lightsToRenderThisFrame.add(cursorLight);
	}

	@Override
	public void dispose() {
		disposeFrameBuffers();
		worldRenderer.dispose();
		combinedRenderer.dispose();
		cursorLight.dispose();
	}

	public void renderGame(GameContext gameContext, OrthographicCamera camera, float fadeAmount) {
		TiledMap worldMap = gameContext.getAreaMap();
		int screenX = Gdx.input.getX();
		int screenY = Gdx.input.getY();
		if (renderingOptions.debug().showIndividualLightingBuffers()) {
			screenX = renderingOptions.debug().adjustScreenXForSplitView(screenX);
			screenY = renderingOptions.debug().adjustScreenYForSplitView(screenY);
		}

		Vector3 unprojected = camera.unproject(new Vector3(screenX, screenY, 0));

		// TODO move the below to an update method not in a renderer

		Vector2 cursorLightPosition = new Vector2(unprojected.x, unprojected.y);
		cursorLight.setWorldPosition(cursorLightPosition);
		lightsToRenderThisFrame.clear();
		particlesToRenderAsUI.clear();

		MapTile cursorLightTile = worldMap.getTile(cursorLightPosition);
		cursorLightEnabled = cursorLightTile != null && cursorLightTile.getExploration().equals(TileExploration.EXPLORED) &&
				!cursorLightTile.hasWall() && !cursorLightTile.hasDoorway();

		if (cursorLightEnabled) {
			lightProcessor.updateLightGeometry(cursorLight, worldMap);
			lightsToRenderThisFrame.add(cursorLight);
		}


		diffuseFrameBuffer.begin();
		worldRenderer.renderWorld(worldMap, camera, diffuseSpriteCache, RenderMode.DIFFUSE, lightsToRenderThisFrame, particlesToRenderAsUI);
		diffuseFrameBuffer.end();

		normalMapFrameBuffer.begin();
		worldRenderer.renderWorld(worldMap, camera, normalSpriteCache, RenderMode.NORMALS, null, null);
		normalMapFrameBuffer.end();

		/////// Draw lighting info ///

		lightingFrameBuffer.begin();
		worldLightingRenderer.renderWorldLighting(gameContext, lightsToRenderThisFrame, camera, bumpMapTextureRegion);
		lightingFrameBuffer.end();


		if (renderingOptions.debug().showIndividualLightingBuffers()) {
			////// Draw combined final render ///

			combinedFrameBuffer.begin();
			combinedRenderer.renderFinal(diffuseTextureRegion, lightingTextureRegion, fadeAmount);
			combinedFrameBuffer.end();

			frameBufferSpriteBatch.begin();
			frameBufferSpriteBatch.disableBlending();
			frameBufferSpriteBatch.setProjectionMatrix(viewportCamera.projection);

			frameBufferSpriteBatch.draw(diffuseTextureRegion, -Gdx.graphics.getWidth() / 2.0f, 0f,
					Gdx.graphics.getWidth() / 2.0f,  Gdx.graphics.getHeight() / 2.0f);

			frameBufferSpriteBatch.draw(bumpMapTextureRegion, 0f, 0f,
					Gdx.graphics.getWidth() / 2.0f,  Gdx.graphics.getHeight() / 2.0f);

			frameBufferSpriteBatch.draw(lightingTextureRegion, -Gdx.graphics.getWidth() / 2.0f, -Gdx.graphics.getHeight() / 2.0f,
					Gdx.graphics.getWidth() / 2.0f,  Gdx.graphics.getHeight() / 2.0f);

			frameBufferSpriteBatch.draw(combinedTextureRegion, 0f, -Gdx.graphics.getHeight() / 2.0f,
					Gdx.graphics.getWidth() / 2.0f,  Gdx.graphics.getHeight() / 2.0f);

			frameBufferSpriteBatch.end();
		} else {
			combinedRenderer.renderFinal(diffuseTextureRegion, lightingTextureRegion, fadeAmount);
			inWorldUIRenderer.render(gameContext, camera, particlesToRenderAsUI, diffuseSpriteCache);
			militaryOrdersRenderer.render(gameContext, camera);
		}

		debugRenderer.render(worldMap, camera);

	}

	public void onResize(int width, int height) {
		viewportCamera.setToOrtho(false, width, height);
		disposeFrameBuffers();
		initFrameBuffers(width, height);
	}

	private void initFrameBuffers(int width, int height) {
		diffuseFrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, /* hasDepth */ false, /* hasStencil */ false);
		diffuseTextureRegion = new TextureRegion(diffuseFrameBuffer.getColorBufferTexture(), width, height);
		diffuseTextureRegion.flip(false, true);

		normalMapFrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, /* hasDepth */ false, /* hasStencil */ false);
		bumpMapTextureRegion = new TextureRegion(normalMapFrameBuffer.getColorBufferTexture(), width, height);
		bumpMapTextureRegion.flip(false, true);

		lightingFrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, /* hasDepth */ false, /* hasStencil */ false);
		lightingTextureRegion = new TextureRegion(lightingFrameBuffer.getColorBufferTexture(), width, height);
		lightingTextureRegion.flip(false, true);

		combinedFrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, /* hasDepth */ false, /* hasStencil */ false);
		combinedTextureRegion = new TextureRegion(combinedFrameBuffer.getColorBufferTexture(), width, height);
		combinedTextureRegion.flip(false, true);
	}

	private void disposeFrameBuffers() {
		diffuseFrameBuffer.dispose();
		normalMapFrameBuffer.dispose();
		lightingFrameBuffer.dispose();
		combinedFrameBuffer.dispose();
	}

}
