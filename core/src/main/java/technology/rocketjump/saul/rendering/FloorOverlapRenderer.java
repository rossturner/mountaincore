package technology.rocketjump.saul.rendering;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.IntArray;
import technology.rocketjump.saul.assets.ChannelTypeDictionary;
import technology.rocketjump.saul.assets.model.ChannelType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.mapping.tile.MapVertex;
import technology.rocketjump.saul.mapping.tile.floor.FloorOverlap;
import technology.rocketjump.saul.mapping.tile.floor.OverlapQuadrantDictionary;
import technology.rocketjump.saul.mapping.tile.underground.ChannelLayout;
import technology.rocketjump.saul.rendering.custom_libgdx.AlphaMaskSpriteBatch;
import technology.rocketjump.saul.rendering.custom_libgdx.DualAlphaMaskSpriteBatch;
import technology.rocketjump.saul.sprites.DiffuseTerrainSpriteCacheProvider;
import technology.rocketjump.saul.sprites.MasksSpriteCache;
import technology.rocketjump.saul.sprites.TerrainSpriteCache;
import technology.rocketjump.saul.sprites.model.QuadrantSprites;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;

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
			MapVertex[] vertices = gameContext.getAreaMap().getVertices(mapTile.getTileX(), mapTile.getTileY());
			List<FloorOverlap> toRender;


			if (floorSource == TerrainRenderer.FloorSource.ACTUAL) {
				toRender = mapTile.getActualFloor().getOverlaps();
			} else if (floorSource == TerrainRenderer.FloorSource.TRANSITORY) {
				toRender = mapTile.getActualFloor().getTransitoryOverlaps();
			} else {
				toRender = Collections.emptyList();
			}

			if (!toRender.isEmpty()) {
				for (FloorOverlap floorOverlap : toRender) {
					IntArray overlapQuadrants = overlapQuadrantDictionary.getOverlapQuadrants(floorOverlap.getLayout().getId());
					QuadrantSprites quadrantAlphaSprites = masksSpriteCache.getMasksForOverlap(floorOverlap.getFloorType().getOverlapType(), floorOverlap.getLayout(), mapTile.getSeed());
					Sprite overlapSprite = spriteCache.getFloorSpriteForType(floorOverlap.getFloorType(), mapTile.getSeed());

					Color[] vertexColors = floorOverlap.getVertexColors();
					if (renderMode.equals(RenderMode.DIFFUSE)) {
						alphaMaskSpriteBatch.setColors(vertexColors);
					} else {
						alphaMaskSpriteBatch.setColors(WHITE_QUADRANT_COLORS);
					}



					float bottomLeft = vertices[0].getTransitoryFloorAlpha();
					float topLeft = vertices[1].getTransitoryFloorAlpha();
					float topRight = vertices[2].getTransitoryFloorAlpha();
					float bottomRight = vertices[3].getTransitoryFloorAlpha();

					float leftMiddle = (topLeft + bottomLeft)/2.0f;
					float topMiddle = (topLeft + topRight)/2.0f;
					float rightMiddle = (topRight + bottomRight)/2.0f;
					float bottomMiddle = (bottomLeft + bottomRight)/2.0f;

					float middle = (bottomLeft + bottomRight + topLeft + topRight) / 4.0f;

					if (floorSource == TerrainRenderer.FloorSource.ACTUAL) {
						bottomLeft = 1;
						topLeft = 1;
						topRight = 1;
						bottomRight = 1;
						leftMiddle = 1;
						topMiddle = 1;
						rightMiddle = 1;
						bottomMiddle = 1;
						middle = 1;
					}

					if (overlapQuadrants.get(0) != 0) {
						vertexColors[0].a = leftMiddle;
						vertexColors[1].a = topLeft;
						vertexColors[2].a = topMiddle;
						vertexColors[3].a = middle;

						alphaMaskSpriteBatch.draw(overlapSprite, quadrantAlphaSprites.getA(), mapTile.getTileX(), mapTile.getTileY(), 0, 0.5f, 0.5f, 0.5f);
					}
					if (overlapQuadrants.get(1) != 0) {
						vertexColors[0].a = middle;
						vertexColors[1].a = topMiddle;
						vertexColors[2].a = topRight;
						vertexColors[3].a = rightMiddle;

						alphaMaskSpriteBatch.draw(overlapSprite, quadrantAlphaSprites.getB(), mapTile.getTileX(), mapTile.getTileY(), 0.5f, 0.5f, 0.5f, 0.5f);
					}
					if (overlapQuadrants.get(2) != 0) {
						vertexColors[0].a = bottomLeft;
						vertexColors[1].a = leftMiddle;
						vertexColors[2].a = middle;
						vertexColors[3].a = bottomMiddle;

						alphaMaskSpriteBatch.draw(overlapSprite, quadrantAlphaSprites.getC(), mapTile.getTileX(), mapTile.getTileY(), 0, 0f, 0.5f, 0.5f);
					}
					if (overlapQuadrants.get(3) != 0) {
						vertexColors[0].a = bottomMiddle;
						vertexColors[1].a = middle;
						vertexColors[2].a = rightMiddle;
						vertexColors[3].a = bottomRight;
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
