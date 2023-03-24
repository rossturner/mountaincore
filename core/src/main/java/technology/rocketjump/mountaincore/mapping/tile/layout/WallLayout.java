package technology.rocketjump.mountaincore.mapping.tile.layout;

import technology.rocketjump.mountaincore.mapping.tile.TileNeighbours;

public class WallLayout extends TileLayout {

    public WallLayout(TileNeighbours neighbours) {
        super(neighbours, (tile, direction) ->
                tile.hasWall() || tile.hasDoorway()
        );
    }

    public WallLayout(int id) {
        super(id);
    }

    public static TileLayout fromString(String diagram) {
        return fromString(diagram, (tile, direction) -> tile.hasWall());
    }

}
