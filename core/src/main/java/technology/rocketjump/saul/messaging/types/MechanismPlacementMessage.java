package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.model.physical.mechanism.MechanismType;
import technology.rocketjump.saul.mapping.tile.MapTile;

public class MechanismPlacementMessage {
	public final MapTile mapTile;
	public final MechanismType mechanismType;

	public MechanismPlacementMessage(MapTile mapTile, MechanismType mechanismType) {
		this.mapTile = mapTile;
		this.mechanismType = mechanismType;
	}
}
