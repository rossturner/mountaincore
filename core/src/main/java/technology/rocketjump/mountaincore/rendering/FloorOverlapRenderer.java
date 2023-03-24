package technology.rocketjump.mountaincore.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntArray;
import technology.rocketjump.mountaincore.assets.ChannelTypeDictionary;
import technology.rocketjump.mountaincore.assets.model.ChannelType;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.GameContextAware;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.mapping.tile.MapVertex;
import technology.rocketjump.mountaincore.mapping.tile.floor.FloorOverlap;
import technology.rocketjump.mountaincore.mapping.tile.floor.OverlapQuadrantDictionary;
import technology.rocketjump.mountaincore.mapping.tile.underground.ChannelLayout;
import technology.rocketjump.mountaincore.rendering.custom_libgdx.AlphaMaskSpriteBatch;
import technology.rocketjump.mountaincore.rendering.custom_libgdx.DualAlphaMaskSpriteBatch;
import technology.rocketjump.mountaincore.sprites.DiffuseTerrainSpriteCacheProvider;
import technology.rocketjump.mountaincore.sprites.MasksSpriteCache;
import technology.rocketjump.mountaincore.sprites.TerrainSpriteCache;
import technology.rocketjump.mountaincore.sprites.model.QuadrantSprites;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;

@Singleton
public class FloorOverlapRenderer implements Disposable, GameContextAware {

	public static final Color[] WHITE_ARRAY = {Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE};
	public static final Color[] WHITE_QUADRANT_COLORS = {Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE};
	private final AlphaMaskSpriteBatch alphaMaskSpriteBatch = new AlphaMaskSpriteBatch();
	private final DualAlphaMaskSpriteBatch dualAlphaMaskSpriteBatch = new DualAlphaMaskSpriteBatch();

	private final MasksSpriteCache masksSpriteCache;
	private final OverlapQuadrantDictionary overlapQuadrantDictionary;
	private final TerrainSpriteCache diffuseTerrainSpriteCache;
	private final ChannelType channelMaskType;
	private GameContext gameContext;

	@Inject
	public FloorOverlapRenderer(MasksSpriteCache masksSpriteCache, OverlapQuadrantDictionary overlapQuadrantDictionary,
								DiffuseTerrainSpriteCacheProvider diffuseTerrainSpriteCacheProvider, ChannelTypeDictionary channelTypeDictionary) {
		this.masksSpriteCache = masksSpriteCache;
		this.overlapQuadrantDictionary = overlapQuadrantDictionary;
		this.diffuseTerrainSpriteCache = diffuseTerrainSpriteCacheProvider.get();
		channelMaskType = channelTypeDictionary.getByName("channel_mask");
	}


