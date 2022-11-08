package technology.rocketjump.saul.screens.menus.options;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.audio.model.SoundAsset;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.misc.twitch.TwitchDataStore;
import technology.rocketjump.saul.misc.twitch.model.TwitchAccountInfo;
import technology.rocketjump.saul.persistence.UserPreferences;
import technology.rocketjump.saul.rendering.utils.HexColors;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.MenuButtonFactory;
import technology.rocketjump.saul.ui.widgets.WidgetFactory;

import static technology.rocketjump.saul.messaging.MessageType.*;
import static technology.rocketjump.saul.persistence.UserPreferences.PreferenceKey.*;

@Singleton
public class TwitchOptionsTab implements OptionsTab, Telegraph, DisplaysText {

	private static final String INTEGRATION_URL = "https://id.twitch.tv/oauth2/authorize?client_id=6gk8asspwcrt787lxge71kc418a3ng&redirect_uri=http://kingunderthemounta.in/twitch/&response_type=code&scope=channel:read:subscriptions&force_verify=true";

	private final MessageDispatcher messageDispatcher;
	private final I18nTranslator i18nTranslator;
	private final UserPreferences userPreferences;
	private final Skin skin;
	private final SoundAsset clickSoundAsset;
	private final TwitchDataStore twitchDataStore;
	private final WidgetFactory widgetFactory;
	private final MenuButtonFactory menuButtonFactory;


	private CheckBox viewersAsSettersCheckbox;
	private CheckBox prioritiseSubsCheckbox;
	private Container<TextButton> disconnectAccountButton;
	private TwitchAccountInfo accountInfo;

	private boolean twitchEnabled;
	private CheckBox twitchEnabledCheckbox;

	private Container<TextButton> linkAccountButton;
	private Container<TextButton> codeSubmitButton;
	private Label loginLabel;
	private Label authCodeFailureLabel;
	private Label codeLabel;
	private TextField codeInput;
	private Table unauthenticatedTable;
	private Table authenticatedTable;

	private boolean authCodeFailure = false;

	@Inject
	public TwitchOptionsTab(GuiSkinRepository guiSkinRepository, I18nTranslator i18nTranslator, SoundAssetDictionary soundAssetDictionary,
							UserPreferences userPreferences, MessageDispatcher messageDispatcher,
							TwitchDataStore twitchDataStore, WidgetFactory widgetFactory, MenuButtonFactory menuButtonFactory) {

		this.messageDispatcher = messageDispatcher;
		this.i18nTranslator = i18nTranslator;
		this.userPreferences = userPreferences;
		this.skin = guiSkinRepository.getMenuSkin();
		this.clickSoundAsset = soundAssetDictionary.getByName("MenuClick");
		this.twitchDataStore = twitchDataStore;
		this.widgetFactory = widgetFactory;
		this.menuButtonFactory = menuButtonFactory;

		messageDispatcher.addListener(this, MessageType.TWITCH_AUTH_CODE_FAILURE);
		messageDispatcher.addListener(this, MessageType.TWITCH_ACCOUNT_INFO_UPDATED);
	}

	@Override
	public void populate(Table menuTable) {
		authenticatedTable.clear();
		unauthenticatedTable.clear();

		Stack loginStack = new Stack();
		loginStack.add(unauthenticatedTable);
		loginStack.add(authenticatedTable);

		menuTable.add(twitchEnabledCheckbox).spaceBottom(50f).row();
		menuTable.add(loginStack).row();
		authCodeFailureLabel.setWrap(true);

		linkAccountButton.fillX();

		authenticatedTable.add(loginLabel).spaceBottom(50f).row();
		authenticatedTable.add(disconnectAccountButton).spaceBottom(50f).row();
		authenticatedTable.add(viewersAsSettersCheckbox).spaceBottom(50f).row();
		authenticatedTable.add(prioritiseSubsCheckbox).spaceBottom(50f).row();

		unauthenticatedTable.add(linkAccountButton).padBottom(30f).growX().row();
		unauthenticatedTable.add(codeLabel).spaceBottom(30f).row();
		unauthenticatedTable.add(codeInput).spaceBottom(30f).growX().row();
		unauthenticatedTable.add(codeSubmitButton).spaceBottom(50f).row();
		unauthenticatedTable.add(authCodeFailureLabel).growX().row();
	}

	private void reset() {
		unauthenticatedTable.setVisible(false);
		authenticatedTable.setVisible(false);
		authCodeFailureLabel.setVisible(false);

		if (twitchEnabled) {
			if (accountInfo != null) {
				loginLabel.setText(accountInfo.getLogin());
				authenticatedTable.setVisible(true);
			} else {
				unauthenticatedTable.setVisible(true);

				if (authCodeFailure) {
					authCodeFailureLabel.setVisible(true);
				}
			}
		}
	}

