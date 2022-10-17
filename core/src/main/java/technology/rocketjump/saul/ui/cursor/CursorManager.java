package technology.rocketjump.saul.ui.cursor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Disposable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.messaging.MessageType;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class CursorManager implements Telegraph {

	private static final int DEFAULT_HOTSPOT_OFFSET = 12;
	private final Map<String, Cursor> cursorsByName = new HashMap<>();
	private Deque<GameCursor> currentCursorStack = new ArrayDeque<>();

	@Inject
	public CursorManager(MessageDispatcher messageDispatcher) {
		// note that main application onResize is called at startup

		messageDispatcher.addListener(this, MessageType.PUSH_CURSOR_TO_STACK);
		messageDispatcher.addListener(this, MessageType.POP_CURSOR_FROM_STACK);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.PUSH_CURSOR_TO_STACK -> {
				pushCursor((GameCursor)msg.extraInfo);
				return true;
			}
			case MessageType.POP_CURSOR_FROM_STACK -> {
				popCursor();
				return true;
			}
			default -> throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + getClass().getSimpleName());
		}
	}

	public void pushCursor(GameCursor cursor) {
		if (cursor == null) {
			cursor = GameCursor.CURSOR;
		}
		if (currentCursorStack.isEmpty() || !currentCursorStack.peek().equals(cursor)) {
			currentCursorStack.push(cursor);
		}
		resetCursor();
	}

	public void popCursor() {
		if (!currentCursorStack.isEmpty()) {
			currentCursorStack.pop();
		}

		if (currentCursorStack.isEmpty()) {
			pushCursor(GameCursor.CURSOR);
		}

		resetCursor();
	}

	public void onResize() {
		cursorsByName.values().forEach(Disposable::dispose);
		cursorsByName.clear();

		createCursors();

		currentCursorStack.clear();
		pushCursor(GameCursor.CURSOR);
	}

	private void resetCursor() {
		String cursorName = currentCursorStack.peek().cursorName();
		if (!cursorsByName.containsKey(cursorName)) {
			Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
		} else {
			Gdx.graphics.setCursor(cursorsByName.get(cursorName));
		}
	}

	private void createCursors() {
		FileHandle cursorsDir = Gdx.files.internal("assets/ui/cursors");
		for (FileHandle cursorFile : cursorsDir.list()) {
			if (cursorFile.name().startsWith("cursor_") && cursorFile.name().endsWith(".png")) {
				createCursor(cursorFile);
			}
		}
	}

	private void createCursor(FileHandle cursorFile) {
		Pixmap cursorPixmap = new Pixmap(cursorFile);
		if (shouldHalfSize()) {
			cursorPixmap = halfSize(cursorPixmap);
		}
		Cursor cursor = Gdx.graphics.newCursor(cursorPixmap,
				// The following offsets the hotspot for the cursor from the top left corner of the image to (12,12) or (6,6) depending on scale
				shouldHalfSize() ? DEFAULT_HOTSPOT_OFFSET / 2 : DEFAULT_HOTSPOT_OFFSET,
				shouldHalfSize() ? DEFAULT_HOTSPOT_OFFSET / 2 : DEFAULT_HOTSPOT_OFFSET);
		String name = cursorFile.nameWithoutExtension();
		name = name.substring(7);
		cursorsByName.put(name, cursor);
		cursorPixmap.dispose();
	}

	private Pixmap halfSize(Pixmap fullSize) {
		Pixmap halfSize = new Pixmap(fullSize.getWidth() / 2, fullSize.getHeight() / 2, fullSize.getFormat());
		halfSize.drawPixmap(fullSize,
				0, 0, fullSize.getWidth(), fullSize.getWidth(),
				0, 0, halfSize.getWidth(), halfSize.getHeight()
		);
		fullSize.dispose();
		return halfSize;
	}

	private boolean shouldHalfSize() {
		Graphics.DisplayMode desktopMode = LwjglApplicationConfiguration.getDesktopDisplayMode();
		return desktopMode.width < 2000;
	}
}
