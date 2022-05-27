package technology.rocketjump.saul.rooms.components;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.saul.misc.Destructible;
import technology.rocketjump.saul.persistence.model.ChildPersistable;
import technology.rocketjump.saul.rooms.Room;

public abstract class RoomComponent implements Destructible, ChildPersistable {

	protected final Room parent;
	protected final MessageDispatcher messageDispatcher;

	public RoomComponent(Room parent, MessageDispatcher messageDispatcher) {
		this.parent = parent;
		this.messageDispatcher = messageDispatcher;
	}

	public abstract RoomComponent clone(Room newParent);

	public abstract void mergeFrom(RoomComponent otherComponent);

	public abstract void tileRemoved(GridPoint2 location);

}
