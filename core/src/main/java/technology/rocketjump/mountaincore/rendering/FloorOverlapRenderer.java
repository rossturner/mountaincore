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
import technology.rocketjump.mountaincore.mapping.tile.floor.FloorOverlap;
import technology.rocketjump.mountaincore.mapping.tile.floor.OverlapQuadrantDictionary;
import technology.rocketjump.mountaincore.mapping.tile.floor.TileFloor;
import technology.rocketjump.mountaincore.mapping.tile.underground.ChannelLayout;
import technology.rocketjump.mountaincore.rendering.custom_libgdx.AlphaMaskSpriteBatch;
import technology.rocketjump.mountaincore.rendering.custom_libgdx.DualAlphaMaskSpriteBatch;
import technology.rocketjump.mountaincore.rendering.utils.TransitoryFloorQuadrantCalculator;
import technology.rocketjump.mountaincore.sprites.DiffuseTerrainSpriteCacheProvider;
import technology.rocketjump.mountaincore.sprites.MasksSpriteCache;
import technology.rocketjump.mountaincore.sprites.TerrainSpriteCache;
import technology.rocketjump.mountaincore.sprites.model.QuadrantSprites;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;

@Singleton
public class FloorOverlapRenderer implements Disposable, GameContextAware {

	public static final Color[] WHITE_ARRAY = {Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE};
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
			final TransitoryFloorQuadrantCalculator transitoryFloorQuadrantCalculator;

			if (floorSource == TerrainRenderer.FloorSource.ACTUAL) {
				toRender = mapTile.getActualFloor().getOverlaps();
			} else if (floorSource == TerrainRenderer.FloorSource.TRANSITORY) {
				toRender = mapTile.getActualFloor().getTransitoryOverlaps();
			} else {
				toRender = Collections.emptyList();
			}

			if (renderMode == RenderMode.NORMALS) {
				transitoryFloorQuadrantCalculator = TransitoryFloorQuadrantCalculator.NORMAL_RENDER_MODE_CALCULATOR;
			} else if (floorSource == TerrainRenderer.FloorSource.TRANSITORY) {
				transitoryFloorQuadrantCalculator = new TransitoryFloorQuadrantCalculator(mapTile, gameContext);
			} else {
				transitoryFloorQuadrantCalculator = TransitoryFloorQuadrantCalculator.NULL_CALCULATOR;
			}

