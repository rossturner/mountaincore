package technology.rocketjump.mountaincore.rendering;

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
import technology.rocketjump.mountaincore.assets.AssetDisposable;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.model.TiledMap;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.mapping.tile.TileExploration;
import technology.rocketjump.mountaincore.particles.model.ParticleEffectInstance;
import technology.rocketjump.mountaincore.rendering.lighting.CombinedLightingResultRenderer;
import technology.rocketjump.mountaincore.rendering.lighting.LightProcessor;
import technology.rocketjump.mountaincore.rendering.lighting.PointLight;
import technology.rocketjump.mountaincore.rendering.lighting.WorldLightingRenderer;
import technology.rocketjump.mountaincore.sprites.TerrainSpriteCache;
import technology.rocketjump.mountaincore.ui.InWorldUIRenderer;
import technology.rocketjump.mountaincore.ui.MilitaryOrdersRenderer;

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
	private FrameBuffer selectedEntitiesFrameBuffer;
	private TextureRegion selectedEntitiesTextureRegion;
	private FrameBuffer militaryOrdersFrameBuffer;
	private TextureRegion militaryOrdersTextureRegion;
	private FrameBuffer combinedFrameBuffer;
	private TextureRegion combinedTextureRegion;

	private TextureRegion[] textureRegions;
	private String[] textureRegionNames;

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
	private final OutlineExtensionRenderer outlineExtensionRenderer;

	@Inject
	public GameRenderer(LightProcessor lightProcessor,
						RenderingOptions renderingOptions, WorldRenderer worldRenderer, WorldLightingRenderer worldLightingRenderer,
						CombinedLightingResultRenderer combinedRenderer, DebugRenderer debugRenderer,
						InWorldUIRenderer inWorldUIRenderer,
						MilitaryOrdersRenderer militaryOrdersRenderer,
						@Named("diffuse") TerrainSpriteCache diffuseSpriteCache,
						@Named("normal") TerrainSpriteCache normalSpriteCache,
						ScreenWriter screenWriter, OutlineExtensionRenderer outlineExtensionRenderer) {
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
		this.outlineExtensionRenderer = outlineExtensionRenderer;

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

		//--------Render selected entities---------
		selectedEntitiesFrameBuffer.begin();
		boolean hasSelection = inWorldUIRenderer.renderEntityMasks(camera, gameContext);
		selectedEntitiesFrameBuffer.end();

		//-------Image processing pipeline---------
		TextureRegion overlay = null;
		if (hasSelection) {
			TextureRegion firstOutlinePass = outlineExtensionRenderer.outline(selectedEntitiesTextureRegion);
			TextureRegion secondOutlinePass = outlineExtensionRenderer.outline(firstOutlinePass);
			overlay = secondOutlinePass;
		}

		//-------Military orders-------------------
		militaryOrdersFrameBuffer.begin();
		militaryOrdersRenderer.render(gameContext, camera);
		militaryOrdersFrameBuffer.end();

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
					Gdx.graphics.getWidth() / 2.0f, Gdx.graphics.getHeight() / 2.0f);

			frameBufferSpriteBatch.draw(bumpMapTextureRegion, 0f, 0f,
					Gdx.graphics.getWidth() / 2.0f, Gdx.graphics.getHeight() / 2.0f);

			frameBufferSpriteBatch.draw(lightingTextureRegion, -Gdx.graphics.getWidth() / 2.0f, -Gdx.graphics.getHeight() / 2.0f,
					Gdx.graphics.getWidth() / 2.0f, Gdx.graphics.getHeight() / 2.0f);

			frameBufferSpriteBatch.draw(combinedTextureRegion, 0f, -Gdx.graphics.getHeight() / 2.0f,
					Gdx.graphics.getWidth() / 2.0f, Gdx.graphics.getHeight() / 2.0f);

			frameBufferSpriteBatch.end();
		} else if (renderingOptions.debug().getFrameBufferIndex() > 0 && renderingOptions.debug().getFrameBufferIndex() <= textureRegions.length) {
			//0 means render game normally
			int frameBufferIndex = renderingOptions.debug().getFrameBufferIndex();

			TextureRegion toRender = textureRegions[frameBufferIndex - 1];
			String textureRegionName = textureRegionNames[frameBufferIndex - 1];
			screenWriter.printLine(textureRegionName);

			combinedFrameBuffer.begin();
			combinedRenderer.renderFinal(diffuseTextureRegion, lightingTextureRegion, fadeAmount);
			combinedFrameBuffer.end();

			frameBufferSpriteBatch.begin();
			frameBufferSpriteBatch.disableBlending();
			frameBufferSpriteBatch.setProjectionMatrix(viewportCamera.projection);
			frameBufferSpriteBatch.draw(toRender, -Gdx.graphics.getWidth() / 2.0f, -Gdx.graphics.getHeight() / 2.0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
			frameBufferSpriteBatch.end();
		} else {
			combinedRenderer.renderFinal(diffuseTextureRegion, lightingTextureRegion, fadeAmount);
			if (overlay != null) {
				combinedRenderer.renderOverlay(overlay, selectedEntitiesTextureRegion);
			}
			combinedRenderer.renderTransparentOverlay(militaryOrdersTextureRegion);
			inWorldUIRenderer.render(gameContext, camera, particlesToRenderAsUI, diffuseSpriteCache);
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

		selectedEntitiesFrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false, false);
		selectedEntitiesTextureRegion = new TextureRegion(selectedEntitiesFrameBuffer.getColorBufferTexture(), width, height);
		selectedEntitiesTextureRegion.flip(false, true);

		outlineExtensionRenderer.initFrameBuffers(width, height);

		militaryOrdersFrameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, width, height, false, false);
		militaryOrdersTextureRegion = new TextureRegion(militaryOrdersFrameBuffer.getColorBufferTexture(), width, height);
		militaryOrdersTextureRegion.flip(false, true);

		int frameBufferCount = 8;
		textureRegions = new TextureRegion[frameBufferCount];
		textureRegions[0] = diffuseTextureRegion;
		textureRegions[1] = bumpMapTextureRegion;
		textureRegions[2] = lightingTextureRegion;
		textureRegions[3] = selectedEntitiesTextureRegion;

		textureRegions[4] = outlineExtensionRenderer.getFirstTextureRegion();
		textureRegions[5] = outlineExtensionRenderer.getSecondTextureRegion();
		textureRegions[6] = militaryOrdersTextureRegion;

		textureRegions[7] = combinedTextureRegion;

		textureRegionNames = new String[frameBufferCount];
		textureRegionNames[0] = "Diffuse Texture";
		textureRegionNames[1] = "Bump Map Texture";
		textureRegionNames[2] = "Lighting Texture";
		textureRegionNames[3] = "Selected Entities Texture";

		textureRegionNames[4] = "Outline Extension 1 Texture";
		textureRegionNames[5] = "Outline Extension 2 Texture";

		textureRegionNames[6] = "Military orders texture";

		textureRegionNames[7] = "Combined Texture";
	}

	private void disposeFrameBuffers() {
		diffuseFrameBuffer.dispose();
		normalMapFrameBuffer.dispose();
		lightingFrameBuffer.dispose();
		combinedFrameBuffer.dispose();
		outlineExtensionRenderer.dispose();
		militaryOrdersFrameBuffer.dispose();
		textureRegions = null;
	}

}
