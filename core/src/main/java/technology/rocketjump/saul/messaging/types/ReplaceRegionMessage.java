package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.mapping.tile.MapTile;

public class ReplaceRegionMessage {

	public final MapTile tileToReplace;
	public final int replacementRegionId;

	public ReplaceRegionMessage(MapTile tileToReplace, int replacementRegionId) {
		this.tileToReplace = tileToReplace;
		this.replacementRegionId = replacementRegionId;
	}
}
