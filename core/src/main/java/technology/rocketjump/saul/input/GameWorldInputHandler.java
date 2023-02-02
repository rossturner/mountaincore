package technology.rocketjump.saul.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
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
import technology.rocketjump.saul.rendering.DebugRenderingOptions;
import technology.rocketjump.saul.rendering.RenderingOptions;
import technology.rocketjump.saul.rendering.camera.DisplaySettings;
import technology.rocketjump.saul.rendering.camera.GlobalSettings;
import technology.rocketjump.saul.rendering.camera.PrimaryCameraWrapper;

import java.util.*;

/**
 * This class is for input directly in the game world, as compared to some input that was caught by the GUI instead
 */
@Singleton
public class GameWorldInputHandler implements InputProcessor, GameContextAware, Telegraph {

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

	private final Map<CommandName, Runnable> keyDownActions = new HashMap<>();
	private final Map<CommandName, Runnable> keyUpActions = new HashMap<>();

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

		keyDownActions.put(CommandName.PAN_CAMERA_LEFT, () -> primaryCameraWrapper.setMovementX(-1));
		keyDownActions.put(CommandName.PAN_CAMERA_RIGHT, () -> primaryCameraWrapper.setMovementX(1));
		keyDownActions.put(CommandName.PAN_CAMERA_UP, () -> primaryCameraWrapper.setMovementY(1));
		keyDownActions.put(CommandName.PAN_CAMERA_DOWN, () -> primaryCameraWrapper.setMovementY(-1));
		keyDownActions.put(CommandName.FAST_PAN, () -> primaryCameraWrapper.setPanSpeedMultiplier(true));
		keyDownActions.put(CommandName.ZOOM_IN, () -> primaryCameraWrapper.setMovementZ(-0.075f));
		keyDownActions.put(CommandName.ZOOM_OUT, () -> primaryCameraWrapper.setMovementZ(0.075f));
		keyDownActions.put(CommandName.QUICKSAVE, () -> messageDispatcher.dispatchMessage(MessageType.REQUEST_SAVE));
		keyDownActions.put(CommandName.QUICKLOAD, () -> messageDispatcher.dispatchMessage(MessageType.TRIGGER_QUICKLOAD));

		rebuildKeyupActions();
		messageDispatcher.addListener(this, MessageType.DEV_MODE_CHANGED);
	}

	private Map<CommandName, Runnable> rebuildKeyupActions() {
		keyUpActions.clear();
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

		DebugRenderingOptions debug = renderingOptions.debug();
		if (GlobalSettings.DEV_MODE) {
			keyUpActions.put(CommandName.DEBUG_FRAME_BUFFER_0, () -> debug.setFrameBufferIndex(0));
			keyUpActions.put(CommandName.DEBUG_FRAME_BUFFER_1, () -> debug.setFrameBufferIndex(1));
			keyUpActions.put(CommandName.DEBUG_FRAME_BUFFER_2, () -> debug.setFrameBufferIndex(2));
			keyUpActions.put(CommandName.DEBUG_FRAME_BUFFER_3, () -> debug.setFrameBufferIndex(3));
			keyUpActions.put(CommandName.DEBUG_FRAME_BUFFER_4, () -> debug.setFrameBufferIndex(4));
			keyUpActions.put(CommandName.DEBUG_FRAME_BUFFER_5, () -> debug.setFrameBufferIndex(5));
			keyUpActions.put(CommandName.DEBUG_FRAME_BUFFER_6, () -> debug.setFrameBufferIndex(6));
			keyUpActions.put(CommandName.DEBUG_FRAME_BUFFER_7, () -> debug.setFrameBufferIndex(7));
			keyUpActions.put(CommandName.DEBUG_FRAME_BUFFER_8, () -> debug.setFrameBufferIndex(8));
			keyUpActions.put(CommandName.DEBUG_FRAME_BUFFER_9, () -> debug.setFrameBufferIndex(9));
			keyUpActions.put(CommandName.DEBUG_SHOW_JOB_STATUS, () -> debug.setShowJobStatus(!debug.showJobStatus()));
			keyUpActions.put(CommandName.DEBUG_TOGGLE_FLOOR_OVERLAP_RENDERING, renderingOptions::toggleFloorOverlapRenderingEnabled);
			keyUpActions.put(CommandName.DEBUG_SHOW_INDIVIDUAL_LIGHTING_BUFFERS, debug::toggleShowIndividualLightingBuffers);
			keyUpActions.put(CommandName.DEBUG_SHOW_LIQUID_FLOW, debug::toggleShowLiquidFlow);
			keyUpActions.put(CommandName.DEBUG_SHOW_ZONES, debug::toggleShowZones);
			keyUpActions.put(CommandName.DEBUG_SHOW_PATHFINDING_NODES, debug::toggleShowPathfindingNodes);
			keyUpActions.put(CommandName.DEBUG_HIDE_GUI, () -> DisplaySettings.showGui = !DisplaySettings.showGui);
			keyUpActions.put(CommandName.DEBUG_GAME_SPEED_ULTRA_FAST, () -> messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.SPEED5));
			keyUpActions.put(CommandName.DEBUG_GAME_SPEED_SLOW, () -> messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.VERY_SLOW));
			keyUpActions.put(CommandName.DEBUG_SHOW_MENU, () -> messageDispatcher.dispatchMessage(MessageType.TOGGLE_DEBUG_VIEW));
		}
		return keyUpActions;
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

		if (activeCommands.contains(CommandName.PAN_CAMERA_LEFT) ||
				activeCommands.contains(CommandName.PAN_CAMERA_RIGHT) ||
				activeCommands.contains(CommandName.PAN_CAMERA_UP) ||
				activeCommands.contains(CommandName.PAN_CAMERA_DOWN) ) {
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

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.DEV_MODE_CHANGED -> {
				rebuildKeyupActions();
				return true;
			}
			default -> throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + getClass().getSimpleName() + ", " + msg);
		}
	}
}
