package technology.rocketjump.saul.screens;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import technology.rocketjump.saul.ui.widgets.GameDialog;

import static technology.rocketjump.saul.rendering.camera.DisplaySettings.GUI_DESIGN_SIZE;

/**
 * Deals with basic boilerplate of rendering scene2d stage and empty implementation for some hooks
 */
public abstract class AbstractGameScreen implements GameScreen {
	protected final OrthographicCamera camera = new OrthographicCamera();
	protected final Viewport viewport = new ExtendViewport(GUI_DESIGN_SIZE.x, GUI_DESIGN_SIZE.y);
	protected final Stage stage;

	protected AbstractGameScreen() {
		this.stage = new Stage(viewport);
	}

	@Override
	public void pause() { }

	@Override
	public void resume() { }

	@Override
	public void hide() { }

	@Override
	public void render(float delta) {
		camera.update();
		stage.act(delta);
		stage.draw();
	}

	@Override
	public void showDialog(GameDialog dialog) {
		dialog.show(stage);
	}

	@Override
	public void resize(int width, int height) {
		camera.setToOrtho(false, width, height);
		viewport.update(width, height, true);
	}
}
