package technology.rocketjump.saul.ui.eventlistener;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.ui.cursor.GameCursor;

public class ChangeCursorOnHover extends InputListener {

	private final GameCursor cursor;
	private final MessageDispatcher messageDispatcher;
	private final Actor parentActor;

	public ChangeCursorOnHover(Actor parentActor, GameCursor cursor, MessageDispatcher messageDispatcher) {
		this.parentActor = parentActor;
		this.cursor = cursor;
		this.messageDispatcher = messageDispatcher;
	}

	@Override
	public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
		if (parentActor instanceof Button button && button.isDisabled()) {
			return;
		}
		messageDispatcher.dispatchMessage(MessageType.SET_HOVER_CURSOR, cursor);
	}

	@Override
	public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
		messageDispatcher.dispatchMessage(MessageType.SET_HOVER_CURSOR, null);
	}
}
