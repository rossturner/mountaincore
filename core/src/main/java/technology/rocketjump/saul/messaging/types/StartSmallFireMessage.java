package technology.rocketjump.saul.messaging.types;

import com.badlogic.gdx.math.GridPoint2;

public class StartSmallFireMessage {
	public final long targetEntityId;
	public final GridPoint2 jobLocation;

	public StartSmallFireMessage(long targetEntityId, GridPoint2 jobLocation) {
		this.targetEntityId = targetEntityId;
		this.jobLocation = jobLocation;
	}
}