	public void render(List<MapTile> tilesToRender, OrthographicCamera camera, RenderMode renderMode, TerrainSpriteCache spriteCache, TerrainRenderer.FloorSource floorSource) {
		alphaMaskSpriteBatch.setProjectionMatrix(camera.combined);
		alphaMaskSpriteBatch.begin();
		alphaMaskSpriteBatch.setColor(Color.WHITE);

		for (MapTile mapTile : tilesToRender) {
			final List<FloorOverlap> toRender;
			//TODO: hate this design
			BiConsumer<FloorOverlap, AlphaMaskSpriteBatch> quadrant0Adjuster = (floorOverlap, alphaMaskSpriteBatch) -> { };
			BiConsumer<FloorOverlap, AlphaMaskSpriteBatch> quadrant1Adjuster = (floorOverlap, alphaMaskSpriteBatch) -> { };
			BiConsumer<FloorOverlap, AlphaMaskSpriteBatch> quadrant2Adjuster = (floorOverlap, alphaMaskSpriteBatch) -> { };
			BiConsumer<FloorOverlap, AlphaMaskSpriteBatch> quadrant3Adjuster = (floorOverlap, alphaMaskSpriteBatch) -> { };


			if (floorSource == TerrainRenderer.FloorSource.ACTUAL) {
				toRender = mapTile.getActualFloor().getOverlaps();
			} else if (floorSource == TerrainRenderer.FloorSource.TRANSITORY) {
				toRender = mapTile.getActualFloor().getTransitoryOverlaps();
				MapVertex[] vertices =  gameContext.getAreaMap().getVertices(mapTile.getTileX(), mapTile.getTileY());
				float bottomLeft = vertices[0].getTransitoryFloorAlpha();
				float topLeft = vertices[1].getTransitoryFloorAlpha();
				float topRight = vertices[2].getTransitoryFloorAlpha();
				float bottomRight = vertices[3].getTransitoryFloorAlpha();

				float leftMiddle = (topLeft + bottomLeft)/2.0f;
				float topMiddle = (topLeft + topRight)/2.0f;
				float rightMiddle = (topRight + bottomRight)/2.0f;
				float bottomMiddle = (bottomLeft + bottomRight)/2.0f;

				float middle = (bottomLeft + bottomRight + topLeft + topRight) / 4.0f;


				quadrant0Adjuster = (floorOverlap, alphaMaskSpriteBatch) -> {
					Color[] vertexColors = floorOverlap.getVertexColors();
					vertexColors[0].a = leftMiddle;
					vertexColors[1].a = topLeft;
					vertexColors[2].a = topMiddle;
					vertexColors[3].a = middle;
					alphaMaskSpriteBatch.setColors(vertexColors);
				};

				quadrant1Adjuster = (floorOverlap, alphaMaskSpriteBatch) -> {
					Color[] vertexColors = floorOverlap.getVertexColors();
					vertexColors[0].a = middle;
					vertexColors[1].a = topMiddle;
					vertexColors[2].a = topRight;
					vertexColors[3].a = rightMiddle;
					alphaMaskSpriteBatch.setColors(vertexColors);
				};

				quadrant2Adjuster = (floorOverlap, alphaMaskSpriteBatch) -> {
					Color[] vertexColors = floorOverlap.getVertexColors();
					vertexColors[0].a = bottomLeft;
					vertexColors[1].a = leftMiddle;
					vertexColors[2].a = middle;
					vertexColors[3].a = bottomMiddle;
					alphaMaskSpriteBatch.setColors(vertexColors);
				};

				quadrant3Adjuster = (floorOverlap, alphaMaskSpriteBatch) -> {
					Color[] vertexColors = floorOverlap.getVertexColors();
					vertexColors[0].a = bottomMiddle;
					vertexColors[1].a = middle;
					vertexColors[2].a = rightMiddle;
					vertexColors[3].a = bottomRight;
					alphaMaskSpriteBatch.setColors(vertexColors);
				};

			} else {
				toRender = Collections.emptyList();
			}

			if (!toRender.isEmpty()) {
				for (FloorOverlap floorOverlap : toRender) {
					IntArray overlapQuadrants = overlapQuadrantDictionary.getOverlapQuadrants(floorOverlap.getLayout().getId());
					QuadrantSprites quadrantAlphaSprites = masksSpriteCache.getMasksForOverlap(floorOverlap.getFloorType().getOverlapType(), floorOverlap.getLayout(), mapTile.getSeed());
					Sprite overlapSprite = spriteCache.getFloorSpriteForType(floorOverlap.getFloorType(), mapTile.getSeed());

					if (renderMode.equals(RenderMode.DIFFUSE)) {
						alphaMaskSpriteBatch.setColors(floorOverlap.getVertexColors());
					} else {
						alphaMaskSpriteBatch.setColors(WHITE_QUADRANT_COLORS);
					}

					//TODO: optimize me so don't need to run in actual floor
					if (overlapQuadrants.get(0) != 0) {
						quadrant0Adjuster.accept(floorOverlap, alphaMaskSpriteBatch);
						alphaMaskSpriteBatch.draw(overlapSprite, quadrantAlphaSprites.getA(), mapTile.getTileX(), mapTile.getTileY(), 0, 0.5f, 0.5f, 0.5f);
					}
					if (overlapQuadrants.get(1) != 0) {
						quadrant1Adjuster.accept(floorOverlap, alphaMaskSpriteBatch);
						alphaMaskSpriteBatch.draw(overlapSprite, quadrantAlphaSprites.getB(), mapTile.getTileX(), mapTile.getTileY(), 0.5f, 0.5f, 0.5f, 0.5f);
					}
					if (overlapQuadrants.get(2) != 0) {
						quadrant2Adjuster.accept(floorOverlap, alphaMaskSpriteBatch);
						alphaMaskSpriteBatch.draw(overlapSprite, quadrantAlphaSprites.getC(), mapTile.getTileX(), mapTile.getTileY(), 0, 0f, 0.5f, 0.5f);
					}
					if (overlapQuadrants.get(3) != 0) {
						quadrant3Adjuster.accept(floorOverlap, alphaMaskSpriteBatch);
						alphaMaskSpriteBatch.draw(overlapSprite, quadrantAlphaSprites.getD(), mapTile.getTileX(), mapTile.getTileY(), 0.5f, 0f, 0.5f, 0.5f);
					}
				}

			}
		}

		alphaMaskSpriteBatch.end();
	}




