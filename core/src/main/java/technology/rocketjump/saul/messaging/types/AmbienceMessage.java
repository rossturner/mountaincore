package technology.rocketjump.saul.messaging.types;

public class AmbienceMessage {

	public final int outdoorTiles;
	public final int riverTiles;
	public final int totalTiles;

	public AmbienceMessage(int outdoorTiles, int riverTiles, int totalTiles) {
		this.outdoorTiles = outdoorTiles;
		this.riverTiles = riverTiles;
		this.totalTiles = totalTiles;
	}

}
