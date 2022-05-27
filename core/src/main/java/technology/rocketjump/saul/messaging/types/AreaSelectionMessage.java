package technology.rocketjump.saul.messaging.types;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.saul.messaging.MessageType;

public class AreaSelectionMessage {

	public static final int MESSAGE_TYPE = MessageType.AREA_SELECTION;

	private final Vector2 minPoint;
	private final Vector2 maxPoint;

	public AreaSelectionMessage(Vector2 minPoint, Vector2 maxPoint) {
		this.minPoint = minPoint;
		this.maxPoint = maxPoint;
	}

	public Vector2 getMinPoint() {
		return minPoint;
	}

	public Vector2 getMaxPoint() {
		return maxPoint;
	}
}
