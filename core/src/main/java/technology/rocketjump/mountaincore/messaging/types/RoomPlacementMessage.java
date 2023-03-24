package technology.rocketjump.mountaincore.messaging.types;

import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.mountaincore.production.StockpileGroup;
import technology.rocketjump.mountaincore.rooms.RoomTile;
import technology.rocketjump.mountaincore.rooms.RoomType;

import java.util.Map;

public class RoomPlacementMessage {

	private final Map<GridPoint2, RoomTile> roomTiles;
	private final RoomType roomType;
	public final StockpileGroup stockpileGroup;

	public RoomPlacementMessage(Map<GridPoint2, RoomTile> roomTiles, RoomType roomType, StockpileGroup stockpileGroup) {
		this.roomTiles = roomTiles;
		this.roomType = roomType;
		this.stockpileGroup = stockpileGroup;
	}

	public Map<GridPoint2, RoomTile> getRoomTiles() {
		return roomTiles;
	}

	public RoomType getRoomType() {
		return roomType;
	}
}
