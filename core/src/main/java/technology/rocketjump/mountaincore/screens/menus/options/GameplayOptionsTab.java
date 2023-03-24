package technology.rocketjump.mountaincore.screens.menus.options;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.audio.model.SoundAsset;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.input.KeyBindingUIWidget;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.RequestSoundMessage;
import technology.rocketjump.mountaincore.persistence.UserPreferences;
import technology.rocketjump.mountaincore.rendering.camera.GlobalSettings;
import technology.rocketjump.mountaincore.ui.i18n.DisplaysText;
import technology.rocketjump.mountaincore.ui.i18n.I18nText;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;
import technology.rocketjump.mountaincore.ui.skins.GuiSkinRepository;
import technology.rocketjump.mountaincore.ui.widgets.BlurredBackgroundDialog;
import technology.rocketjump.mountaincore.ui.widgets.EnhancedScrollPane;
import technology.rocketjump.mountaincore.ui.widgets.MenuButtonFactory;
import technology.rocketjump.mountaincore.ui.widgets.WidgetFactory;

@Singleton
public class GameplayOptionsTab implements OptionsTab, DisplaysText {

	private final Skin skin;
	private final SoundAsset clickSoundAsset;
	private final UserPreferences userPreferences;
	private final MessageDispatcher messageDispatcher;
	private final I18nTranslator i18nTranslator;
	private final WidgetFactory widgetFactory;
	private final MenuButtonFactory menuButtonFactory;
	private final SoundAssetDictionary soundAssetDictionary;

	private Container<TextButton> keyBindingsButton;
	private CheckBox edgeScrollingCheckbox;
	private CheckBox zoomToCursorCheckbox;
	private CheckBox treeTransparencyCheckbox;

	@Inject
	public GameplayOptionsTab(UserPreferences userPreferences, MessageDispatcher messageDispatcher, GuiSkinRepository guiSkinRepository,
	                          SoundAssetDictionary soundAssetDictionary, I18nTranslator i18nTranslator, WidgetFactory widgetFactory,
	                          MenuButtonFactory menuButtonFactory, SoundAssetDictionary soundAssetDictionary1) {
		this.userPreferences = userPreferences;
		this.messageDispatcher = messageDispatcher;
		this.clickSoundAsset = soundAssetDictionary.getByName("MenuClick");
		this.i18nTranslator = i18nTranslator;
		this.skin = guiSkinRepository.getMenuSkin();
		this.widgetFactory = widgetFactory;
		this.menuButtonFactory = menuButtonFactory;
		this.soundAssetDictionary = soundAssetDictionary1;
	}

	@Override
	public void populate(Table menuTable) {
		// GAMEPLAY
		menuTable.add(keyBindingsButton).spaceBottom(50f).row();
		menuTable.add(edgeScrollingCheckbox).spaceBottom(50f).row();
		menuTable.add(zoomToCursorCheckbox).spaceBottom(50f).row();
		menuTable.add(treeTransparencyCheckbox).spaceBottom(50f).row();
	}

	@Override
	public OptionsTabName getTabName() {
		return OptionsTabName.GAMEPLAY;
	}

	@Override
	public void rebuildUI() {

		keyBindingsButton = menuButtonFactory.createButton("GUI.OPTIONS.KEY_BINDINGS", skin, MenuButtonFactory.ButtonStyle.BTN_OPTIONS_SECONDARY)
				.withAction(() -> {

					BlurredBackgroundDialog dialog = new BlurredBackgroundDialog(I18nText.BLANK, skin, messageDispatcher, soundAssetDictionary);
					Label titleRibbon = new Label(i18nTranslator.translate("GUI.OPTIONS.KEY_BINDINGS"), skin, "key_bindings_title_ribbon");
					titleRibbon.setAlignment(Align.center);
					Label gameplayLabel = new Label(i18nTranslator.translate(OptionsTabName.GAMEPLAY.getI18nKey()), skin, "secondary_banner_title");
					gameplayLabel.setAlignment(Align.center);
					KeyBindingUIWidget keyBindingUIWidget = new KeyBindingUIWidget(skin, userPreferences, i18nTranslator, messageDispatcher, soundAssetDictionary);
					Container<TextButton> resetBindingsButton = menuButtonFactory.createButton("GUI.OPTIONS.RESET", skin, MenuButtonFactory.ButtonStyle.BTN_OPTIONS_SECONDARY)
							.withAction(() -> keyBindingUIWidget.resetToDefaultSettings())
							.build();

					ScrollPane scrollPane = new EnhancedScrollPane(keyBindingUIWidget, skin);
					scrollPane.setForceScroll(false, true);
					scrollPane.setFadeScrollBars(false);
					scrollPane.setScrollbarsVisible(true);
					scrollPane.setScrollBarPositions(true, true);

					Table mainTable = new Table();
					mainTable.setBackground(skin.getDrawable("asset_square_bg"));

					mainTable.defaults().padLeft(120f).padRight(120f);
					mainTable.add(titleRibbon).spaceTop(28f).spaceBottom(50f).row();
					mainTable.add(gameplayLabel).align(Align.left).row();
					mainTable.add(scrollPane).growX().height(1256f).padBottom(50f).row();
					mainTable.add(resetBindingsButton).padBottom(100f).row();

					dialog.getContentTable().add(mainTable);

					messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, dialog);
				})
				.build();

		edgeScrollingCheckbox = widgetFactory.createLeftLabelledCheckboxNoBackground("GUI.OPTIONS.GAMEPLAY.USE_EDGE_SCROLLING", skin, 428f);
		GlobalSettings.USE_EDGE_SCROLLING = Boolean.parseBoolean(userPreferences.getPreference(UserPreferences.PreferenceKey.EDGE_SCROLLING));;
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
		GlobalSettings.ZOOM_TO_CURSOR = Boolean.parseBoolean(userPreferences.getPreference(UserPreferences.PreferenceKey.ZOOM_TO_CURSOR));;
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
		GlobalSettings.TREE_TRANSPARENCY_ENABLED = Boolean.parseBoolean(userPreferences.getPreference(UserPreferences.PreferenceKey.TREE_TRANSPARENCY));;
		treeTransparencyCheckbox.setChecked(GlobalSettings.TREE_TRANSPARENCY_ENABLED);
		treeTransparencyCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(clickSoundAsset));
				GlobalSettings.TREE_TRANSPARENCY_ENABLED = treeTransparencyCheckbox.isChecked();
				userPreferences.setPreference(UserPreferences.PreferenceKey.TREE_TRANSPARENCY, String.valueOf(GlobalSettings.TREE_TRANSPARENCY_ENABLED));
			}
			return true;
		});

	}
}
