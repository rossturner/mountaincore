package technology.rocketjump.saul.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.NotImplementedException;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.logging.CrashHandler;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.misc.twitch.TwitchDataStore;
import technology.rocketjump.saul.misc.twitch.model.TwitchAccountInfo;
import technology.rocketjump.saul.persistence.UserPreferences;
import technology.rocketjump.saul.rendering.ScreenWriter;
import technology.rocketjump.saul.rendering.camera.GlobalSettings;
import technology.rocketjump.saul.rendering.utils.HexColors;
import technology.rocketjump.saul.screens.menus.*;
import technology.rocketjump.saul.screens.menus.options.OptionsTabName;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.i18n.I18nUpdatable;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.GameDialog;
import technology.rocketjump.saul.ui.widgets.I18nTextButton;
import technology.rocketjump.saul.ui.widgets.I18nWidgetFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static technology.rocketjump.saul.persistence.UserPreferences.PreferenceKey.TWITCH_INTEGRATION_ENABLED;
import static technology.rocketjump.saul.rendering.camera.DisplaySettings.GUI_DESIGN_SIZE;
import static technology.rocketjump.saul.rendering.camera.GlobalSettings.VERSION;

/**
 * Just to show some basic start game / options / quit etc.
 * <p>
 * Should slowly pan across a background image
 */
@Singleton
public class MainMenuScreen implements Telegraph, GameScreen, I18nUpdatable, GameContextAware {

	private static final float PIXEL_SCROLL_PER_SECOND = 70f;
	private final MessageDispatcher messageDispatcher;
	private final ScreenWriter screenWriter;
	private final TopLevelMenu topLevelMenu;
	private final OptionsMenu optionsMenu;
	private final ModsMenu modsMenu;
	private final EmbarkMenu embarkMenu;
	private final LoadGameMenu loadGameMenu;
	private final Skin uiSkin;
	private final SpriteBatch basicSpriteBatch = new SpriteBatch();
	private final OrthographicCamera camera = new OrthographicCamera();
	private final Viewport viewport = new ExtendViewport(GUI_DESIGN_SIZE.x, GUI_DESIGN_SIZE.y);

	private Texture backgroundImage;
	private float backgroundScale = 1f;
	private GridPoint2 backgroundOffset = new GridPoint2();
	private TextureRegion backgroundRegion;
	private GridPoint2 backgroundRegionSize;


	private final Stage stage;

	private final Table containerTable;
	private final Table versionTable;

	private Menu currentMenu;

	private final UserPreferences userPreferences;
	private final TwitchDataStore twitchDataStore;
	private final I18nTranslator i18nTranslator;

	@Inject
	public MainMenuScreen(MessageDispatcher messageDispatcher, ScreenWriter screenWriter, EmbarkMenu embarkMenu,
						  LoadGameMenu loadGameMenu, GuiSkinRepository guiSkinRepository,
						  UserPreferences userPreferences, TopLevelMenu topLevelMenu, OptionsMenu optionsMenu,
						  PrivacyOptInMenu privacyOptInMenu, CrashHandler crashHandler, I18nWidgetFactory i18nWidgetFactory,
						  ModsMenu modsMenu, TwitchDataStore twitchDataStore, I18nTranslator i18nTranslator) {
		this.messageDispatcher = messageDispatcher;
		this.screenWriter = screenWriter;
		this.embarkMenu = embarkMenu;
		this.loadGameMenu = loadGameMenu;
		this.uiSkin = guiSkinRepository.getDefault();
		this.topLevelMenu = topLevelMenu;
		this.optionsMenu = optionsMenu;
		this.modsMenu = modsMenu;
		this.userPreferences = userPreferences;
		this.twitchDataStore = twitchDataStore;
		this.i18nTranslator = i18nTranslator;

		containerTable = new Table(uiSkin);
		containerTable.setFillParent(true);
		containerTable.center();

		stage = new Stage(viewport);
		stage.addActor(containerTable);

		versionTable = new Table(uiSkin);
		versionTable.setFillParent(true);
		versionTable.left().bottom();
		String versionText = VERSION.toString();
		if (GlobalSettings.DEV_MODE) {
			versionText += " (DEV MODE ENABLED)";
		}
		versionTable.add(new Label(versionText, uiSkin)).left().pad(5);
		I18nTextButton newVersionButton = i18nWidgetFactory.createTextButton("GUI.NEW_VERSION_AVAILABLE");
		newVersionButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				Gdx.net.openURI("https://rocketjumptechnology.itch.io/king-under-the-mountain");
			}
		});

		I18nTextButton viewRoadmapButton = i18nWidgetFactory.createTextButton("GUI.VIEW_ROADMAP");
		viewRoadmapButton.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				Gdx.net.openURI("http://kingunderthemounta.in/roadmap/");
			}
		});

