package technology.rocketjump.saul.messaging.types;

public class StartNewGameMessage {

	public final String settlementName;
	public final long seed;
	public final int mapWidth;
	public final int mapHeight;
	public final boolean peacefulMode;

	public StartNewGameMessage(String settlementName, long seed, int mapWidth, int mapHeight, boolean peacefulMode) {
		this.settlementName = settlementName;
		this.seed = seed;
		this.mapWidth = mapWidth;
		this.mapHeight = mapHeight;
		this.peacefulMode = peacefulMode;
	}
}
