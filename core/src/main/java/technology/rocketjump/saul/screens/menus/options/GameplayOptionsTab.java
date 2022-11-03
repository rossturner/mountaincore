package technology.rocketjump.saul.screens.menus.options;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.audio.model.SoundAsset;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.input.CommandName;
import technology.rocketjump.saul.input.KeyBindingInputProcessor;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.RequestSoundMessage;
import technology.rocketjump.saul.persistence.UserPreferences;
import technology.rocketjump.saul.rendering.camera.GlobalSettings;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.BlurredBackgroundDialog;
import technology.rocketjump.saul.ui.widgets.MenuButtonFactory;
import technology.rocketjump.saul.ui.widgets.WidgetFactory;

import java.util.Set;

import static technology.rocketjump.saul.persistence.UserPreferences.PreferenceKey.ALLOW_HINTS;

@Singleton
public class GameplayOptionsTab implements OptionsTab, Telegraph, DisplaysText {

	private final Skin skin;
	private final SoundAsset clickSoundAsset;
	private final UserPreferences userPreferences;
	private final MessageDispatcher messageDispatcher;
	private final I18nTranslator i18nTranslator;
	private final WidgetFactory widgetFactory;
	private final MenuButtonFactory menuButtonFactory;

	private Container<TextButton> keyBindingsButton;
	private CheckBox edgeScrollingCheckbox;
	private CheckBox zoomToCursorCheckbox;
	private CheckBox treeTransparencyCheckbox;
	private CheckBox pauseOnNotificationCheckbox;
	private CheckBox enableHintsCheckbox;

	@Inject
	public GameplayOptionsTab(UserPreferences userPreferences, MessageDispatcher messageDispatcher, GuiSkinRepository guiSkinRepository,
	                          SoundAssetDictionary soundAssetDictionary, I18nTranslator i18nTranslator, WidgetFactory widgetFactory,
	                          MenuButtonFactory menuButtonFactory) {
		this.userPreferences = userPreferences;
		this.messageDispatcher = messageDispatcher;
		this.clickSoundAsset = soundAssetDictionary.getByName("MenuClick");
		this.i18nTranslator = i18nTranslator;
		this.skin = guiSkinRepository.getMenuSkin();
		this.widgetFactory = widgetFactory;
		this.menuButtonFactory = menuButtonFactory;
	}

