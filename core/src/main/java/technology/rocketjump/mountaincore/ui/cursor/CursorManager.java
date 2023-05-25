package technology.rocketjump.mountaincore.ui.cursor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Disposable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.messaging.MessageType;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class CursorManager implements Telegraph {

	private final Map<GameCursor, Cursor> allCursors = new HashMap<>();
	private GameCursor hoverCursor; // has priority
	private GameCursor specialCursor;
	private GameCursor interactionModeCursor;

	@Inject
	public CursorManager(MessageDispatcher messageDispatcher) {
		// note that main application onResize is called at startup

		messageDispatcher.addListener(this, MessageType.SET_HOVER_CURSOR);
		messageDispatcher.addListener(this, MessageType.SET_SPECIAL_CURSOR);
		messageDispatcher.addListener(this, MessageType.SET_INTERACTION_MODE_CURSOR);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.SET_HOVER_CURSOR -> {
				hoverCursor = (GameCursor)msg.extraInfo;
				resetCursor();
				return true;
			}
			case MessageType.SET_SPECIAL_CURSOR -> {
				specialCursor = (GameCursor)msg.extraInfo;
				resetCursor();
				return true;
			}
			case MessageType.SET_INTERACTION_MODE_CURSOR -> {
				interactionModeCursor = (GameCursor)msg.extraInfo;
				resetCursor();
				return true;
			}
			default -> throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + getClass().getSimpleName());
		}
	}

	public void onResize() {
		allCursors.values().forEach(Disposable::dispose);
		allCursors.clear();

		createCursors();

		resetCursor();
	}

	private void resetCursor() {
		GameCursor selected;
		// Essentially the cursor properties on this class are a series of priorities for which to use
		if (hoverCursor != null) {
			selected = hoverCursor;
		} else if (specialCursor != null) {
			selected = specialCursor;
		} else if (interactionModeCursor != null) {
			selected = interactionModeCursor;
		} else {
			selected = GameCursor.CURSOR;
		}

		if (!allCursors.containsKey(selected)) {
			Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
		} else {
			Gdx.graphics.setCursor(allCursors.get(selected));
		}
	}

	private void createCursors() {
		boolean lowResolution = isResolution1080pOrLower();
		FileHandle cursorsDir = lowResolution ? Gdx.files.internal("assets/ui/cursors/1080p") : Gdx.files.internal("assets/ui/cursors/4k");
		for (FileHandle cursorFile : cursorsDir.list()) {
			if (cursorFile.name().startsWith("cursor_") && cursorFile.name().endsWith(".png")) {
				createCursor(cursorFile, lowResolution);
			}
		}
	}

	private void createCursor(FileHandle cursorFile, boolean lowResolution) {
		Pixmap cursorPixmap = new Pixmap(cursorFile);
		String name = cursorFile.nameWithoutExtension();
		name = name.substring(7);
		GameCursor gameCursor = GameCursor.forName(name);
		Cursor cursor = Gdx.graphics.newCursor(cursorPixmap,
				// The following offsets the hotspot for the cursor from the top left corner of the image to (12,12) or (6,6) depending on scale
				lowResolution ? gameCursor.hotspotX / 2 : gameCursor.hotspotX,
				lowResolution ? gameCursor.hotspotY / 2 : gameCursor.hotspotY);
		allCursors.put(gameCursor, cursor);
		cursorPixmap.dispose();
	}

	private boolean isResolution1080pOrLower() {
		Graphics.DisplayMode desktopMode = Lwjgl3ApplicationConfiguration.getDisplayMode();
		return desktopMode.width < 2000;
	}
}
