package technology.rocketjump.mountaincore.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.persistence.UserPreferences;
import technology.rocketjump.mountaincore.ui.ViewportUtils;
import technology.rocketjump.mountaincore.ui.widgets.GameDialog;


/**
 * Deals with basic boilerplate of rendering scene2d stage and empty implementation for some hooks
 */
public abstract class AbstractGameScreen implements GameScreen {
	protected final OrthographicCamera camera = new OrthographicCamera();
	protected final ExtendViewport viewport;
	protected final Stage stage;

	protected AbstractGameScreen(UserPreferences userPreferences, MessageDispatcher messageDispatcher) {
		Vector2 viewportDimensions = ViewportUtils.scaledViewportDimensions(userPreferences);
		this.viewport = new ExtendViewport(viewportDimensions.x, viewportDimensions.y);
		this.stage = new Stage(viewport);
		messageDispatcher.addListener(msg -> {
			if (MessageType.GUI_SCALE_CHANGED == msg.message) {
				Vector2 updatedDimensions = ViewportUtils.scaledViewportDimensions(userPreferences);
				viewport.setMinWorldWidth(updatedDimensions.x);
				viewport.setMinWorldHeight(updatedDimensions.y);
				resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
				return true;
			}
			return false;
		}, MessageType.GUI_SCALE_CHANGED);
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
