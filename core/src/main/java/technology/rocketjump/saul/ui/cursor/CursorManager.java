package technology.rocketjump.saul.ui.cursor;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Cursor;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.utils.Disposable;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class CursorManager {

	private static final int DEFAULT_HOTSPOT_OFFSET = 12;
	private final Map<String, Cursor> cursorsByName = new HashMap<>();
	private String currentCursorName;

	@Inject
	public CursorManager() {
		// note that main application onResize is called at startup
	}

	public void onResize() {
		cursorsByName.values().forEach(Disposable::dispose);
		cursorsByName.clear();

		createCursors();

		switchToCursor(currentCursorName);
	}

	public void switchToCursor(String cursorName) {
		if (cursorName == null) {
			cursorName = "cursor";
		}
		if (!cursorsByName.containsKey(cursorName)) {
			Gdx.graphics.setSystemCursor(Cursor.SystemCursor.Arrow);
			this.currentCursorName = null;
		} else {
			Gdx.graphics.setCursor(cursorsByName.get(cursorName));
			this.currentCursorName = cursorName;
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
