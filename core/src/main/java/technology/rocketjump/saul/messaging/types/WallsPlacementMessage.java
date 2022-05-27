package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.rooms.constructions.WallConstruction;

import java.util.List;

public class WallsPlacementMessage {

	public final List<WallConstruction> wallConstructions;

	public WallsPlacementMessage(List<WallConstruction> wallConstructions) {
		this.wallConstructions = wallConstructions;
	}
}
