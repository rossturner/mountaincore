package technology.rocketjump.mountaincore.messaging.types;

public class GameSaveMessage {

	public final boolean asynchronous;

	public GameSaveMessage(boolean asynchronous) {
		this.asynchronous = asynchronous;
	}
}
