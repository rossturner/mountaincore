package technology.rocketjump.mountaincore.mapping.tile.layout;

import technology.rocketjump.mountaincore.assets.model.FloorType;
import technology.rocketjump.mountaincore.assets.model.OverlapType;
import technology.rocketjump.mountaincore.assets.model.WallType;
import technology.rocketjump.mountaincore.mapping.tile.CompassDirection;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.mapping.tile.TileNeighbours;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;

import java.util.Map;

/**
 * For lighting and shadows to work we need the "edges" that make up the outside edges of the wall
 * and the edges of the "top" of the wall. As a lightsource shines on a wall, it will ignore outside edges
 * which are "facing" the light source. Therefore, if we use a specific winding (libgdx uses clockwise winding)
 * for describing the outside edges of a wall layout, those facing a provided lightsource are disregarded.
 * All of the "top" edges are counted. See http://gamedev.stackexchange.com/questions/115125/where-to-cast-light-shadows-in-a-2-5d-view
 *
 * ^ I don't think this doc comment should live here anymore, probably relates to lighting calculations for specific tile layouts
 */
public class TileLayout {


	public static final char WALL_CHARACTER = 'X';
	public static final char SPACE_CHARACTER = '.';
	private static final GameMaterial MOCK_MATERIAL = new GameMaterial("brick", 1, GameMaterialType.OTHER);
	private static final FloorType MOCK_FLOOR_DEFINITION = new FloorType("mock floor definition", null,
			-1L, GameMaterialType.OTHER, 0, 0, new OverlapType("mock overlap type"), false, 1f, null, null, null, null, null);
	private static final WallType MOCK_WALL_TYPE = new WallType("Mock wall type", "TEST.I18NKEY", 0L, GameMaterialType.OTHER, false, null,null, null, null);

	private final int id;
	private final LayoutCheck layoutCheck;

	public interface LayoutCheck {
		boolean doesNeighbourAffectLayout(MapTile tile, CompassDirection direction);
	}

	public TileLayout(TileNeighbours neighbours, LayoutCheck layoutCheck) {
		int wallLayoutId = 0;
		for (Map.Entry<CompassDirection, MapTile> entry : neighbours.entrySet()) {
			if (entry.getValue() != null && layoutCheck.doesNeighbourAffectLayout(entry.getValue(), entry.getKey())) {
				wallLayoutId |= entry.getKey().getBinaryMask();
			}
		}
		this.id = wallLayoutId;
		this.layoutCheck = layoutCheck;
	}

	public TileLayout(int id) {
		this.id = id;
		this.layoutCheck = (tile, direction) -> false;
	}

	public static TileLayout fromString(String diagram, LayoutCheck layoutCheck) {
		diagram = diagram.replaceAll("\n", "");
		TileNeighbours neighbours = new TileNeighbours();

		for (CompassDirection compassDirection : CompassDirection.values()) {
			neighbours.put(compassDirection, new MapTile(0L, compassDirection.getXOffset(), compassDirection.getYOffset(), MOCK_FLOOR_DEFINITION, MOCK_MATERIAL));
			setIfWallInDirection(compassDirection, diagram, neighbours);
		}

		return new TileLayout(neighbours, (tile, direction) -> tile.hasWall());
	}

	private static void setIfWallInDirection(CompassDirection direction, String diagram, TileNeighbours neighbours) {
		if (wallAtIndex(direction.getIndex(), diagram)) {
			neighbours.get(direction).addWall(new TileNeighbours(), MOCK_MATERIAL, MOCK_WALL_TYPE);
		}
	}

	private static boolean wallAtIndex(int index, String diagram) {
		return diagram.charAt(index) == WALL_CHARACTER;
	}

	public TileLayout reduceToMeaningfulForm() {
		char[] characters = this.toString().replaceAll("\n", "").toCharArray();

		emptyCornerIfEitherSiblingIsEmpty(characters, CompassDirection.NORTH_WEST, CompassDirection.NORTH, CompassDirection.WEST);
		emptyCornerIfEitherSiblingIsEmpty(characters, CompassDirection.NORTH_EAST, CompassDirection.NORTH, CompassDirection.EAST);
		emptyCornerIfEitherSiblingIsEmpty(characters, CompassDirection.SOUTH_WEST, CompassDirection.SOUTH, CompassDirection.WEST);
		emptyCornerIfEitherSiblingIsEmpty(characters, CompassDirection.SOUTH_EAST, CompassDirection.SOUTH, CompassDirection.EAST);

		return TileLayout.fromString(String.valueOf(characters), layoutCheck);
	}

	private void emptyCornerIfEitherSiblingIsEmpty(char[] characters, CompassDirection corner,
												   CompassDirection sibling1, CompassDirection sibling2) {
		if (characters[sibling1.getIndex()] == SPACE_CHARACTER ||
				characters[sibling2.getIndex()] == SPACE_CHARACTER) {
			characters[corner.getIndex()] = SPACE_CHARACTER;
		}
	}

	public int getId() {
		return id;
	}

	@Override
	public String toString() {
		return topRow() + "\n" + middleRow() + "\n" + bottomRow();
	}

	private StringBuilder addWallCharacterIf(boolean isWall, StringBuilder builder) {
		if (isWall) {
			builder.append(WALL_CHARACTER);
		} else {
			builder.append(SPACE_CHARACTER);
		}
		return builder;
	}

	public boolean meaningfulTileInDirection(CompassDirection direction) {
		return (id & direction.getBinaryMask()) > 0;
	}

	public String topRow() {
		StringBuilder row = new StringBuilder();
		addWallCharacterIf(meaningfulTileInDirection(CompassDirection.NORTH_WEST), row);
		addWallCharacterIf(meaningfulTileInDirection(CompassDirection.NORTH), row);
		addWallCharacterIf(meaningfulTileInDirection(CompassDirection.NORTH_EAST), row);
		return row.toString();
	}

	public String middleRow() {
		StringBuilder row = new StringBuilder();
		addWallCharacterIf(meaningfulTileInDirection(CompassDirection.WEST), row);
		row.append(WALL_CHARACTER); // Always a wall in the middle
		addWallCharacterIf(meaningfulTileInDirection(CompassDirection.EAST), row);
		return row.toString();
	}

	public String bottomRow() {
		StringBuilder row = new StringBuilder();
		addWallCharacterIf(meaningfulTileInDirection(CompassDirection.SOUTH_WEST), row);
		addWallCharacterIf(meaningfulTileInDirection(CompassDirection.SOUTH), row);
		addWallCharacterIf(meaningfulTileInDirection(CompassDirection.SOUTH_EAST), row);
		return row.toString();
	}

}
