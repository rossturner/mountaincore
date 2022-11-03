package technology.rocketjump.saul.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.environment.model.GameSpeed;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.MouseChangeMessage;
import technology.rocketjump.saul.persistence.UserPreferences;
import technology.rocketjump.saul.rendering.RenderingOptions;
import technology.rocketjump.saul.rendering.camera.GlobalSettings;
import technology.rocketjump.saul.rendering.camera.PrimaryCameraWrapper;

import java.util.*;

/**
 * This class is for input directly in the game world, as compared to some input that was caught by the GUI instead
 * <p>
 * MODDING - Keybindings should be driven by a moddable file, and later by an in-game keybindings menu
 */
@Singleton
public class GameWorldInputHandler implements InputProcessor, GameContextAware {

	public static final int SCROLL_BORDER = 2;

	private final UserPreferences userPreferences;
	private final PrimaryCameraWrapper primaryCameraWrapper;
	private final RenderingOptions renderingOptions;
	private final MessageDispatcher messageDispatcher;
	private GameContext gameContext;
	private final Set<CommandName> activeCommands = EnumSet.noneOf(CommandName.class);
	private final Set<Integer> keysPressed = new HashSet<>();
	private final Map<Integer, Boolean> buttonsPressed = new HashMap<>();
	private float startX, startY;

	private final Map<CommandName, Runnable> keyDownActions;
	private final Map<CommandName, Runnable> keyUpActions;

