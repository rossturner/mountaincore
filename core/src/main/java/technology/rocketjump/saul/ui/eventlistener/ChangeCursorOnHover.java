package technology.rocketjump.saul.ui.eventlistener;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.ui.cursor.GameCursor;

public class ChangeCursorOnHover extends InputListener {

	private final GameCursor cursor;
	private final MessageDispatcher messageDispatcher;

	public ChangeCursorOnHover(GameCursor cursor, MessageDispatcher messageDispatcher) {
		this.cursor = cursor;
		this.messageDispatcher = messageDispatcher;
	}

	@Override
	public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
		messageDispatcher.dispatchMessage(MessageType.PUSH_CURSOR_TO_STACK, cursor);
	}

	@Override
	public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
		messageDispatcher.dispatchMessage(MessageType.POP_CURSOR_FROM_STACK);
	}
}
