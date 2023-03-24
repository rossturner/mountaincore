package technology.rocketjump.mountaincore.ui.actions;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.rooms.RoomType;
import technology.rocketjump.mountaincore.ui.GameInteractionMode;

public class SetInteractionMode implements ButtonAction {

	private final GameInteractionMode targetMode;
	private final RoomType placementRoomType;
	private final MessageDispatcher messageDispatcher;

	public SetInteractionMode(GameInteractionMode targetMode, MessageDispatcher messageDispatcher) {
		this.targetMode = targetMode;
		this.messageDispatcher = messageDispatcher;
		this.placementRoomType = null;
	}

	public SetInteractionMode(GameInteractionMode targetMode, RoomType placementRoomType, MessageDispatcher messageDispatcher) {
		this.targetMode = targetMode;
		this.messageDispatcher = messageDispatcher;
		this.placementRoomType = placementRoomType;
	}

	@Override
	public void onClick() {
		if (placementRoomType != null) {
			targetMode.setRoomType(placementRoomType);
		}
		messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, targetMode);
	}

}