	public void renderWithChannelMasks(List<MapTile> tilesToRender, OrthographicCamera camera, RenderMode renderMode, TerrainSpriteCache spriteCache) {
		if (renderMode.equals(RenderMode.NORMALS)) {
			// TODO FIXME Currently an issue when rendering normals where diffuse render ends up white with wrong texture when camera is in certain position
			// Could look to move channel masks to masks spritesheet rather than diffuse terrain spritesheet
			return;
		}
		dualAlphaMaskSpriteBatch.setProjectionMatrix(camera.combined);
		dualAlphaMaskSpriteBatch.begin();

		for (MapTile mapTile : tilesToRender) {
			if (!mapTile.getFloor().getOverlaps().isEmpty()) {

				if (mapTile.getUnderTile() == null || mapTile.getUnderTile().getChannelLayout() == null) {
					continue;
				}

				ChannelLayout channelLayout = mapTile.getUnderTile().getChannelLayout();
				QuadrantSprites quadrantChannelMaskSprites = diffuseTerrainSpriteCache.getSpritesForChannel(channelMaskType, channelLayout, mapTile.getSeed());

				for (FloorOverlap floorOverlap : mapTile.getFloor().getOverlaps()) {
					IntArray overlapQuadrants = overlapQuadrantDictionary.getOverlapQuadrants(floorOverlap.getLayout().getId());
					QuadrantSprites quadrantAlphaSprites = masksSpriteCache.getMasksForOverlap(floorOverlap.getFloorType().getOverlapType(), floorOverlap.getLayout(), mapTile.getSeed());
					Sprite overlapSprite = spriteCache.getFloorSpriteForType(floorOverlap.getFloorType(), mapTile.getSeed());

					if (renderMode.equals(RenderMode.DIFFUSE)) {
						Color[] vertexColors = floorOverlap.getVertexColors();
						dualAlphaMaskSpriteBatch.setColors(vertexColors);
					} else {
						dualAlphaMaskSpriteBatch.setColors(WHITE_ARRAY);
					}

					if (overlapQuadrants.get(0) != 0) {
						dualAlphaMaskSpriteBatch.draw(overlapSprite, quadrantAlphaSprites.getA(), quadrantChannelMaskSprites.getA(),
								mapTile.getTileX(), mapTile.getTileY(), 0, 0.5f, 0.5f, 0.5f);
					}
					if (overlapQuadrants.get(1) != 0) {
						dualAlphaMaskSpriteBatch.draw(overlapSprite, quadrantAlphaSprites.getB(), quadrantChannelMaskSprites.getB(),
								mapTile.getTileX(), mapTile.getTileY(), 0.5f, 0.5f, 0.5f, 0.5f);
					}
					if (overlapQuadrants.get(2) != 0) {
						dualAlphaMaskSpriteBatch.draw(overlapSprite, quadrantAlphaSprites.getC(), quadrantChannelMaskSprites.getC(),
								mapTile.getTileX(), mapTile.getTileY(), 0, 0f, 0.5f, 0.5f);
					}
					if (overlapQuadrants.get(3) != 0) {
						dualAlphaMaskSpriteBatch.draw(overlapSprite, quadrantAlphaSprites.getD(), quadrantChannelMaskSprites.getD(),
								mapTile.getTileX(), mapTile.getTileY(), 0.5f, 0f, 0.5f, 0.5f);
					}
				}

			}
		}

		dualAlphaMaskSpriteBatch.end();
	}

	@Override
	public void dispose() {
		alphaMaskSpriteBatch.dispose();
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
