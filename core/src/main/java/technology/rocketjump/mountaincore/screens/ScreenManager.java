package technology.rocketjump.mountaincore.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.EntityStore;
import technology.rocketjump.mountaincore.environment.GameClock;
import technology.rocketjump.mountaincore.environment.model.GameSpeed;
import technology.rocketjump.mountaincore.gamecontext.*;
import technology.rocketjump.mountaincore.jobs.SkillDictionary;
import technology.rocketjump.mountaincore.jobs.model.Skill;
import technology.rocketjump.mountaincore.mapping.factories.TiledMapFactory;
import technology.rocketjump.mountaincore.mapping.model.InvalidMapGenerationException;
import technology.rocketjump.mountaincore.mapping.model.TiledMap;
import technology.rocketjump.mountaincore.messaging.InfoType;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.async.ErrorType;
import technology.rocketjump.mountaincore.messaging.types.StartNewGameMessage;
import technology.rocketjump.mountaincore.persistence.UserPreferences;
import technology.rocketjump.mountaincore.rendering.camera.GlobalSettings;
import technology.rocketjump.mountaincore.rendering.camera.PrimaryCameraWrapper;
import technology.rocketjump.mountaincore.ui.GameInteractionMode;
import technology.rocketjump.mountaincore.ui.GameViewMode;
import technology.rocketjump.mountaincore.ui.views.GuiViewName;
import technology.rocketjump.mountaincore.ui.widgets.GameDialog;
import technology.rocketjump.mountaincore.ui.widgets.GameDialogDictionary;
import technology.rocketjump.mountaincore.ui.widgets.ModalDialog;

import java.util.ArrayList;
import java.util.List;

import static technology.rocketjump.mountaincore.jobs.SkillDictionary.NULL_PROFESSION;
import static technology.rocketjump.mountaincore.rendering.camera.GlobalSettings.DEV_MODE;

@Singleton
public class ScreenManager implements Telegraph, GameContextAware {

	private final GameScreenDictionary gameScreenDictionary;
	private final MessageDispatcher messageDispatcher;
	private final TiledMapFactory mapFactory;
	private final SkillDictionary skillDictionary;
	private final GameContextFactory gameContextFactory;
	private final GameContextRegister gameContextRegister;
	private final GameDialogDictionary dialogDictionary;
	private final EntityStore entityStore;
	private final UserPreferences userPreferences;
	private final PrimaryCameraWrapper primaryCameraWrapper;

	private GameScreen currentScreen;

	private MainGameScreen mainGameScreen;
	private MainMenuScreen mainMenuScreen;
	private GameContext gameContext;

	@Inject
	public ScreenManager(GameScreenDictionary gameScreenDictionary, MessageDispatcher messageDispatcher, TiledMapFactory mapFactory,
						 SkillDictionary skillDictionary, GameContextFactory gameContextFactory,
						 GameContextRegister gameContextRegister, GameDialogDictionary dialogDictionary,
						 EntityStore entityStore, UserPreferences userPreferences, PrimaryCameraWrapper primaryCameraWrapper, MainGameScreen mainGameScreen, MainMenuScreen mainMenuScreen) {
		this.gameScreenDictionary = gameScreenDictionary;
		this.messageDispatcher = messageDispatcher;
		this.mapFactory = mapFactory;
		this.skillDictionary = skillDictionary;
		this.gameContextFactory = gameContextFactory;
		this.gameContextRegister = gameContextRegister;
		this.dialogDictionary = dialogDictionary;
		this.entityStore = entityStore;
		this.userPreferences = userPreferences;
		this.primaryCameraWrapper = primaryCameraWrapper;

		this.mainGameScreen = mainGameScreen;
		this.mainMenuScreen = mainMenuScreen;

		messageDispatcher.addListener(this, MessageType.START_NEW_GAME);
		messageDispatcher.addListener(this, MessageType.SWITCH_SCREEN);
		messageDispatcher.addListener(this, MessageType.GUI_SHOW_ERROR);
		messageDispatcher.addListener(this, MessageType.GUI_SHOW_INFO);
		messageDispatcher.addListener(this, MessageType.NOTIFY_RESTART_REQUIRED);
		messageDispatcher.addListener(this, MessageType.SHOW_DIALOG);
		messageDispatcher.addListener(this, MessageType.INITIALISE_SPAWN_POINT);
	}