	@Inject
	public GameWorldInputHandler(UserPreferences userPreferences, PrimaryCameraWrapper primaryCameraWrapper,
	                             RenderingOptions renderingOptions, MessageDispatcher messageDispatcher) {
		this.userPreferences = userPreferences;
		this.primaryCameraWrapper = primaryCameraWrapper;
		this.renderingOptions = renderingOptions;
		this.messageDispatcher = messageDispatcher;
		buttonsPressed.put(Input.Buttons.LEFT, false);
		buttonsPressed.put(Input.Buttons.MIDDLE, false);
		buttonsPressed.put(Input.Buttons.RIGHT, false);
		buttonsPressed.put(Input.Buttons.FORWARD, false);
		buttonsPressed.put(Input.Buttons.BACK, false);

		keyDownActions = new HashMap<>();
		keyDownActions.put(CommandName.PAN_CAMERA_LEFT, () -> primaryCameraWrapper.setMovementX(-1));
		keyDownActions.put(CommandName.PAN_CAMERA_RIGHT, () -> primaryCameraWrapper.setMovementX(1));
		keyDownActions.put(CommandName.PAN_CAMERA_UP, () -> primaryCameraWrapper.setMovementY(1));
		keyDownActions.put(CommandName.PAN_CAMERA_DOWN, () -> primaryCameraWrapper.setMovementY(-1));
		keyDownActions.put(CommandName.FAST_PAN, () -> primaryCameraWrapper.setPanSpeedMultiplier(true));
		keyDownActions.put(CommandName.ZOOM_IN, () -> primaryCameraWrapper.setMovementZ(-0.075f));
		keyDownActions.put(CommandName.ZOOM_OUT, () -> primaryCameraWrapper.setMovementZ(0.075f));
		keyDownActions.put(CommandName.QUICKSAVE, () -> messageDispatcher.dispatchMessage(MessageType.REQUEST_SAVE));
		keyDownActions.put(CommandName.QUICKLOAD, () -> messageDispatcher.dispatchMessage(MessageType.TRIGGER_QUICKLOAD));

		keyUpActions = new HashMap<>();
		keyUpActions.put(CommandName.PAN_CAMERA_LEFT, () -> primaryCameraWrapper.setMovementX(0));
		keyUpActions.put(CommandName.PAN_CAMERA_RIGHT, () -> primaryCameraWrapper.setMovementX(0));
		keyUpActions.put(CommandName.PAN_CAMERA_UP, () -> primaryCameraWrapper.setMovementY(0));
		keyUpActions.put(CommandName.PAN_CAMERA_DOWN, () -> primaryCameraWrapper.setMovementY(0));
		keyUpActions.put(CommandName.FAST_PAN, () -> primaryCameraWrapper.setPanSpeedMultiplier(false));
		keyUpActions.put(CommandName.ZOOM_IN, () -> primaryCameraWrapper.setMovementZ(0));
		keyUpActions.put(CommandName.ZOOM_OUT, () -> primaryCameraWrapper.setMovementZ(0));
		keyUpActions.put(CommandName.PAUSE, () -> messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.PAUSED));
		keyUpActions.put(CommandName.GAME_SPEED_NORMAL, () -> messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.NORMAL));
		keyUpActions.put(CommandName.GAME_SPEED_FAST, () -> messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.SPEED2));
		keyUpActions.put(CommandName.GAME_SPEED_FASTER, () -> messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.SPEED3));
		keyUpActions.put(CommandName.GAME_SPEED_FASTEST, () -> messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.SPEED4));
		keyUpActions.put(CommandName.ROTATE, () -> messageDispatcher.dispatchMessage(MessageType.ROTATE_FURNITURE));



		/*
		boolean leftControlPressed = Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT);
		if (GlobalSettings.DEV_MODE) {
			if (leftControlPressed && keycode >= Input.Keys.NUM_0 && keycode <= Input.Keys.NUM_9) {
				renderingOptions.debug().setFrameBufferIndex(keycode - Input.Keys.NUM_0);
			} else if (keycode == Input.Keys.J) {
				renderingOptions.debug().setShowJobStatus(!renderingOptions.debug().showJobStatus());
			} else if (keycode == Input.Keys.O) {
				renderingOptions.toggleFloorOverlapRenderingEnabled();
			} else if (keycode == Input.Keys.L) {
				renderingOptions.debug().setShowIndividualLightingBuffers(!renderingOptions.debug().showIndividualLightingBuffers());
			} else if (keycode == Input.Keys.F) {
				renderingOptions.debug().setShowLiquidFlow(!renderingOptions.debug().isShowLiquidFlow());
			} else if (keycode == Input.Keys.Z) {
				renderingOptions.debug().setShowZones(!renderingOptions.debug().isShowZones());
			} else if (keycode == Input.Keys.T) {
				renderingOptions.debug().setShowPathfindingNodes(!renderingOptions.debug().showPathfindingNodes());
			} else if (keycode == Input.Keys.G) {
				DisplaySettings.showGui = !DisplaySettings.showGui;
			} else if (keycode == Input.Keys.NUM_5 && gameContext != null) {
				messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.SPEED5);
			} else if (keycode == Input.Keys.NUM_6 && gameContext != null) {
				messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.VERY_SLOW);
			} else if (keycode == Input.Keys.GRAVE) {
				messageDispatcher.dispatchMessage(MessageType.TOGGLE_DEBUG_VIEW);
			}
		}



		 */
	}

	@Override
	public boolean keyDown(int keycode) {
		keysPressed.add(keycode);

		Set<CommandName> commandNames = userPreferences.getCommandsFor(keysPressed);

		for (CommandName commandName : commandNames) {
			if (activeCommands.add(commandName)) {
				Runnable action = keyDownActions.get(commandName);
				if (action != null) {
					action.run();
				}
			}
		}

		return true;
	}

	@Override
	public boolean keyUp(int keycode) {
		keysPressed.remove(keycode);

		Set<CommandName> commandNames = userPreferences.getCommandsFor(keysPressed);
		Set<CommandName> inactiveCommands = new HashSet<>(activeCommands);
		inactiveCommands.removeAll(commandNames);
		activeCommands.removeAll(inactiveCommands);

		if (keycode == Input.Keys.ESCAPE) {
			messageDispatcher.dispatchMessage(MessageType.CANCEL_SCREEN_OR_GO_TO_MAIN_MENU);
		} else {
			for (CommandName commandName : inactiveCommands) {
				Runnable action = keyUpActions.get(commandName);
				if (action != null) {
					action.run();
				}
			}
		}

		return true;
	}

	@Override
	public boolean keyTyped(char character) {
		return false;
	}

	@Override
	public boolean touchDown(int screenX, int screenY, int pointer, int button) {
		if (renderingOptions.debug().showIndividualLightingBuffers()) {
			screenX = renderingOptions.debug().adjustScreenXForSplitView(screenX);
			screenY = renderingOptions.debug().adjustScreenYForSplitView(screenY);
		}

		this.startX = screenX;
		this.startY = screenY;
		this.buttonsPressed.put(button, true);

		Vector3 worldPosition = primaryCameraWrapper.getCamera().unproject(new Vector3(screenX, screenY, 0));
		Vector2 worldPosition2 = new Vector2(worldPosition.x, worldPosition.y);
		MouseChangeMessage.MouseButtonType mouseButtonType = MouseChangeMessage.MouseButtonType.byButtonCode(button);
		if (mouseButtonType != null) {
			MouseChangeMessage mouseChangeMessage = new MouseChangeMessage(screenX, screenY, worldPosition2, mouseButtonType);
			messageDispatcher.dispatchMessage(null, MessageType.MOUSE_DOWN, mouseChangeMessage);
			return true;
		}
		return false;
	}

	@Override
	public boolean touchUp(int screenX, int screenY, int pointer, int button) {
		this.buttonsPressed.put(button, false);
		if (renderingOptions.debug().showIndividualLightingBuffers()) {
			screenX = renderingOptions.debug().adjustScreenXForSplitView(screenX);
			screenY = renderingOptions.debug().adjustScreenYForSplitView(screenY);
		}

		Vector3 worldPosition = primaryCameraWrapper.getCamera().unproject(new Vector3(screenX, screenY, 0));
		Vector2 worldPosition2 = new Vector2(worldPosition.x, worldPosition.y);
		MouseChangeMessage.MouseButtonType mouseButtonType = MouseChangeMessage.MouseButtonType.byButtonCode(button);
		if (mouseButtonType != null) {
			MouseChangeMessage mouseChangeMessage = new MouseChangeMessage(screenX, screenY, worldPosition2, mouseButtonType);
			messageDispatcher.dispatchMessage(null, MessageType.MOUSE_UP, mouseChangeMessage);
			return true;
		}
		return false;
	}

	@Override
	public boolean touchDragged(int screenX, int screenY, int pointer) {
		//Copied from CameraInputController
		if (buttonsPressed.get(Input.Buttons.MIDDLE)) {
			final float deltaX = (screenX - startX) / Gdx.graphics.getWidth();
			final float deltaY = (startY - screenY) / Gdx.graphics.getHeight();
			startX = screenX;
			startY = screenY;

			primaryCameraWrapper.moveTo(deltaX, deltaY);
		}
		Vector3 worldPosition = primaryCameraWrapper.getCamera().unproject(new Vector3(screenX, screenY, 0));
		Vector2 worldPosition2 = new Vector2(worldPosition.x, worldPosition.y);
		MouseChangeMessage mouseMovedMessage = new MouseChangeMessage(screenX, screenY, worldPosition2, null);
		messageDispatcher.dispatchMessage(null, MessageType.MOUSE_MOVED, mouseMovedMessage);

		return true;
	}

	@Override
	public boolean mouseMoved(int screenX, int screenY) {
		int width = Gdx.graphics.getWidth();
		int height = Gdx.graphics.getHeight();

		//TODO: need to do something for these
		if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT) ||
				Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT) ||
				Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP) ||
				Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
			return false; // Don't do anything if already scrolling by key
		}

		if (GlobalSettings.USE_EDGE_SCROLLING) {
			if (screenX <= SCROLL_BORDER) {
				primaryCameraWrapper.setMovementX(-1);
			} else if (screenX >= width - SCROLL_BORDER) {
				primaryCameraWrapper.setMovementX(1);
			} else {
				primaryCameraWrapper.setMovementX(0);
			}

			if (screenY <= SCROLL_BORDER) {
				primaryCameraWrapper.setMovementY(1);
			} else if (screenY >= height - SCROLL_BORDER) {
				primaryCameraWrapper.setMovementY(-1);
			} else {
				primaryCameraWrapper.setMovementY(0);
			}
		}


		return true;
	}


	@Override
	public boolean scrolled(float amountX, float amountY) {
		if (!buttonsPressed.get(Input.Buttons.MIDDLE)) { //Don't zoom when holding in middle button
			primaryCameraWrapper.zoom((int) amountY);
		}
		return true;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
