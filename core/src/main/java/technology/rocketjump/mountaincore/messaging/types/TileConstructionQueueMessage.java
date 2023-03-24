package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.mapping.tile.MapTile;

public class TileConstructionQueueMessage {
	public final MapTile parentTile;
	public final boolean constructionQueued;

	public TileConstructionQueueMessage(MapTile parentTile, boolean constructionQueued) {
		this.parentTile = parentTile;
		this.constructionQueued = constructionQueued;
	}
}
