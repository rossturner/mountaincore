package technology.rocketjump.mountaincore.mapping.tile;

import java.util.EnumMap;

public class TileNeighbours extends EnumMap<CompassDirection, MapTile> {

    public TileNeighbours() {
        super(CompassDirection.class);
    }

}
