package technology.rocketjump.saul.messaging.types;

public class StartNewGameMessage {

	public final String settlementName;
	public final long seed;
	public final int mapWidth;
	public final int mapHeight;

	public StartNewGameMessage(String settlementName, long seed, int mapWidth, int mapHeight) {
		this.settlementName = settlementName;
		this.seed = seed;
		this.mapWidth = mapWidth;
		this.mapHeight = mapHeight;
	}
}