//		stage.addActor(versionTable);

		if (crashHandler.isOptInConfirmationRequired()) {
			currentMenu = privacyOptInMenu;
		} else {
			currentMenu = topLevelMenu;
		}
		currentMenu.show();

		messageDispatcher.addListener(this, MessageType.SWITCH_MENU);
		messageDispatcher.addListener(this, MessageType.TWITCH_ACCOUNT_INFO_UPDATED);
		messageDispatcher.addListener(this, MessageType.PREFERENCE_CHANGED);
		messageDispatcher.addListener(this, MessageType.SAVED_GAMES_LIST_UPDATED);
		messageDispatcher.addListener(this, MessageType.START_NEW_GAME);
		messageDispatcher.addListener(this, MessageType.PERFORM_LOAD);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.SWITCH_MENU: {
				MenuType targetMenuType = (MenuType) msg.extraInfo;
				MenuType currentMenuType = MenuType.byInstance(currentMenu);
				if (!targetMenuType.equals(currentMenuType)) {
					if (currentMenu != null) {
						currentMenu.hide();
					}
					switch (targetMenuType) {
						case TOP_LEVEL_MENU:
							currentMenu = topLevelMenu;
							break;
						case TWITCH_OPTIONS_MENU:
							optionsMenu.setCurrentTab(OptionsTabName.TWITCH);
						case PRIVACY_OPT_IN_MENU:
						case OPTIONS_MENU:
							currentMenu = optionsMenu;
							break;
						case EMBARK_MENU:
							currentMenu = embarkMenu;
							break;
						case LOAD_GAME_MENU:
							currentMenu = loadGameMenu;
							break;
						case MODS_MENU:
							currentMenu = modsMenu;
							break;
						default:
							throw new NotImplementedException("not yet implemented:" + targetMenuType.name());
					}
					currentMenu.show();
					reset();
				}
				return true;
			}
			case MessageType.TWITCH_ACCOUNT_INFO_UPDATED: {
				resetVersionTable();
				return false;
			}
			case MessageType.PREFERENCE_CHANGED: {
				UserPreferences.PreferenceKey changedPreference = (UserPreferences.PreferenceKey) msg.extraInfo;
				if (changedPreference.equals(TWITCH_INTEGRATION_ENABLED)) {
					resetVersionTable();
					return true;
				} else {
					return false;
				}
			}
			case MessageType.SAVED_GAMES_LIST_UPDATED: {
				topLevelMenu.savedGamesUpdated();
				loadGameMenu.savedGamesUpdated();
				return true;
			}
			case MessageType.PERFORM_LOAD:
			case MessageType.START_NEW_GAME: {
				topLevelMenu.gameStarted();
				return false;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this + ", " + msg);
		}
	}

	private void reset() {
		List<Dialog> outstandingDialogs = new ArrayList<>();
		for (Actor child : stage.getRoot().getChildren()) {
			if (child instanceof Dialog) {
				outstandingDialogs.add((Dialog) child);
			}
		}

		stage.clear();

		containerTable.clearChildren();
		currentMenu.reset();
		currentMenu.populate(containerTable);

		stage.addActor(containerTable);
		stage.addActor(versionTable);

		for (Dialog outstandingDialog : outstandingDialogs) {
			stage.addActor(outstandingDialog);
		}
	}

	@Override
	public void show() {
		if (new RandomXS128().nextBoolean()) {
			backgroundImage = new Texture("assets/main_menu/Dwarves.jpg");
		} else {
			backgroundImage = new Texture("assets/main_menu/Dwarf Realm.jpg");
		}

		setupBackgroundRegion();

		InputMultiplexer inputMultiplexer = new InputMultiplexer();
		inputMultiplexer.addProcessor(stage);
		inputMultiplexer.addProcessor(new MainMenuInputHandler());
		Gdx.input.setInputProcessor(inputMultiplexer);

		resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	}

	private void setupBackgroundRegion() {
		backgroundScale = 1f;
		determineBackgroundScale();
		determineBackgroundOffset();
		backgroundRegion = new TextureRegion(backgroundImage, backgroundOffset.x, backgroundOffset.y,
				backgroundRegionSize.x, backgroundRegionSize.y);
	}

	private void determineBackgroundScale() {
		if (backgroundImage.getWidth() < Gdx.graphics.getWidth() || backgroundImage.getHeight() < Gdx.graphics.getHeight()) {
			backgroundScale = 2f;
		} else {
			while (canHalveBackgroundResolution()) {
				backgroundScale *= 0.5f;
			}
		}
		backgroundRegionSize = new GridPoint2(
				Math.round(Gdx.graphics.getWidth() * (1f / backgroundScale)),
				Math.round(Gdx.graphics.getHeight() * (1f / backgroundScale))
		);
	}

	private void determineBackgroundOffset() {
		GridPoint2 maxOffset = new GridPoint2(
				backgroundImage.getWidth() - backgroundRegionSize.x,
				backgroundImage.getHeight() - backgroundRegionSize.y
		);
		Random random = new RandomXS128();
		this.backgroundOffset.set(
				random.nextInt(maxOffset.x), random.nextInt(maxOffset.y)
		);
	}

	private boolean canHalveBackgroundResolution() {
		return (float) backgroundImage.getWidth() * backgroundScale > Gdx.graphics.getWidth() * 2f &&
				(float) backgroundImage.getHeight() * backgroundScale > Gdx.graphics.getHeight() * 2f;
	}

	@Override
	public void hide() {
		backgroundImage.dispose();
		backgroundImage = null;
	}

	@Override
	public void render(float delta) {
		camera.update();
		stage.act(delta);

		// Show middle section of background from xCursor to xCursor + width
		basicSpriteBatch.begin();
		basicSpriteBatch.setProjectionMatrix(camera.combined);
		basicSpriteBatch.draw(backgroundRegion, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		basicSpriteBatch.end();

		stage.draw();

		screenWriter.render();
	}

	@Override
	public String getName() {
		return "MAIN_MENU";
	}

	@Override
	public void showDialog(GameDialog dialog) {
		dialog.show(stage);
	}

	@Override
	public void resize(int width, int height) {
		camera.setToOrtho(false, width, height);

		setupBackgroundRegion();

		viewport.update(width, height, true);

		reset();
	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void dispose() {

	}

	private void resetVersionTable() {

		versionTable.clearChildren();
		versionTable.top().left().padTop(50f).padLeft(25f);

		if (twitchEnabled()) {
			TwitchAccountInfo accountInfo = twitchDataStore.getAccountInfo();
			Label twitchLabel;
			if (accountInfo == null) {
				String twitchLabelText = i18nTranslator.getTranslatedString("GUI.OPTIONS.TWITCH.DISCONNECTED_LABEL").toString();
				twitchLabel = new Label(twitchLabelText, uiSkin);
				twitchLabel.setColor(HexColors.NEGATIVE_COLOR);
			} else {
				String twitchLabelText = i18nTranslator.getTranslatedString("GUI.OPTIONS.TWITCH.CONNECTED_LABEL").toString() + " (" + accountInfo.getLogin() + ")";
				twitchLabel = new Label(twitchLabelText, uiSkin);
			}
			versionTable.add(twitchLabel).colspan(3).left().pad(5).row();
		}

		String versionText = VERSION.toString();
		if (GlobalSettings.DEV_MODE) {
			versionText += " (DEV MODE ENABLED)";
		}
		versionTable.add(new Label(versionText, uiSkin)).left().pad(5).row();
	}

	private boolean twitchEnabled() {
		return Boolean.parseBoolean(userPreferences.getPreference(UserPreferences.PreferenceKey.TWITCH_INTEGRATION_ENABLED, "false"));
	}

	@Override
	public void onLanguageUpdated() {
		// Not translated but needs triggering for font change
		resetVersionTable();
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		resetVersionTable();
		reset();
	}

	@Override
	public void clearContextRelatedState() {
		resetVersionTable();
		reset();
	}

	private static class MainMenuInputHandler implements InputProcessor {

		@Override
		public boolean keyDown(int keycode) {
			return false;
		}

		@Override
		public boolean keyUp(int keycode) {
			return false;
		}

		@Override
		public boolean keyTyped(char character) {
			return false;
		}

		@Override
		public boolean touchDown(int screenX, int screenY, int pointer, int button) {
			return false;
		}

		@Override
		public boolean touchUp(int screenX, int screenY, int pointer, int button) {
			return false;
		}

		@Override
		public boolean touchDragged(int screenX, int screenY, int pointer) {
			return false;
		}

		@Override
		public boolean mouseMoved(int screenX, int screenY) {
			return false;
		}

		@Override
		public boolean scrolled(float amountX, float amountY) {
			return false;
		}
	}
}
