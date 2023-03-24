package technology.rocketjump.mountaincore.ui.cursor;

import java.util.Arrays;

public enum GameCursor {

	ADD_TILE(6, 6),
	ATTACK(8, 8),
	BRIDGE(6, 6),
	CANCEL(6, 6),
	CURSOR(12, 12),
	DECONSTRUCT(6, 6),
	DOOR(6, 6),
	FLOOR(6, 6),
	I_BEAM(32, 32),
	GEARS(6, 6),
	LOGGING(6, 6),
	MINING(6, 6),
	PRIORITY(6, 6),
	REORDER_HORIZONTAL(32, 32),
	REORDER_VERTICAL(32, 32),
	RESIZE(32, 32),
	ROOFING(6, 6),
	ROOMS(6, 6),
	SELECT(26, 6),
	SICKLE(6, 6),
	SPADE(6, 6),
	SPLASH(6, 6),
	SUBTRACT_TILE(6, 6),
	WALL(6, 6);

	public final int hotspotX;
	public final int hotspotY;

	GameCursor(int hotspotX, int hotspotY) {
		this.hotspotX = hotspotX;
		this.hotspotY = hotspotY;
	}

	public static GameCursor forName(String name) {
		return Arrays.stream(values()).filter(c -> c.name().toLowerCase().equals(name)).findFirst().orElse(CURSOR);
	}

}
