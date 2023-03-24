package technology.rocketjump.mountaincore.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.StringBuilder;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.persistence.UserPreferences;
import technology.rocketjump.mountaincore.ui.ViewportUtils;
import technology.rocketjump.mountaincore.ui.i18n.DisplaysText;
import technology.rocketjump.mountaincore.ui.skins.GuiSkinRepository;

/**
 * This class draws text on top of the screen,
 * mostly used for debugging purposes, but also renders dragged area size as WidthxHeight near cursor
 */
@Singleton
public class ScreenWriter implements DisplaysText {

	private static final float LINE_HEIGHT = 60f;
	private final ExtendViewport viewport;
	private final Vector2 viewportDimensions;
	private Label label;
	private Label dragSizeLabel;
	private final Skin skin;
	private final Stage stage;
	private final Array<String> lines = new Array<>();
	public Vector2 offsetPosition = new Vector2();
	private boolean dragging;


	@Inject
	public ScreenWriter(GuiSkinRepository guiSkinRepository, UserPreferences userPreferences, MessageDispatcher messageDispatcher) {
        this.viewportDimensions = ViewportUtils.scaledViewportDimensions(userPreferences);
		viewport = new ExtendViewport(viewportDimensions.x,viewportDimensions.y);
		stage = new Stage(viewport);
		skin = guiSkinRepository.getMainGameSkin();
		messageDispatcher.addListener(msg -> {
			if (MessageType.GUI_SCALE_CHANGED == msg.message) {
				Vector2 updatedDimensions = ViewportUtils.scaledViewportDimensions(userPreferences);
				viewport.setMinWorldWidth(updatedDimensions.x);
				viewport.setMinWorldHeight(updatedDimensions.y);
				onResize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
				return true;
			}
			return false;
		}, MessageType.GUI_SCALE_CHANGED);
		rebuildUI();
	}

	@Override
	public void rebuildUI() {
		if (label != null) {
			label.remove();
		}
		label = new Label("Default text", skin);
		dragSizeLabel = new Label("Test", skin);

		Vector2 mainLabelCoords = stage.screenToStageCoordinates(new Vector2(150f, viewportDimensions.y - 60f - label.getHeight()));
		label.setPosition(mainLabelCoords.x, mainLabelCoords.y);

		stage.addActor(label);
	}

	private void clearText() {
		lines.clear();
	}

	public void printLine(String line) {
		lines.add(line);
	}

	public void render() {
		StringBuilder linesBuilder = new StringBuilder();
		for (String line : lines) {
			linesBuilder.append(line).append("\n");
		}
		label.setText(linesBuilder.toString());
		Vector2 basePosition = new Vector2(150f, viewportDimensions.y - 60f - (lines.size * LINE_HEIGHT));
		basePosition.add(offsetPosition);
		label.setPosition(basePosition.x, basePosition.y);

		if (dragging) {
			if (!dragSizeLabel.hasParent()) {
				stage.addActor(dragSizeLabel);
			}
			Vector2 stageCoords = stage.screenToStageCoordinates(new Vector2(Gdx.input.getX() + 30, Gdx.input.getY() - 30));
			dragSizeLabel.setPosition(stageCoords.x, stageCoords.y);
		} else {
			dragSizeLabel.remove();
		}

		stage.act(Gdx.graphics.getDeltaTime());
		stage.draw();

		clearText();
	}

	public void onResize(int screenWidth, int screenHeight) {
		viewport.update(screenWidth, screenHeight, true);
	}
	private int currentTileWidth = 0;

	private int currentTileHeight = 0;

	public int getCurrentTileWidth() {
		return currentTileWidth;
	}

	public int getCurrentTileHeight() {
		return currentTileHeight;
	}

	public void setDragging(boolean isDragging, int width, int height) {
		if (currentTileWidth != width || currentTileHeight != height) {
			currentTileWidth = width;
			currentTileHeight = height;
			dragSizeLabel.setText(width + "x" + height);
		}
		this.dragging = isDragging;
	}
}
