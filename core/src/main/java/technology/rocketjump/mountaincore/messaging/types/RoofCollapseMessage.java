package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.mapping.tile.MapTile;

import java.util.Set;

public class RoofCollapseMessage {

	public final Set<MapTile> tilesToCollapseConstructedRoofing;

	public RoofCollapseMessage(Set<MapTile> tilesToCollapseConstructedRoofing) {
		this.tilesToCollapseConstructedRoofing = tilesToCollapseConstructedRoofing;
	}
}
