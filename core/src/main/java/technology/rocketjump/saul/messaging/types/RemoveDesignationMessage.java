package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.mapping.tile.MapTile;

public class RemoveDesignationMessage {

	private final MapTile targetTile;

	public RemoveDesignationMessage(MapTile targetTile) {
		this.targetTile = targetTile;
	}

	public MapTile getTargetTile() {
		return targetTile;
	}

}