	private void startNewGame(StartNewGameMessage newGameMessage) {
		clearState();

		GameSeed worldSeed = newGameSeed(newGameMessage.seed, newGameMessage.mapWidth, newGameMessage.mapHeight);

		GameClock gameClock = new GameClock();
		GameContext gameContext = gameContextFactory.create(newGameMessage.settlementName, null, worldSeed.seed, gameClock, newGameMessage.peacefulMode);
		gameContextRegister.setNewContext(gameContext); // FIXME Should be able to remove this

		TiledMap map = null;
		while (map == null) {
			try {
				map = mapFactory.create(worldSeed.seed, worldSeed.mapWidth, worldSeed.mapHeight, gameContext);
			} catch (InvalidMapGenerationException e) {
				Logger.warn("Invalid map generated: " + e.getMessage());
				worldSeed = newGameSeed(new RandomXS128(newGameMessage.seed).nextLong(), newGameMessage.mapWidth, newGameMessage.mapHeight);
				Logger.info("Retrying generation with new seed: " + worldSeed);
			}
		}

		mapFactory.preSelectSpawnStep(gameContext, messageDispatcher);
		boolean isChooseSpawnLocation = chooseSpawnLocation(userPreferences);
		if (!isChooseSpawnLocation) {
			mapFactory.postSelectSpawnStep(gameContext, messageDispatcher, buildProfessionList());
		}
		// Trigger context change again for camera to be updated with map
		gameContextRegister.setNewContext(gameContext);
		if (isChooseSpawnLocation) {
			gameContext.getAreaMap().setEmbarkPoint(null);
		} else {
			messageDispatcher.dispatchMessage(MessageType.SETTLEMENT_SPAWNED);
		}

		mainGameScreen.show();
	}

	public static boolean chooseSpawnLocation(UserPreferences userPreferences) {
		return (DEV_MODE && GlobalSettings.CHOOSE_SPAWN_LOCATION) ||
				!Boolean.parseBoolean(userPreferences.getPreference(UserPreferences.PreferenceKey.ENABLE_TUTORIAL));
	}

	private List<Skill> buildProfessionList() {
		List<Skill> professionList = new ArrayList<>();
		if (!DEV_MODE) {
			add(professionList, "MINER", 1);
			add(professionList, "LUMBERJACK", 1);
			add(professionList, "CARPENTER", 1);
			add(professionList, "STONEMASON", 1);
			add(professionList, "BLACKSMITH", 1);
			add(professionList, "FARMER", 1);
			add(professionList, NULL_PROFESSION.getName(), 1);
		} else if (GlobalSettings.STRESS_TEST) {
			List<Skill> allProfessions = new ArrayList<>(skillDictionary.getSelectableProfessions());
			for (int cursor = 0; cursor < 1000; cursor++) {
				professionList.add(allProfessions.get(cursor % allProfessions.size()));
			}
		} else {
			for (int iteration = 0; iteration < 2; iteration++) {
				for (Skill profession : skillDictionary.getSelectableProfessions()) {
					if (profession.getName().equals("VILLAGER")) {
						continue;
					}
					professionList.add(profession);
				}
			}
		}

		return professionList;
	}

	private void add(List<Skill> professionList, String professionName, int quantity) {
		for (int cursor = 0; cursor < quantity; cursor++) {
			Skill profession = skillDictionary.getByName(professionName);
			if (profession == null) {
				Logger.error("Could not find profession by name: " + professionName);
			} else {
				professionList.add(profession);
			}
		}
	}

	public Screen getCurrentScreen() {
		return currentScreen;
	}

