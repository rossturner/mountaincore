package technology.rocketjump.saul.messaging.types;

public class StartNewGameMessage {

	public final String settlementName;
	public final long seed;

	public StartNewGameMessage(String settlementName, long seed) {
		this.settlementName = settlementName;
		this.seed = seed;
	}
}
