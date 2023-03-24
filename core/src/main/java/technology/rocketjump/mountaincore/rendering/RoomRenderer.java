package technology.rocketjump.mountaincore.rendering;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.google.inject.Inject;
import technology.rocketjump.mountaincore.assets.RoomEdgeTypeDictionary;
import technology.rocketjump.mountaincore.assets.WallQuadrantDictionary;
import technology.rocketjump.mountaincore.assets.model.RoomEdgeType;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.mapping.tile.layout.RoomTileLayout;
import technology.rocketjump.mountaincore.rooms.Room;
import technology.rocketjump.mountaincore.sprites.TerrainSpriteCache;
import technology.rocketjump.mountaincore.sprites.model.QuadrantSprites;

public class RoomRenderer {

	public static final float TILE_MIDPOINT_DISTANCE = 0.5f;
	private final WallQuadrantDictionary quadrantDictionary;
	private final RoomEdgeTypeDictionary roomEdgeTypeDictionary;

	private final RoomEdgeType roughEdgeType;
	private final RoomEdgeType straightEdgeType;

	@Inject
	public RoomRenderer(WallQuadrantDictionary quadrantDictionary, RoomEdgeTypeDictionary roomEdgeTypeDictionary) {
		this.quadrantDictionary = quadrantDictionary;
		this.roomEdgeTypeDictionary = roomEdgeTypeDictionary;

		// MODDING expose this
		roughEdgeType = roomEdgeTypeDictionary.getByName("soft");
		straightEdgeType = roomEdgeTypeDictionary.getByName("straight");
	}

	public void render(MapTile mapTile, Batch spriteBatch, TerrainSpriteCache spriteCache) {
		Room room = mapTile.getRoomTile().getRoom();
		spriteBatch.setColor(room.getBorderColor());

		RoomTileLayout layout = mapTile.getRoomTile().getLayout();
		RoomEdgeType roomEdgeType = mapTile.getFloor().getFloorType().isConstructed() ? straightEdgeType : roughEdgeType;
		QuadrantSprites quadrantSprites = spriteCache.getSpritesForRoomEdge(roomEdgeType, layout);

		float worldX = mapTile.getTileX();
		float worldY = mapTile.getTileY();

		// Quadrant A
		if (quadrantSprites.getA() != null) {
			spriteBatch.draw(
					quadrantSprites.getA().getTexture(), // texture
					worldX,
					worldY + TILE_MIDPOINT_DISTANCE,
					TILE_MIDPOINT_DISTANCE / 2, // midpointX relative to world position
					TILE_MIDPOINT_DISTANCE / 2, // midpointY relative to world position
					TILE_MIDPOINT_DISTANCE, // width in world units
					TILE_MIDPOINT_DISTANCE, // height in world units
					quadrantSprites.getA().getScaleX(),
					quadrantSprites.getA().getScaleY(),
					0,
					quadrantSprites.getA().getRegionX(),
					quadrantSprites.getA().getRegionY(),
					quadrantSprites.getA().getRegionWidth() - 32, // src image width
					quadrantSprites.getA().getRegionHeight() - 32, // src image height
					false,
					false);
		}

		// Quadrant B
		if (quadrantSprites.getB() != null) {
			spriteBatch.draw(
					quadrantSprites.getB().getTexture(), // texture
					worldX + TILE_MIDPOINT_DISTANCE,
					worldY + TILE_MIDPOINT_DISTANCE,
					TILE_MIDPOINT_DISTANCE / 2, // midpointX relative to world position
					TILE_MIDPOINT_DISTANCE / 2, // midpointY relative to world position
					TILE_MIDPOINT_DISTANCE, // width in world units
					TILE_MIDPOINT_DISTANCE, // height in world units
					quadrantSprites.getB().getScaleX(),
					quadrantSprites.getB().getScaleY(),
					0,
					quadrantSprites.getB().getRegionX() + 32,
					quadrantSprites.getB().getRegionY(),
					quadrantSprites.getB().getRegionWidth() - 32, // src image width
					quadrantSprites.getB().getRegionHeight() - 32, // src image height
					false,
					false);
		}

		// Quadrant C
		if (quadrantSprites.getC() != null) {
			spriteBatch.draw(
					quadrantSprites.getC().getTexture(), // texture
					worldX,
					worldY,
					TILE_MIDPOINT_DISTANCE / 2, // midpointX relative to world position
					TILE_MIDPOINT_DISTANCE / 2, // midpointY relative to world position
					TILE_MIDPOINT_DISTANCE, // width in world units
					TILE_MIDPOINT_DISTANCE, // height in world units
					quadrantSprites.getC().getScaleX(),
					quadrantSprites.getC().getScaleY(),
					0,
					quadrantSprites.getC().getRegionX(),
					quadrantSprites.getC().getRegionY() + 32,
					quadrantSprites.getC().getRegionWidth() - 32, // src image width
					quadrantSprites.getC().getRegionHeight() - 32, // src image height
					false,
					false);
		}

		// Quadrant D
		if (quadrantSprites.getD() != null) {
			spriteBatch.draw(
					quadrantSprites.getD().getTexture(), // texture
					worldX + TILE_MIDPOINT_DISTANCE,
					worldY,
					TILE_MIDPOINT_DISTANCE / 2, // midpointX relative to world position
					TILE_MIDPOINT_DISTANCE / 2, // midpointY relative to world position
					TILE_MIDPOINT_DISTANCE, // width in world units
					TILE_MIDPOINT_DISTANCE, // height in world units
					quadrantSprites.getD().getScaleX(),
					quadrantSprites.getD().getScaleY(),
					0,
					quadrantSprites.getD().getRegionX() + 32,
					quadrantSprites.getD().getRegionY() + 32,
					quadrantSprites.getD().getRegionWidth() - 32, // src image width
					quadrantSprites.getD().getRegionHeight() - 32, // src image height
					false,
					false);
		}


	}

}
