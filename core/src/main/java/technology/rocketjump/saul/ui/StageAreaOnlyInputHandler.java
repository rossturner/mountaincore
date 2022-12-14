package technology.rocketjump.saul.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.utils.Pools;
import technology.rocketjump.saul.ui.widgets.GameDialog;
import technology.rocketjump.saul.ui.widgets.GameDialogMessageHandler;

import java.util.List;

public class StageAreaOnlyInputHandler implements InputProcessor {

	private final Stage parent;
	private final GameInteractionStateContainer interactionStateContainer;
	private final GameDialogMessageHandler gameDialogMessageHandler;

	public StageAreaOnlyInputHandler(Stage parent, GameInteractionStateContainer interactionStateContainer,
									 GameDialogMessageHandler gameDialogMessageHandler) {
		this.parent = parent;
		this.interactionStateContainer = interactionStateContainer;
		this.gameDialogMessageHandler = gameDialogMessageHandler;
	}

	@Override
	public boolean keyDown(int keycode) {
		return parent.keyDown(keycode);
	}

	@Override
	public boolean keyUp(int keycode) {
		return parent.keyUp(keycode);
	}

	@Override
	public boolean keyTyped(char character) {
		return parent.keyTyped(character);
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		Vector2 mouseStageCoords = parent.screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
		Actor target = parent.hit(mouseStageCoords.x, mouseStageCoords.y, true);
		parent.touchDown(screenX, screenY, pointer, button);
		if (button == Input.Buttons.RIGHT) {
			List<GameDialog> displayedDialogs = gameDialogMessageHandler.getDisplayedDialogs();
			if (displayedDialogs.size() > 0) {
				displayedDialogs.get(0).close();
				return true;
			}
		}

		if (target == null || button == Input.Buttons.RIGHT || interactionStateContainer.isDragging()) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		Vector2 mouseStageCoords = parent.screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
		Actor target = parent.hit(mouseStageCoords.x, mouseStageCoords.y, true);
		parent.touchUp(screenX, screenY, pointer, button);
		if (target == null || button == Input.Buttons.RIGHT || interactionStateContainer.isDragging()) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		return parent.touchDragged(screenX, screenY, pointer);
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		Vector2 mouseStageCoords = parent.screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
		Actor target = parent.hit(mouseStageCoords.x, mouseStageCoords.y, true);

		parent.mouseMoved(screenX, screenY);
		if (target == null || interactionStateContainer.isDragging()) {
			return false;
		} else {
			return true;
		}

	}

	/**
	 * This is overriding default stage behaviour on scrolling to act where the mouse is rather than what has focus,
	 * mostly so scrolling in the game world zooms in and out while a Widget has focus
	 */
	@Override
	public boolean scrolled(float amountX, float amountY) {
		Vector2 mouseStageCoords = parent.screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));

		if (parent.getScrollFocus() != null && parent.getScrollFocus() instanceof ScrollPane) {
			Actor scrollActor = parent.getScrollFocus();
			Vector2 localCoords = scrollActor.stageToLocalCoordinates(mouseStageCoords);
			if (0 <= localCoords.x && localCoords.x <= scrollActor.getWidth() &&
					0 <= localCoords.y && localCoords.y <= scrollActor.getHeight()) {
				// Mouse is over the focused scroll pane, so let the default behaviour happen
				InputEvent event = Pools.obtain(InputEvent.class);
				event.setStage(parent);
				event.setType(InputEvent.Type.scrolled);
				event.setScrollAmountX(amountX);
				event.setScrollAmountY(amountY);
				event.setStageX(mouseStageCoords.x);
				event.setStageY(mouseStageCoords.y);
				scrollActor.fire(event);
				boolean handled = event.isHandled();
				Pools.free(event);
				return handled;
			}
		}
		return false;
	}

}
