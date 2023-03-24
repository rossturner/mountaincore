package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.mapping.tile.MapTile;

public class ReplaceRegionMessage {

	public final MapTile tileToReplace;
	public final int replacementRegionId;

	public ReplaceRegionMessage(MapTile tileToReplace, int replacementRegionId) {
		this.tileToReplace = tileToReplace;
		this.replacementRegionId = replacementRegionId;
	}
}
