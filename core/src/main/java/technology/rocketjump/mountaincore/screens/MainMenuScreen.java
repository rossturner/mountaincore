package technology.rocketjump.mountaincore.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.NotImplementedException;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.GameContextAware;
import technology.rocketjump.mountaincore.logging.CrashHandler;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.misc.twitch.TwitchDataStore;
import technology.rocketjump.mountaincore.misc.twitch.model.TwitchAccountInfo;
import technology.rocketjump.mountaincore.persistence.UserPreferences;
import technology.rocketjump.mountaincore.rendering.ScreenWriter;
import technology.rocketjump.mountaincore.rendering.camera.GlobalSettings;
import technology.rocketjump.mountaincore.screens.menus.*;
import technology.rocketjump.mountaincore.screens.menus.options.OptionsTabName;
import technology.rocketjump.mountaincore.ui.i18n.DisplaysText;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;
import technology.rocketjump.mountaincore.ui.skins.GuiSkinRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Stack;

import static technology.rocketjump.mountaincore.rendering.camera.GlobalSettings.VERSION;

/**
 * Just to show some basic start game / options / quit etc.
 * <p>
 * Should slowly pan across a background image
 */
@Singleton
public class MainMenuScreen extends AbstractGameScreen implements Telegraph, DisplaysText, GameContextAware {

	private final MessageDispatcher messageDispatcher;
	private final ScreenWriter screenWriter;
	private final TopLevelMenu topLevelMenu;
	private final OptionsMenu optionsMenu;
	private final EmbarkMenu embarkMenu;
	private final LoadGameMenu loadGameMenu;
	private final CreditsMenu creditsMenu;
	private final Skin uiSkin;
	private final SpriteBatch basicSpriteBatch = new SpriteBatch();

	private Texture backgroundImage;
	private float backgroundScale = 1f;
	private GridPoint2 backgroundOffset = new GridPoint2();
	private TextureRegion backgroundRegion;
	private GridPoint2 backgroundRegionSize;

	private final Table containerTable;
	private final Table versionTable;

	private Stack<Menu> traversedMenus = new Stack<>();

	private final UserPreferences userPreferences;
	private final TwitchDataStore twitchDataStore;
	private final I18nTranslator i18nTranslator;

