package technology.rocketjump.mountaincore.mapping.tile.layout;

import technology.rocketjump.mountaincore.mapping.tile.TileNeighbours;
import technology.rocketjump.mountaincore.rooms.constructions.ConstructionType;

public class WallConstructionLayout extends TileLayout {

    public WallConstructionLayout(TileNeighbours neighbours) {
        super(neighbours, (tile, direction) ->
                tile.hasWall() || (tile.hasDoorway() && direction.getYOffset() >= 0) || // Don't count doorways below this Y level as they don't have North wall caps
                // Also check for wall or doorway constructions as these affect construction layout, but not real wall layout
                (tile.hasConstruction() && tile.getConstruction().getConstructionType().equals(ConstructionType.WALL_CONSTRUCTION)) ||
                (tile.hasConstruction() && tile.getConstruction().getConstructionType().equals(ConstructionType.DOORWAY_CONSTRUCTION) && direction.getYOffset() >= 0)
        );
    }

    public WallConstructionLayout(int id) {
        super(id);
    }

}
