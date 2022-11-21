package technology.rocketjump.saul.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.StringBuilder;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.rendering.camera.DisplaySettings;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;

/**
 * This class draws text on top of the screen,
 * mostly used for debugging purposes, but also renders dragged area size as WidthxHeight near cursor
 */
@Singleton
public class ScreenWriter implements DisplaysText {

	private static final float LINE_HEIGHT = 60f;
	private final Viewport viewport;
	private Label label;
	private Label dragSizeLabel;
	private final Skin skin;
	private Stage stage;
	private Array<String> lines = new Array<>();
	public Vector2 offsetPosition = new Vector2();
	private boolean dragging;


	@Inject
	public ScreenWriter(GuiSkinRepository guiSkinRepository) {
		viewport = new ExtendViewport(DisplaySettings.GUI_DESIGN_SIZE.x, DisplaySettings.GUI_DESIGN_SIZE.y);
		stage = new Stage(viewport);
		skin = guiSkinRepository.getMainGameSkin();

		rebuildUI();
	}

	@Override
	public void rebuildUI() {
		if (label != null) {
			label.remove();
		}
		label = new Label("Default text", skin);
		dragSizeLabel = new Label("Test", skin);

		Vector2 mainLabelCoords = stage.screenToStageCoordinates(new Vector2(150f, DisplaySettings.GUI_DESIGN_SIZE.y - 60f - label.getHeight()));
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
		Vector2 basePosition = new Vector2(150f, DisplaySettings.GUI_DESIGN_SIZE.y - 60f - (lines.size * LINE_HEIGHT));
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
