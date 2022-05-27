package technology.rocketjump.saul.mapping.tile.underground;

import technology.rocketjump.saul.mapping.tile.TileNeighbours;
import technology.rocketjump.saul.mapping.tile.layout.TileLayout;

public class ChannelLayout extends TileLayout {

	public ChannelLayout(TileNeighbours neighbours) {
		super(neighbours, (tile, direction) ->
				tile.hasChannel()
		);
	}

	public ChannelLayout(int id) {
		super(id);
	}
}