	@Override
	public boolean handleMessage(Telegram msg) {

		switch (msg.message) {
			case MessageType.START_NEW_GAME: {
				StartNewGameMessage newGameMessage = (StartNewGameMessage) msg.extraInfo;
				// Reset interaction state so cursor is not left in an odd setting
				messageDispatcher.dispatchMessage(MessageType.SET_HOVER_CURSOR, null);
				messageDispatcher.dispatchMessage(MessageType.SET_SPECIAL_CURSOR, null);
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DEFAULT);
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.DEFAULT_MENU);

				if (currentScreen != null) {
					currentScreen.hide();
				}
				startNewGame(newGameMessage);
				currentScreen = mainGameScreen;
				return true;
			}
			case MessageType.SWITCH_SCREEN: {
				// Reset interaction state so cursor is not left in an odd setting
				messageDispatcher.dispatchMessage(MessageType.SET_HOVER_CURSOR, null);
				messageDispatcher.dispatchMessage(MessageType.SET_SPECIAL_CURSOR, null);
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_INTERACTION_MODE, GameInteractionMode.DEFAULT);
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW_MODE, GameViewMode.DEFAULT);
				if (gameContext != null && gameContext.getSettlementState().getGameState().equals(GameState.SELECT_SPAWN_LOCATION)) {
					messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.SELECT_STARTING_LOCATION);
				} else {
					messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.DEFAULT_MENU);
				}

				String targetScreenName = (String) msg.extraInfo;
				GameScreen targetScreenInstance = gameScreenDictionary.getByName(targetScreenName);
				if (targetScreenInstance != null) {
					String currentScreenName = currentScreen != null ? currentScreen.getName() : "";
					if (!currentScreenName.equals(targetScreenName)) {
						if (currentScreen != null) {
							currentScreen.hide();
						}
						targetScreenInstance.show();
						currentScreen = targetScreenInstance;
						// Disable ambient effects when going to not main game
						messageDispatcher.dispatchMessage(MessageType.AMBIENCE_PAUSE, !targetScreenName.equals("MAIN_GAME"));
					}
				} else {
					Logger.error("Could not find " + GameScreen.class.getSimpleName() + " with name " + targetScreenName);
				}
				return true;
			}
			case MessageType.GUI_SHOW_ERROR: {
				ErrorType errorType = (ErrorType) msg.extraInfo;
				ModalDialog dialog = dialogDictionary.getErrorDialog(errorType);
				currentScreen.showDialog(dialog);
				return true;
			}
			case MessageType.GUI_SHOW_INFO: {
				InfoType infoType = (InfoType) msg.extraInfo;
				ModalDialog dialog = dialogDictionary.getInfoDialog(infoType);
				currentScreen.showDialog(dialog);
				return true;
			}
			case MessageType.SHOW_DIALOG: {
				GameDialog dialog = (GameDialog) msg.extraInfo;
				messageDispatcher.dispatchMessage(MessageType.SET_HOVER_CURSOR, null);
				messageDispatcher.dispatchMessage(MessageType.GUI_REMOVE_ALL_TOOLTIPS);
				currentScreen.showDialog(dialog);
				primaryCameraWrapper.setMovementX(0);
				primaryCameraWrapper.setMovementY(0);
				return true;
			}
			case MessageType.NOTIFY_RESTART_REQUIRED: {
				// TODO this can be inlined to GUI_SHOW_INFO message
				ModalDialog dialog = dialogDictionary.getInfoDialog(InfoType.RESTART_REQUIRED);
				currentScreen.showDialog(dialog);
				return true;
			}
			case MessageType.INITIALISE_SPAWN_POINT: {
				mapFactory.postSelectSpawnStep(gameContext, messageDispatcher, buildProfessionList());
				gameContext.getSettlementState().setGameState(GameState.NORMAL);
				messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.PAUSED);
				messageDispatcher.dispatchMessage(MessageType.GUI_SWITCH_VIEW, GuiViewName.DEFAULT_MENU);
				messageDispatcher.dispatchMessage(MessageType.SETTLEMENT_SPAWNED);
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	public void clearState() {
		entityStore.dispose();
		// Clear out any pending messages
		messageDispatcher.clearQueue(); // FIXME #31 on loading a game need to re-instantiate things on the queue or else reset job state and other timed tasks
	}

	private GameSeed newGameSeed(long seed, int mapWidth, int mapHeight) {
		if (seed == 0L) {
			seed = new RandomXS128().nextLong();
		}

		return new GameSeed(seed, mapWidth, mapHeight);
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}

	private static class GameSeed {

		public final long seed;
		public final int mapWidth;
		public final int mapHeight;

		public GameSeed(long seed, int mapWidth, int mapHeight) {
			this.seed = seed;
			this.mapWidth = mapWidth;
			this.mapHeight = mapHeight;
		}
	}

	public void onResize(int width, int height) {
		if (currentScreen != null) {
			currentScreen.resize(width, height);
		}
	}

}
