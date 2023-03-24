package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.mapping.tile.MapTile;

public class RemoveDesignationMessage {

	private final MapTile targetTile;

	public RemoveDesignationMessage(MapTile targetTile) {
		this.targetTile = targetTile;
	}

	public MapTile getTargetTile() {
		return targetTile;
	}

}
