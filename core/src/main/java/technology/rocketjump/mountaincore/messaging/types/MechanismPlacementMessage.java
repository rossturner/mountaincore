package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.entities.model.physical.mechanism.MechanismType;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;

public class MechanismPlacementMessage {
	public final MapTile mapTile;
	public final MechanismType mechanismType;

	public MechanismPlacementMessage(MapTile mapTile, MechanismType mechanismType) {
		this.mapTile = mapTile;
		this.mechanismType = mechanismType;
	}
}
