package technology.rocketjump.saul.ui.cursor;

public enum GameCursor {

	ADD_TILE,
	ATTACK,
	BRIDGE,
	CANCEL,
	CURSOR,
	DECONSTRUCT,
	DOOR,
	FLOOR,
	GEARS,
	LOGGING,
	MINING,
	PRIORITY,
	REORDER_HORIZONTAL,
	REORDER_VERTICAL,
	RESIZE,
	ROOFING,
	ROOMS,
	SELECT,
	SICKLE,
	SPADE,
	SPLASH,
	SUBTRACT_TILE,
	WALL;

	public String cursorName() {
		return name().toLowerCase();
	}

}