	@Override
	public void populate(Table menuTable) {
		// GAMEPLAY
		menuTable.add(keyBindingsButton).spaceBottom(50f).row();
		menuTable.add(edgeScrollingCheckbox).spaceBottom(50f).row();
		menuTable.add(zoomToCursorCheckbox).spaceBottom(50f).row();
		menuTable.add(treeTransparencyCheckbox).spaceBottom(50f).row();
		menuTable.add(pauseOnNotificationCheckbox).spaceBottom(50f).row();
		menuTable.add(enableHintsCheckbox).spaceBottom(50f).row();
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.PREFERENCE_CHANGED: {
				UserPreferences.PreferenceKey changedKey = (UserPreferences.PreferenceKey) msg.extraInfo;
				if (changedKey.equals(UserPreferences.PreferenceKey.ALLOW_HINTS)) {
					enableHintsCheckbox.setChecked(Boolean.parseBoolean(userPreferences.getPreference(ALLOW_HINTS, "true")));
					return true;
				} else {
					return false;
				}
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	@Override
	public OptionsTabName getTabName() {
		return OptionsTabName.GAMEPLAY;
	}

	@Override
	public void rebuildUI() {

		keyBindingsButton = menuButtonFactory.createButton("GUI.OPTIONS.KEY_BINDINGS", skin, MenuButtonFactory.ButtonStyle.BTN_OPTIONS_SECONDARY)
				.withAction(() -> {

					BlurredBackgroundDialog dialog = new BlurredBackgroundDialog(I18nText.BLANK, skin, messageDispatcher, skin.get("square_dialog", Window.WindowStyle.class));
					Label titleRibbon = new Label(i18nTranslator.getTranslatedString("GUI.OPTIONS.KEY_BINDINGS").toString(), skin, "title_ribbon");
					Label gameplayLabel = new Label(i18nTranslator.getTranslatedString(OptionsTabName.GAMEPLAY.getI18nKey()).toString(), skin, "secondary_banner_title");
					gameplayLabel.setAlignment(Align.center);

					Table assignableButtonsTable = new Table();

					ScrollPane scrollPane = new ScrollPane(assignableButtonsTable, skin);
					scrollPane.setForceScroll(false, true);
					scrollPane.setScrollBarPositions(true, true);
					scrollPane.setScrollBarTouch(false);

					dialog.getContentTable().add(titleRibbon).spaceTop(28f).spaceBottom(50f).row();
					dialog.getContentTable().add(gameplayLabel).align(Align.left).row();
					dialog.getContentTable().add(scrollPane).fillX().row();



					//TODO: loop me and proper name actions
					for (CommandName action : CommandName.values()) {
						Label actionLabel = new Label(action.name(), skin, "options_menu_label");
						TextButton primaryKey = new TextButton(userPreferences.getInputFor(action, true), skin, "btn_key_bindings_key");
						TextButton secondaryKey = new TextButton(userPreferences.getInputFor(action, false), skin, "btn_key_bindings_key");
						primaryKey.addListener(new ClickListener(){
							@Override
							public void clicked(InputEvent event, float x, float y) {
								primaryKey.setText("...");
								InputProcessor currentInputProcessor = Gdx.input.getInputProcessor();
								Gdx.input.setInputProcessor(new KeyBindingInputProcessor(currentInputProcessor,
								keyboardKeys -> {
									primaryKey.setChecked(false);
									userPreferences.assignInput(action, false, keyboardKeys, true);
									String inputDescription = userPreferences.getInputFor(action, true);
									primaryKey.setText(inputDescription);
									//TODO: loop through all buttons to clear existing allocation?
								}, mouseButton -> {
									primaryKey.setChecked(false);
									userPreferences.assignInput(action, true, Set.of(mouseButton), true);
									primaryKey.setText(userPreferences.getInputFor(action, true));
								}));

							}
						});

						secondaryKey.addListener(new ClickListener(){
							@Override
							public void clicked(InputEvent event, float x, float y) {
								secondaryKey.setText("...");
								InputProcessor currentInputProcessor = Gdx.input.getInputProcessor();
								Gdx.input.setInputProcessor(new KeyBindingInputProcessor(currentInputProcessor, keyboardKeys -> {
									secondaryKey.setChecked(false);
									userPreferences.assignInput(action, false, keyboardKeys, false);
									String inputDescription = userPreferences.getInputFor(action, false);
									secondaryKey.setText(inputDescription);
									//TODO: loop through all buttons to clear existing allocation?
								}, mouseButton -> {
									secondaryKey.setChecked(false);
									userPreferences.assignInput(action, true, Set.of(mouseButton), false);
									secondaryKey.setText(userPreferences.getInputFor(action, false));
								}));

							}
						});
						assignableButtonsTable.add(actionLabel).growX();
						assignableButtonsTable.add(primaryKey);
						assignableButtonsTable.add(secondaryKey);
						assignableButtonsTable.row();
					}


					messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, dialog);
				})
				.build();

		edgeScrollingCheckbox = widgetFactory.createLeftLabelledCheckboxNoBackground("GUI.OPTIONS.GAMEPLAY.USE_EDGE_SCROLLING", skin, 428f);
		GlobalSettings.USE_EDGE_SCROLLING = Boolean.parseBoolean(userPreferences.getPreference(UserPreferences.PreferenceKey.EDGE_SCROLLING, "true"));;
		edgeScrollingCheckbox.setChecked(GlobalSettings.USE_EDGE_SCROLLING);
		edgeScrollingCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(clickSoundAsset));
				GlobalSettings.USE_EDGE_SCROLLING = edgeScrollingCheckbox.isChecked();
				userPreferences.setPreference(UserPreferences.PreferenceKey.EDGE_SCROLLING, String.valueOf(GlobalSettings.USE_EDGE_SCROLLING));
			}
			return true;
		});

		zoomToCursorCheckbox = widgetFactory.createLeftLabelledCheckboxNoBackground("GUI.OPTIONS.GAMEPLAY.ZOOM_TO_CURSOR", skin, 428f);
		GlobalSettings.ZOOM_TO_CURSOR = Boolean.parseBoolean(userPreferences.getPreference(UserPreferences.PreferenceKey.ZOOM_TO_CURSOR, "true"));;
		zoomToCursorCheckbox.setChecked(GlobalSettings.ZOOM_TO_CURSOR);
		zoomToCursorCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(clickSoundAsset));
				GlobalSettings.ZOOM_TO_CURSOR = zoomToCursorCheckbox.isChecked();
				userPreferences.setPreference(UserPreferences.PreferenceKey.ZOOM_TO_CURSOR, String.valueOf(GlobalSettings.ZOOM_TO_CURSOR));
			}
			return true;
		});

		treeTransparencyCheckbox = widgetFactory.createLeftLabelledCheckboxNoBackground("GUI.OPTIONS.GAMEPLAY.HIDE_TREES_OBSCURING_SETTLERS", skin, 428f);
		GlobalSettings.TREE_TRANSPARENCY_ENABLED = Boolean.parseBoolean(userPreferences.getPreference(UserPreferences.PreferenceKey.TREE_TRANSPARENCY, "true"));;
		treeTransparencyCheckbox.setChecked(GlobalSettings.TREE_TRANSPARENCY_ENABLED);
		treeTransparencyCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(clickSoundAsset));
				GlobalSettings.TREE_TRANSPARENCY_ENABLED = treeTransparencyCheckbox.isChecked();
				userPreferences.setPreference(UserPreferences.PreferenceKey.TREE_TRANSPARENCY, String.valueOf(GlobalSettings.TREE_TRANSPARENCY_ENABLED));
			}
			return true;
		});

		pauseOnNotificationCheckbox = widgetFactory.createLeftLabelledCheckboxNoBackground("GUI.OPTIONS.GAMEPLAY.PAUSE_ON_NOTIFICATION", skin, 428f);
		GlobalSettings.PAUSE_FOR_NOTIFICATIONS = Boolean.parseBoolean(userPreferences.getPreference(UserPreferences.PreferenceKey.PAUSE_FOR_NOTIFICATIONS, "true"));;
		pauseOnNotificationCheckbox.setChecked(GlobalSettings.PAUSE_FOR_NOTIFICATIONS);
		pauseOnNotificationCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(clickSoundAsset));
				GlobalSettings.PAUSE_FOR_NOTIFICATIONS = pauseOnNotificationCheckbox.isChecked();
				userPreferences.setPreference(UserPreferences.PreferenceKey.PAUSE_FOR_NOTIFICATIONS, String.valueOf(GlobalSettings.PAUSE_FOR_NOTIFICATIONS));
			}
			return true;
		});

		enableHintsCheckbox = widgetFactory.createLeftLabelledCheckboxNoBackground("GUI.OPTIONS.MISC.HINTS_ENABLED", skin, 428f);
		enableHintsCheckbox.setProgrammaticChangeEvents(false); // Used so that message triggered below does not loop endlessly
		enableHintsCheckbox.setChecked(Boolean.parseBoolean(userPreferences.getPreference(ALLOW_HINTS, "true")));
		enableHintsCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(clickSoundAsset));
				userPreferences.setPreference(ALLOW_HINTS, String.valueOf(enableHintsCheckbox.isChecked()));
			}
			return true;
		});

		messageDispatcher.addListener(this, MessageType.PREFERENCE_CHANGED);
	}
}