			for (FloorOverlap floorOverlap : toRender) {
				IntArray overlapQuadrants = overlapQuadrantDictionary.getOverlapQuadrants(floorOverlap.getLayout().getId());
				QuadrantSprites quadrantAlphaSprites = masksSpriteCache.getMasksForOverlap(floorOverlap.getFloorType().getOverlapType(), floorOverlap.getLayout(), mapTile.getSeed());
				Sprite overlapSprite = spriteCache.getFloorSpriteForType(floorOverlap.getFloorType(), mapTile.getSeed());

				//TODO: optimize me so don't need to run in actual floor
				if (overlapQuadrants.get(0) != 0) {
					alphaMaskSpriteBatch.setColors(transitoryFloorQuadrantCalculator.adjustAlpha(floorOverlap, TransitoryFloorQuadrantCalculator.Quadrant.ZERO));
					alphaMaskSpriteBatch.draw(overlapSprite, quadrantAlphaSprites.getA(), mapTile.getTileX(), mapTile.getTileY(), 0, 0.5f, 0.5f, 0.5f);
				}
				if (overlapQuadrants.get(1) != 0) {
					alphaMaskSpriteBatch.setColors(transitoryFloorQuadrantCalculator.adjustAlpha(floorOverlap, TransitoryFloorQuadrantCalculator.Quadrant.ONE));
					alphaMaskSpriteBatch.draw(overlapSprite, quadrantAlphaSprites.getB(), mapTile.getTileX(), mapTile.getTileY(), 0.5f, 0.5f, 0.5f, 0.5f);
				}
				if (overlapQuadrants.get(2) != 0) {
					alphaMaskSpriteBatch.setColors(transitoryFloorQuadrantCalculator.adjustAlpha(floorOverlap, TransitoryFloorQuadrantCalculator.Quadrant.TWO));
					alphaMaskSpriteBatch.draw(overlapSprite, quadrantAlphaSprites.getC(), mapTile.getTileX(), mapTile.getTileY(), 0, 0f, 0.5f, 0.5f);
				}
				if (overlapQuadrants.get(3) != 0) {
					alphaMaskSpriteBatch.setColors(transitoryFloorQuadrantCalculator.adjustAlpha(floorOverlap, TransitoryFloorQuadrantCalculator.Quadrant.THREE));
					alphaMaskSpriteBatch.draw(overlapSprite, quadrantAlphaSprites.getD(), mapTile.getTileX(), mapTile.getTileY(), 0.5f, 0f, 0.5f, 0.5f);
				}
			}

		}

		alphaMaskSpriteBatch.end();
	}




	public void renderWithChannelMasks(List<MapTile> tilesToRender, OrthographicCamera camera, RenderMode renderMode, TerrainSpriteCache spriteCache, TerrainRenderer.FloorSource floorSource) {
		if (renderMode.equals(RenderMode.NORMALS)) {
			// TODO FIXME Currently an issue when rendering normals where diffuse render ends up white with wrong texture when camera is in certain position
			// Could look to move channel masks to masks spritesheet rather than diffuse terrain spritesheet
			return;
		}
		dualAlphaMaskSpriteBatch.setProjectionMatrix(camera.combined);
		dualAlphaMaskSpriteBatch.begin();

		for (MapTile mapTile : tilesToRender) {
			TileFloor floor = mapTile.getActualFloor();
			final List<FloorOverlap> toRender;
			final TransitoryFloorQuadrantCalculator transitoryFloorQuadrantCalculator;

			if (floorSource == TerrainRenderer.FloorSource.ACTUAL) {
				toRender = floor.getOverlaps();
			} else if (floorSource == TerrainRenderer.FloorSource.TRANSITORY) {
				toRender = floor.getTransitoryOverlaps();
			} else {
				toRender = Collections.emptyList();
			}


			if (renderMode == RenderMode.NORMALS) {
				transitoryFloorQuadrantCalculator = TransitoryFloorQuadrantCalculator.NORMAL_RENDER_MODE_CALCULATOR;
			} else if (floorSource == TerrainRenderer.FloorSource.TRANSITORY) {
				transitoryFloorQuadrantCalculator = new TransitoryFloorQuadrantCalculator(mapTile, gameContext);
			} else {
				transitoryFloorQuadrantCalculator = TransitoryFloorQuadrantCalculator.NULL_CALCULATOR;
			}

			if (!toRender.isEmpty()) {

				if (mapTile.getUnderTile() == null || mapTile.getUnderTile().getChannelLayout() == null) {
					continue;
				}

				ChannelLayout channelLayout = mapTile.getUnderTile().getChannelLayout();
				QuadrantSprites quadrantChannelMaskSprites = diffuseTerrainSpriteCache.getSpritesForChannel(channelMaskType, channelLayout, mapTile.getSeed());

				for (FloorOverlap floorOverlap : toRender) {
					IntArray overlapQuadrants = overlapQuadrantDictionary.getOverlapQuadrants(floorOverlap.getLayout().getId());
					QuadrantSprites quadrantAlphaSprites = masksSpriteCache.getMasksForOverlap(floorOverlap.getFloorType().getOverlapType(), floorOverlap.getLayout(), mapTile.getSeed());
					Sprite overlapSprite = spriteCache.getFloorSpriteForType(floorOverlap.getFloorType(), mapTile.getSeed());

					//TODO: Fix normal mode rendering to be constant white
					if (overlapQuadrants.get(0) != 0) {
						dualAlphaMaskSpriteBatch.setColors(transitoryFloorQuadrantCalculator.adjustAlpha(floorOverlap, TransitoryFloorQuadrantCalculator.Quadrant.ZERO));
						dualAlphaMaskSpriteBatch.draw(overlapSprite, quadrantAlphaSprites.getA(), quadrantChannelMaskSprites.getA(),
								mapTile.getTileX(), mapTile.getTileY(), 0, 0.5f, 0.5f, 0.5f);
					}
					if (overlapQuadrants.get(1) != 0) {
						dualAlphaMaskSpriteBatch.setColors(transitoryFloorQuadrantCalculator.adjustAlpha(floorOverlap, TransitoryFloorQuadrantCalculator.Quadrant.ONE));
						dualAlphaMaskSpriteBatch.draw(overlapSprite, quadrantAlphaSprites.getB(), quadrantChannelMaskSprites.getB(),
								mapTile.getTileX(), mapTile.getTileY(), 0.5f, 0.5f, 0.5f, 0.5f);
					}
					if (overlapQuadrants.get(2) != 0) {
						dualAlphaMaskSpriteBatch.setColors(transitoryFloorQuadrantCalculator.adjustAlpha(floorOverlap, TransitoryFloorQuadrantCalculator.Quadrant.TWO));
						dualAlphaMaskSpriteBatch.draw(overlapSprite, quadrantAlphaSprites.getC(), quadrantChannelMaskSprites.getC(),
								mapTile.getTileX(), mapTile.getTileY(), 0, 0f, 0.5f, 0.5f);
					}
					if (overlapQuadrants.get(3) != 0) {
						dualAlphaMaskSpriteBatch.setColors(transitoryFloorQuadrantCalculator.adjustAlpha(floorOverlap, TransitoryFloorQuadrantCalculator.Quadrant.THREE));
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
