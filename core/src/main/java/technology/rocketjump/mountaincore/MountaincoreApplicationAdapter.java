package technology.rocketjump.mountaincore;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.codedisaster.steamworks.SteamAPI;
import com.codedisaster.steamworks.SteamException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.pmw.tinylog.Logger;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import technology.rocketjump.mountaincore.assets.AssetDisposable;
import technology.rocketjump.mountaincore.assets.AssetDisposableRegister;
import technology.rocketjump.mountaincore.assets.TextureAtlasRepository;
import technology.rocketjump.mountaincore.audio.AudioUpdater;
import technology.rocketjump.mountaincore.constants.ConstantsRepo;
import technology.rocketjump.mountaincore.entities.tags.TagProcessor;
import technology.rocketjump.mountaincore.gamecontext.GameContextAware;
import technology.rocketjump.mountaincore.gamecontext.GameContextRegister;
import technology.rocketjump.mountaincore.gamecontext.GameUpdateRegister;
import technology.rocketjump.mountaincore.gamecontext.Updatable;
import technology.rocketjump.mountaincore.guice.MountaincoreGuiceModule;
import technology.rocketjump.mountaincore.logging.CrashHandler;
import technology.rocketjump.mountaincore.messaging.InfoType;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.async.BackgroundTaskManager;
import technology.rocketjump.mountaincore.messaging.types.GameSaveMessage;
import technology.rocketjump.mountaincore.misc.AnalyticsManager;
import technology.rocketjump.mountaincore.misc.twitch.TwitchMessageHandler;
import technology.rocketjump.mountaincore.misc.twitch.TwitchTaskRunner;
import technology.rocketjump.mountaincore.modding.LocalModRepository;
import technology.rocketjump.mountaincore.modding.ModSyncMessageHandler;
import technology.rocketjump.mountaincore.modding.authentication.ModioRequestAdapter;
import technology.rocketjump.mountaincore.modding.authentication.SteamUserManager;
import technology.rocketjump.mountaincore.modding.model.ParsedMod;
import technology.rocketjump.mountaincore.persistence.UserFileManager;
import technology.rocketjump.mountaincore.rendering.GameRenderer;
import technology.rocketjump.mountaincore.rendering.ScreenWriter;
import technology.rocketjump.mountaincore.rendering.camera.GlobalSettings;
import technology.rocketjump.mountaincore.rendering.camera.PrimaryCameraWrapper;
import technology.rocketjump.mountaincore.screens.ScreenManager;
import technology.rocketjump.mountaincore.screens.menus.OptionsMenu;
import technology.rocketjump.mountaincore.screens.menus.options.OptionsTab;
import technology.rocketjump.mountaincore.ui.DisplaysTextRegister;
import technology.rocketjump.mountaincore.ui.GuiContainer;
import technology.rocketjump.mountaincore.ui.cursor.CursorManager;
import technology.rocketjump.mountaincore.ui.i18n.DisplaysText;
import technology.rocketjump.mountaincore.ui.i18n.I18nRepo;
import technology.rocketjump.mountaincore.ui.views.TimeDateGuiView;
import technology.rocketjump.mountaincore.ui.widgets.ImageButtonFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class MountaincoreApplicationAdapter extends ApplicationAdapter {

	private GameRenderer gameRenderer;
	private PrimaryCameraWrapper primaryCameraWrapper;
	private ScreenWriter screenWriter;
	private MessageDispatcher messageDispatcher;
	private BackgroundTaskManager backgroundTaskManager; // Unused directly but needs creating for dispatched messages
	private CursorManager cursorManager;
	private ImageButtonFactory imageButtonFactory; // Unused, to init profession image buttons
	private DisplaysTextRegister displaysTextRegister;
	private AssetDisposableRegister assetDisposableRegister;
	private GuiContainer guiContainer;
	private GameContextRegister gameContextRegister;
	private GameUpdateRegister gameUpdateRegister;
	private AudioUpdater audioUpdater;
	private ScreenManager screenManager;
	private ConstantsRepo constantsRepo;
	private TwitchTaskRunner twitchTaskRunner;
	private ModioRequestAdapter modioRequestAdapter;
	private boolean crashHappened;

	@Override
	public void create() {
		initSteamAPI();
		try {
			Injector injector = Guice.createInjector(new MountaincoreGuiceModule());

			injector.getInstance(I18nRepo.class).init(injector.getInstance(TextureAtlasRepository.class));

			injector.getInstance(TwitchMessageHandler.class);
			screenWriter = injector.getInstance(ScreenWriter.class);
			gameRenderer = injector.getInstance(GameRenderer.class);
			screenWriter = injector.getInstance(ScreenWriter.class);
			primaryCameraWrapper = injector.getInstance(PrimaryCameraWrapper.class);
			messageDispatcher = injector.getInstance(MessageDispatcher.class);
			backgroundTaskManager = injector.getInstance(BackgroundTaskManager.class);
			twitchTaskRunner = injector.getInstance(TwitchTaskRunner.class);

			guiContainer = injector.getInstance(GuiContainer.class);
			cursorManager = injector.getInstance(CursorManager.class);
			imageButtonFactory = injector.getInstance(ImageButtonFactory.class);

			screenManager = injector.getInstance(ScreenManager.class);
			audioUpdater = injector.getInstance(AudioUpdater.class);
			constantsRepo = injector.getInstance(ConstantsRepo.class);

			gameContextRegister = injector.getInstance(GameContextRegister.class);
			gameUpdateRegister = injector.getInstance(GameUpdateRegister.class);
			displaysTextRegister = injector.getInstance(DisplaysTextRegister.class);
			assetDisposableRegister = injector.getInstance(AssetDisposableRegister.class);
			modioRequestAdapter = injector.getInstance(ModioRequestAdapter.class);
			UserFileManager userFileManager = injector.getInstance(UserFileManager.class);

			Reflections reflections = new Reflections("technology.rocketjump.mountaincore", new SubTypesScanner());
			Set<Class<? extends Updatable>> updateableClasses = reflections.getSubTypesOf(Updatable.class);
			updateableClasses.forEach(MountaincoreGuiceModule::checkForSingleton);
			gameUpdateRegister.registerClasses(updateableClasses, injector);

			// Get all implementations of GameContextAware and instantiate them
			Set<Class<? extends GameContextAware>> gameContextAwareClasses = reflections.getSubTypesOf(GameContextAware.class);
			gameContextAwareClasses.forEach(MountaincoreGuiceModule::checkForSingleton);
			gameContextRegister.registerClasses(gameContextAwareClasses, injector);

			Set<Class<? extends DisplaysText>> displaysTextClasses = reflections.getSubTypesOf(DisplaysText.class);
			displaysTextClasses.forEach(MountaincoreGuiceModule::checkForSingleton);
			displaysTextRegister.registerClasses(displaysTextClasses, injector);

			Set<Class<? extends AssetDisposable>> assetDisposableClasses = reflections.getSubTypesOf(AssetDisposable.class);
			assetDisposableClasses.forEach(MountaincoreGuiceModule::checkForSingleton);
			assetDisposableRegister.registerClasses(assetDisposableClasses, injector);

			Set<Class<? extends OptionsTab>> optionsTabClasses = reflections.getSubTypesOf(OptionsTab.class);
			List<OptionsTab> optionsTabInstances = new ArrayList<>();
			for (Class<? extends OptionsTab> optionsTabClass : optionsTabClasses) {
				if (!optionsTabClass.isInterface()) {
					optionsTabInstances.add(injector.getInstance(optionsTabClass));
				}
			}
			injector.getInstance(OptionsMenu.class).setTabImplementations(optionsTabInstances);

			injector.getInstance(TagProcessor.class).init();
			injector.getInstance(TimeDateGuiView.class).rebuildUI();
			injector.getInstance(SteamUserManager.class);
			injector.getInstance(ModSyncMessageHandler.class);

			messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_MENU");

			messageDispatcher.dispatchMessage(MessageType.LANGUAGE_CHANGED);
			AnalyticsManager.startAnalytics(userFileManager.readClientId());

			LocalModRepository localModRepository = injector.getInstance(LocalModRepository.class);
			List<ParsedMod> incompatibleMods = localModRepository.getIncompatibleMods();
			if (!incompatibleMods.isEmpty()) {
				messageDispatcher.dispatchMessage(MessageType.GUI_SHOW_INFO, InfoType.MOD_INCOMPATIBLE);
				// Update preference string
				localModRepository.setActiveMods(localModRepository.getActiveMods());
			}

		} catch (Throwable e) {
			CrashHandler.logCrash(e);
			Gdx.app.exit();
			crashHappened = true;
			submitErrorDialog(e);
		}
	}

	@Override
	public void render() {
		if (SteamAPI.isSteamRunning()) {
			SteamAPI.runCallbacks();
		}
		try {
			Color bgColor = constantsRepo.getWorldConstants().getBackgroundColorInstance();
			Gdx.gl.glClearColor(bgColor.r, bgColor.g, bgColor.b, bgColor.a);
			Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
			float deltaTime = Math.min(Gdx.graphics.getDeltaTime(), 1f / 15f); // Force longer duration frames to behave as though they're at 15fps

			messageDispatcher.update();
			screenManager.getCurrentScreen().render(deltaTime);
			audioUpdater.update();
			twitchTaskRunner.update(deltaTime);
			backgroundTaskManager.update(deltaTime);
		} catch (Throwable e) {
			CrashHandler.logCrash(e);
			crashHappened = true;
			onExit();
			submitErrorDialog(e);
		}
	}

	public void onExit() {
		AnalyticsManager.stopAnalytics();
		if (!crashHappened) {
			messageDispatcher.dispatchMessage(MessageType.PERFORM_SAVE, new GameSaveMessage(false));
		}
		messageDispatcher.dispatchMessage(MessageType.SHUTDOWN_IN_PROGRESS);
		try { SteamAPI.shutdown(); } catch (Throwable ignored) {}
		backgroundTaskManager.shutdown();
		modioRequestAdapter.dispose();
		Gdx.app.exit();
	}

	@Override
	public void resize(int width, int height) {
		if (width == 0 || height == 0 || crashHappened) {
			return;
		}
		try {
			primaryCameraWrapper.onResize(width, height);
			screenWriter.onResize(width, height);
			gameRenderer.onResize(width, height);
			guiContainer.onResize(width, height);
			screenManager.onResize(width, height);
			cursorManager.onResize();
		} catch (Exception e) {
			CrashHandler.logCrash(e);
			throw e;
		}
	}

	@Override
	public void dispose() {
		super.dispose();
		onExit();
	}

	private void submitErrorDialog(Throwable e) {
		Gdx.app.addLifecycleListener(new LifecycleListener() {
			@Override
			public void pause() {

			}

			@Override
			public void resume() {

			}

			@Override
			public void dispose() {
				CrashHandler.displayCrashDialog(e);
			}
		});
	}

	private static void initSteamAPI() {
		try {
			SteamAPI.loadLibraries();
			if (!SteamAPI.init() && GlobalSettings.DEV_MODE) {
				Logger.info("Steam API init() failed, probably not running under Steam");
			}
		} catch (Throwable e) {
			Logger.error(e, "SteamAPI.loadLibraries() failed");
		}

	}

}