	@Override
	public OptionsTabName getTabName() {
		return OptionsTabName.TWITCH;
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.TWITCH_AUTH_CODE_FAILURE: {
				this.authCodeFailure = true;
				reset();
				return true;
			}
			case TWITCH_ACCOUNT_INFO_UPDATED: {
				this.accountInfo = (TwitchAccountInfo) msg.extraInfo;
				reset();
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	@Override
	public void rebuildUI() {
		this.unauthenticatedTable = new Table();
		this.authenticatedTable = new Table();

		twitchEnabledCheckbox = widgetFactory.createLeftLabelledCheckboxNoBackground("GUI.OPTIONS.TWITCH.ENABLED", skin, 428f);
		twitchEnabledCheckbox.setProgrammaticChangeEvents(false);
		twitchEnabled = Boolean.parseBoolean(userPreferences.getPreference(TWITCH_INTEGRATION_ENABLED, "false"));
		twitchEnabledCheckbox.setChecked(twitchEnabled);
		twitchEnabledCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				twitchEnabled = twitchEnabledCheckbox.isChecked();
				userPreferences.setPreference(TWITCH_INTEGRATION_ENABLED, String.valueOf(twitchEnabledCheckbox.isChecked()));
				messageDispatcher.dispatchMessage(PREFERENCE_CHANGED, TWITCH_INTEGRATION_ENABLED);
				reset();
			}
			return true;
		});

		this.accountInfo = twitchDataStore.getAccountInfo();

		loginLabel = new Label("", skin, "options_menu_label");
		linkAccountButton = menuButtonFactory.createButton("GUI.OPTIONS.TWITCH.LINK_ACCOUNT_BUTTON", skin, MenuButtonFactory.ButtonStyle.BTN_OPTIONS_SECONDARY)
				.withAction(() -> {
					Gdx.net.openURI(INTEGRATION_URL);
				})
				.build();

		disconnectAccountButton = menuButtonFactory.createButton("GUI.OPTIONS.TWITCH.DISCONNECT", skin, MenuButtonFactory.ButtonStyle.BTN_OPTIONS_SECONDARY)
				.withAction(() -> {
					twitchDataStore.setCurrentToken(null);
					twitchDataStore.setAccountInfo(null);
				})
				.build();



		codeLabel = new Label(i18nTranslator.getTranslatedString("GUI.OPTIONS.TWITCH.CODE_LABEL").toString(), skin, "options_menu_label");
		codeInput = new TextField("", skin);
		codeInput.addListener(new ChangeCursorOnHover(GameCursor.I_BEAM, messageDispatcher));

		codeSubmitButton = menuButtonFactory.createButton("GUI.OPTIONS.TWITCH.SUBMIT_BUTTON", skin, MenuButtonFactory.ButtonStyle.BTN_OPTIONS_SECONDARY)
				.withAction(() -> {
					String code = codeInput.getText();
					if (!code.isEmpty()) {
						messageDispatcher.dispatchMessage(TWITCH_AUTH_CODE_SUPPLIED, code);
						reset();
					}
				})
				.build();


		authCodeFailureLabel = new Label(i18nTranslator.getTranslatedString("GUI.OPTIONS.TWITCH.GENERAL_ERROR").toString(), skin, "options_menu_label");
		authCodeFailureLabel.setColor(HexColors.NEGATIVE_COLOR);

		viewersAsSettersCheckbox = widgetFactory.createLeftLabelledCheckboxNoBackground("GUI.OPTIONS.TWITCH.VIEWERS_AS_SETTLERS", skin, 428f);
		viewersAsSettersCheckbox.setProgrammaticChangeEvents(false);
		viewersAsSettersCheckbox.setChecked(Boolean.parseBoolean(userPreferences.getPreference(TWITCH_VIEWERS_AS_SETTLER_NAMES, "false")));
		viewersAsSettersCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				userPreferences.setPreference(TWITCH_VIEWERS_AS_SETTLER_NAMES, String.valueOf(viewersAsSettersCheckbox.isChecked()));
			}
			return true;
		});

		prioritiseSubsCheckbox = widgetFactory.createLeftLabelledCheckboxNoBackground("GUI.OPTIONS.TWITCH.PRIORITISE_SUBSCRIBERS", skin, 428f);
		prioritiseSubsCheckbox.setProgrammaticChangeEvents(false);
		prioritiseSubsCheckbox.setChecked(Boolean.parseBoolean(userPreferences.getPreference(TWITCH_PRIORITISE_SUBSCRIBERS, "false")));
		prioritiseSubsCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				userPreferences.setPreference(TWITCH_PRIORITISE_SUBSCRIBERS, String.valueOf(prioritiseSubsCheckbox.isChecked()));
			}
			return true;
		});

		reset();
	}
}
