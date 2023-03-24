package technology.rocketjump.mountaincore.mapping.tile.layout;

import technology.rocketjump.mountaincore.mapping.tile.TileNeighbours;
import technology.rocketjump.mountaincore.rooms.Room;

public class RoomTileLayout extends TileLayout {

	public RoomTileLayout(TileNeighbours neighbours, Room targetRoom) {
		super(neighbours, (tile, direction) -> tile.hasRoom() && tile.getRoomTile().getRoom().getRoomId() == targetRoom.getRoomId());
	}

	public RoomTileLayout(int id) {
		super(id);
	}
}
