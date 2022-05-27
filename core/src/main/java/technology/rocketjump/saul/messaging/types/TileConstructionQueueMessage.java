package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.mapping.tile.MapTile;

public class TileConstructionQueueMessage {
	public final MapTile parentTile;
	public final boolean constructionQueued;

	public TileConstructionQueueMessage(MapTile parentTile, boolean constructionQueued) {
		this.parentTile = parentTile;
		this.constructionQueued = constructionQueued;
	}
}