	@Inject
	public MainMenuScreen(MessageDispatcher messageDispatcher, ScreenWriter screenWriter, EmbarkMenu embarkMenu,
						  LoadGameMenu loadGameMenu, GuiSkinRepository guiSkinRepository,
						  UserPreferences userPreferences, TopLevelMenu topLevelMenu, OptionsMenu optionsMenu,
						  PrivacyOptInMenu privacyOptInMenu, CrashHandler crashHandler,
						  TwitchDataStore twitchDataStore, I18nTranslator i18nTranslator,
						  CreditsMenu creditsMenu) {
		super(userPreferences, messageDispatcher);
		this.messageDispatcher = messageDispatcher;
		this.screenWriter = screenWriter;
		this.embarkMenu = embarkMenu;
		this.loadGameMenu = loadGameMenu;
		this.uiSkin = guiSkinRepository.getMenuSkin();
		this.topLevelMenu = topLevelMenu;
		this.optionsMenu = optionsMenu;
		this.userPreferences = userPreferences;
		this.twitchDataStore = twitchDataStore;
		this.i18nTranslator = i18nTranslator;
		this.creditsMenu = creditsMenu;

		containerTable = new Table(uiSkin);
		containerTable.setFillParent(true);
		containerTable.center();

		stage.addActor(containerTable);

		versionTable = new Table(uiSkin);
		versionTable.setFillParent(true);
		versionTable.left().bottom();

		setCurrentMenu(topLevelMenu);
		if (crashHandler.isOptInConfirmationRequired()) {
			setCurrentMenu(privacyOptInMenu);
		}
		showCurrentMenu();

		messageDispatcher.addListener(this, MessageType.SWITCH_MENU);
		messageDispatcher.addListener(this, MessageType.MENU_ESCAPED);
		messageDispatcher.addListener(this, MessageType.TWITCH_ACCOUNT_INFO_UPDATED);
		messageDispatcher.addListener(this, MessageType.PREFERENCE_CHANGED);
		messageDispatcher.addListener(this, MessageType.SAVED_GAMES_LIST_UPDATED);
		messageDispatcher.addListener(this, MessageType.START_NEW_GAME);
		messageDispatcher.addListener(this, MessageType.PERFORM_LOAD);
		messageDispatcher.addListener(this, MessageType.DEV_MODE_CHANGED);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.MENU_ESCAPED: {
				if (getCurrentMenu() == topLevelMenu && topLevelMenu.hasGameStarted()) {
					messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_GAME");
				} else {
					goBackMenu();
					reset();
				}
				return true;
			}
			case MessageType.SWITCH_MENU: {
				MenuType targetMenuType = (MenuType) msg.extraInfo;
				MenuType currentMenuType = MenuType.byInstance(getCurrentMenu());
				if (!targetMenuType.equals(currentMenuType)) {
					if (getCurrentMenu() != null) {
						getCurrentMenu().hide();
					}
					switch (targetMenuType) {
						case TOP_LEVEL_MENU:
							setCurrentMenu(topLevelMenu);
							break;
						case TWITCH_OPTIONS_MENU:
							optionsMenu.setCurrentTab(OptionsTabName.TWITCH);
						case PRIVACY_OPT_IN_MENU:
						case OPTIONS_MENU:
							setCurrentMenu(optionsMenu);
							break;
						case EMBARK_MENU:
							setCurrentMenu(embarkMenu);
							break;
						case LOAD_GAME_MENU:
							setCurrentMenu(loadGameMenu);
							break;
						case CREDITS_MENU:
							setCurrentMenu(creditsMenu);
							break;
						default:
							throw new NotImplementedException("not yet implemented:" + targetMenuType.name());
					}
					showCurrentMenu();
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
				if (changedPreference.equals(UserPreferences.PreferenceKey.TWITCH_INTEGRATION_ENABLED)) {
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
			case MessageType.DEV_MODE_CHANGED: {
				resetVersionTable();
				return true;
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
		getCurrentMenu().reset();
		getCurrentMenu().populate(containerTable);

		stage.addActor(containerTable);
		if (getCurrentMenu().showVersionDetails()) {
			stage.addActor(versionTable);
		}

		for (Dialog outstandingDialog : outstandingDialogs) {
			stage.addActor(outstandingDialog);
		}
	}

	@Override
	public void show() {
		backgroundImage = new Texture("assets/main_menu/Dwarven Settlement.png");

		setupBackgroundRegion();

		InputMultiplexer inputMultiplexer = new InputMultiplexer();
		inputMultiplexer.addProcessor(stage);
		inputMultiplexer.addProcessor(new MainMenuInputHandler(messageDispatcher));
		Gdx.input.setInputProcessor(inputMultiplexer);

		resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
	}


	private Menu getCurrentMenu() {
		return traversedMenus.peek();
	}

	private void showCurrentMenu() {
		getCurrentMenu().show();
	}

	private void goBackMenu() {
		getCurrentMenu().hide();
		if (traversedMenus.size() == 1) {
			setCurrentMenu(topLevelMenu);
		} else {
			traversedMenus.pop();
		}
		showCurrentMenu();
	}

	private void setCurrentMenu(Menu menu) {
		if (menu == topLevelMenu) {
			traversedMenus.clear();
		}
		traversedMenus.push(menu);
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
	public void resize(int width, int height) {
		camera.setToOrtho(false, width, height);

		setupBackgroundRegion();

		viewport.update(width, height, true);

		reset();
	}


	@Override
	public void dispose() {

	}

	private void resetVersionTable() {

		versionTable.clearChildren();
		versionTable.top().left().padTop(100f).padLeft(50f);

		String versionText = VERSION.toString();
		if (GlobalSettings.DEV_MODE) {
			versionText += " (DEV MODE ENABLED)";
		}
		versionTable.add(new Label(versionText, uiSkin.get("white_text", Label.LabelStyle.class))).left().pad(5).row();

		if (twitchEnabled()) {
			TwitchAccountInfo accountInfo = twitchDataStore.getAccountInfo();
			Label twitchLabel;
			if (accountInfo == null) {
				String twitchLabelText = i18nTranslator.getTranslatedString("GUI.OPTIONS.TWITCH.DISCONNECTED_LABEL").toString();
				twitchLabel = new Label(twitchLabelText, uiSkin.get("white_text", Label.LabelStyle.class));
			} else {
				String twitchLabelText = i18nTranslator.getTranslatedString("GUI.OPTIONS.TWITCH.CONNECTED_LABEL").toString() + " (" + accountInfo.getLogin() + ")";
				twitchLabel = new Label(twitchLabelText, uiSkin.get("white_text", Label.LabelStyle.class));
			}
			versionTable.add(twitchLabel).colspan(3).left().pad(5).row();
		}

	}

	private boolean twitchEnabled() {
		return Boolean.parseBoolean(userPreferences.getPreference(UserPreferences.PreferenceKey.TWITCH_INTEGRATION_ENABLED));
	}

	@Override
	public void rebuildUI() {
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

		private final MessageDispatcher messageDispatcher;

		private MainMenuInputHandler(MessageDispatcher messageDispatcher) {
			this.messageDispatcher = messageDispatcher;
		}

		@Override
		public boolean keyDown(int keycode) {
			return false;
		}

		@Override
		public boolean keyUp(int keycode) {
			if (keycode == Input.Keys.D && Gdx.input.isKeyPressed(Input.Keys.CONTROL_LEFT) && Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)) {
				GlobalSettings.DEV_MODE = !GlobalSettings.DEV_MODE;
				messageDispatcher.dispatchMessage(MessageType.DEV_MODE_CHANGED);
				return true;
			}
			if (keycode == Input.Keys.ESCAPE) {
				messageDispatcher.dispatchMessage(MessageType.MENU_ESCAPED);
			}
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
