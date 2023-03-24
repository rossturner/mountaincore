package technology.rocketjump.mountaincore.rooms.components;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.mountaincore.misc.Destructible;
import technology.rocketjump.mountaincore.persistence.model.ChildPersistable;
import technology.rocketjump.mountaincore.rooms.Room;

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
