package technology.rocketjump.mountaincore.environment.model;

public enum GameSpeed {

	PAUSED("pause", 0),
	NORMAL("play", 1),
	SPEED2("speed2", 2),
	SPEED3("speed3", 4),
	SPEED4("speed4", 7),

	VERY_SLOW(null, 0.25f),
	SPEED5(null, 18);

	public final String iconName;
	public final float speedMultiplier;

	GameSpeed(String iconName, float speedMultiplier) {
		this.iconName = iconName;
		this.speedMultiplier = speedMultiplier;
	}

	public static final GameSpeed[] VISIBLE_TO_UI = new GameSpeed[] { PAUSED, NORMAL, SPEED2, SPEED3, SPEED4 };

}
