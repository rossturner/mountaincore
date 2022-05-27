package technology.rocketjump.saul;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.google.inject.ConfigurationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.spi.Message;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;
import technology.rocketjump.saul.assets.AssetDisposable;
import technology.rocketjump.saul.assets.AssetDisposableRegister;
import technology.rocketjump.saul.assets.TextureAtlasRepository;
import technology.rocketjump.saul.audio.AudioUpdater;
import technology.rocketjump.saul.constants.ConstantsRepo;
import technology.rocketjump.saul.entities.tags.TagProcessor;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.gamecontext.GameContextRegister;
import technology.rocketjump.saul.gamecontext.GameUpdateRegister;
import technology.rocketjump.saul.gamecontext.Updatable;
import technology.rocketjump.saul.guice.SaulGuiceModule;
import technology.rocketjump.saul.logging.CrashHandler;
import technology.rocketjump.saul.messaging.InfoType;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.async.BackgroundTaskManager;
import technology.rocketjump.saul.messaging.types.GameSaveMessage;
import technology.rocketjump.saul.misc.AnalyticsManager;
import technology.rocketjump.saul.misc.twitch.TwitchMessageHandler;
import technology.rocketjump.saul.misc.twitch.TwitchTaskRunner;
import technology.rocketjump.saul.modding.LocalModRepository;
import technology.rocketjump.saul.modding.model.ParsedMod;
import technology.rocketjump.saul.persistence.UserFileManager;
import technology.rocketjump.saul.rendering.GameRenderer;
import technology.rocketjump.saul.rendering.ScreenWriter;
import technology.rocketjump.saul.rendering.camera.PrimaryCameraWrapper;
import technology.rocketjump.saul.screens.GameScreenDictionary;
import technology.rocketjump.saul.screens.ScreenManager;
import technology.rocketjump.saul.screens.menus.OptionsMenu;
import technology.rocketjump.saul.screens.menus.options.OptionsTab;
import technology.rocketjump.saul.ui.GuiContainer;
import technology.rocketjump.saul.ui.I18nUpdatableRegister;
import technology.rocketjump.saul.ui.cursor.CursorManager;
import technology.rocketjump.saul.ui.i18n.I18nRepo;
import technology.rocketjump.saul.ui.i18n.I18nUpdatable;
import technology.rocketjump.saul.ui.views.TimeDateGuiView;
import technology.rocketjump.saul.ui.widgets.ImageButtonFactory;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class SaulApplicationAdapter extends ApplicationAdapter {

	private GameRenderer gameRenderer;
	private PrimaryCameraWrapper primaryCameraWrapper;
	private ScreenWriter screenWriter;
	private MessageDispatcher messageDispatcher;
	private BackgroundTaskManager backgroundTaskManager; // Unused directly but needs creating for dispatched messages
	private CursorManager cursorManager; // Also unused directly
	private ImageButtonFactory imageButtonFactory; // Unused, to init profession image buttons
	private I18nUpdatableRegister i18nUpdatableRegister;
	private AssetDisposableRegister assetDisposableRegister;
	private GuiContainer guiContainer;
	private GameContextRegister gameContextRegister;
	private GameUpdateRegister gameUpdateRegister;
	private AudioUpdater audioUpdater;
	private ScreenManager screenManager;
	private ConstantsRepo constantsRepo;
	private TwitchTaskRunner twitchTaskRunner;

	@Override
	public void create() {
		try {
			Injector injector = Guice.createInjector(new SaulGuiceModule());

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
			i18nUpdatableRegister = injector.getInstance(I18nUpdatableRegister.class);
			assetDisposableRegister = injector.getInstance(AssetDisposableRegister.class);
			UserFileManager userFileManager = injector.getInstance(UserFileManager.class);

			Reflections reflections = new Reflections("technology.rocketjump.saul", new SubTypesScanner());
			Set<Class<? extends Updatable>> updateableClasses = reflections.getSubTypesOf(Updatable.class);
			updateableClasses.forEach(this::checkForSingleton);
			gameUpdateRegister.registerClasses(updateableClasses, injector);

			// Get all implementations of GameContextAware and instantiate them
			Set<Class<? extends GameContextAware>> gameContextAwareClasses = reflections.getSubTypesOf(GameContextAware.class);
			gameContextAwareClasses.forEach(this::checkForSingleton);
			gameContextRegister.registerClasses(gameContextAwareClasses, injector);

			Set<Class<? extends I18nUpdatable>> i18nUpdateableClasses = reflections.getSubTypesOf(I18nUpdatable.class);
			i18nUpdateableClasses.forEach(this::checkForSingleton);
			i18nUpdatableRegister.registerClasses(i18nUpdateableClasses, injector);

			Set<Class<? extends AssetDisposable>> assetUpdatableClasses = reflections.getSubTypesOf(AssetDisposable.class);
			assetUpdatableClasses.forEach(this::checkForSingleton);
			assetDisposableRegister.registerClasses(assetUpdatableClasses, injector);

			Set<Class<? extends OptionsTab>> optionsTabClasses = reflections.getSubTypesOf(OptionsTab.class);
			List<OptionsTab> optionsTabInstances = new ArrayList<>();
			for (Class<? extends OptionsTab> optionsTabClass : optionsTabClasses) {
				if (!optionsTabClass.isInterface()) {
					optionsTabInstances.add(injector.getInstance(optionsTabClass));
				}
			}
			injector.getInstance(OptionsMenu.class).setTabImplementations(optionsTabInstances);

			injector.getInstance(TagProcessor.class).init();
			injector.getInstance(TimeDateGuiView.class).init(injector.getInstance(GameScreenDictionary.class));

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
		}
	}

	@Override
	public void render() {
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
			onExit();
		}
	}

	public void onExit() {
		AnalyticsManager.stopAnalytics();
		messageDispatcher.dispatchMessage(MessageType.PERFORM_SAVE, new GameSaveMessage(false));
		messageDispatcher.dispatchMessage(MessageType.SHUTDOWN_IN_PROGRESS);
		Gdx.app.exit();
	}

	private void checkForSingleton(Class aClass) {
		if (!aClass.isInterface() && !Modifier.isAbstract(aClass.getModifiers()) && !(
				aClass.isAnnotationPresent(javax.inject.Singleton.class) || aClass.isAnnotationPresent(com.google.inject.Singleton.class)
		)) {
			throw new ConfigurationException(Arrays.asList(new Message(aClass.getName() + " must be annotated with Singleton")));
		}
	}

	@Override
	public void resize(int width, int height) {
		if (width == 0 || height == 0) {
			return;
		}
		try {
			primaryCameraWrapper.onResize(width, height);
			screenWriter.onResize(width, height);
			gameRenderer.onResize(width, height);
			guiContainer.onResize(width, height);
			screenManager.onResize(width, height);
		} catch (Exception e) {
			CrashHandler.logCrash(e);
			throw e;
		}
	}

}
