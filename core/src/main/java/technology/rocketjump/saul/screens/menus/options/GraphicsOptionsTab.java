package technology.rocketjump.saul.screens.menus.options;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.audio.model.SoundAsset;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.RequestSoundMessage;
import technology.rocketjump.saul.persistence.UserPreferences;
import technology.rocketjump.saul.rendering.camera.DisplaySettings;
import technology.rocketjump.saul.screens.menus.Resolution;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.i18n.I18nUpdatable;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.I18nLabel;
import technology.rocketjump.saul.ui.widgets.I18nWidgetFactory;

import java.util.LinkedHashMap;
import java.util.Map;

import static technology.rocketjump.saul.persistence.UserPreferences.FullscreenMode.BORDERLESS_FULLSCREEN;
import static technology.rocketjump.saul.persistence.UserPreferences.FullscreenMode.WINDOWED;
import static technology.rocketjump.saul.persistence.UserPreferences.PreferenceKey.DISPLAY_FULLSCREEN;
import static technology.rocketjump.saul.persistence.UserPreferences.PreferenceKey.FULLSCREEN_MODE;

@Singleton
public class GraphicsOptionsTab implements OptionsTab, I18nUpdatable {

	private final I18nTranslator i18nTranslator;
	private final UserPreferences userPreferences;

	private final I18nLabel graphicsTitle;
	private final I18nLabel resolutionLabel;
	private final SelectBox<Resolution> resolutionSelect;
	private final I18nLabel fullscreenLabel;
	private final SelectBox<String> fullscreenSelect;
	private final EventListener fullscreenSelectListener;
	private boolean restartRequiredNotified;

	private final Map<String, UserPreferences.FullscreenMode> translatedFullscreenModes = new LinkedHashMap<>();

	@Inject
	public GraphicsOptionsTab(UserPreferences userPreferences, GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
							  I18nWidgetFactory i18NWidgetFactory, SoundAssetDictionary soundAssetDictionary, I18nTranslator i18nTranslator) {
		this.i18nTranslator = i18nTranslator;
		this.userPreferences = userPreferences;
		Skin uiSkin = guiSkinRepository.getDefault();
		graphicsTitle = i18NWidgetFactory.createLabel("GUI.OPTIONS.GRAPHICS.TITLE");

		final SoundAsset clickSoundAsset = soundAssetDictionary.getByName("MenuClick");

		fullscreenLabel = i18NWidgetFactory.createLabel("GUI.GRAPHICS.FULLSCREEN");

		fullscreenSelect = new SelectBox<>(uiSkin);
		fullscreenSelectListener = new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(clickSoundAsset));
				UserPreferences.FullscreenMode selectedMode = translatedFullscreenModes.get(fullscreenSelect.getSelected());
				userPreferences.setPreference(FULLSCREEN_MODE, selectedMode.name());
				if (!restartRequiredNotified) {
					messageDispatcher.dispatchMessage(MessageType.NOTIFY_RESTART_REQUIRED);
					restartRequiredNotified = true;
				}
			}
		};
		refreshFullscreenModeOptions();

		resolutionLabel = i18NWidgetFactory.createLabel("GUI.GRAPHICS.RESOLUTION");
		resolutionSelect = new SelectBox<>(uiSkin);
		Array<Resolution> resolutionList = Resolution.defaultResolutions;
		if (!resolutionList.contains(DisplaySettings.currentResolution, false)) {
			resolutionList.insert(0, DisplaySettings.currentResolution);
		}
		resolutionSelect.setItems(resolutionList);
		resolutionSelect.setSelected(DisplaySettings.currentResolution);
		resolutionSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(clickSoundAsset));
				Resolution selectedResolution = resolutionSelect.getSelected();
				userPreferences.setPreference(UserPreferences.PreferenceKey.DISPLAY_RESOLUTION, selectedResolution.toString());
				if (!restartRequiredNotified) {
					messageDispatcher.dispatchMessage(MessageType.NOTIFY_RESTART_REQUIRED);
					restartRequiredNotified = true;
				}
			}
		});

	}

	@Override
	public void populate(Table menuTable) {
		menuTable.add(graphicsTitle).width(250).left().pad(10);
		menuTable.add(new Container<>()).colspan(2).row();

		menuTable.add(fullscreenLabel).pad(10).right(); // pad out 1 cell
		menuTable.add(fullscreenSelect).colspan(2).left().pad(10).row();

		menuTable.add(resolutionLabel).pad(10).right();
		menuTable.add(resolutionSelect).pad(10).left();
		menuTable.add(new Container<>()).row(); // pad out 1 cell
	}

	@Override
	public OptionsTabName getTabName() {
		return OptionsTabName.GRAPHICS;
	}

	private void refreshFullscreenModeOptions() {
		fullscreenSelect.removeListener(fullscreenSelectListener);

		translatedFullscreenModes.clear();

		Array<String> fullscreenModeList = new Array<>();
		UserPreferences.FullscreenMode currentlySelected = getFullscreenMode(userPreferences);
		String selectedValue = "";
		for (UserPreferences.FullscreenMode fullscreenMode : UserPreferences.FullscreenMode.values()) {
			String translatedMode = i18nTranslator.getTranslatedString(fullscreenMode.i18nKey).toString();
			translatedFullscreenModes.put(translatedMode, fullscreenMode);
			fullscreenModeList.add(translatedMode);
			if (fullscreenMode.equals(currentlySelected)) {
				selectedValue = translatedMode;
			}
		}
		fullscreenSelect.setItems(fullscreenModeList);
		fullscreenSelect.setSelected(selectedValue);

		fullscreenSelect.addListener(fullscreenSelectListener);
	}

	public static UserPreferences.FullscreenMode getFullscreenMode(UserPreferences userPreferences) {
		boolean legacyFullscreen = Boolean.parseBoolean(userPreferences.getPreference(DISPLAY_FULLSCREEN, "true"));
		return UserPreferences.FullscreenMode.valueOf(
				userPreferences.getPreference(FULLSCREEN_MODE, legacyFullscreen ? BORDERLESS_FULLSCREEN.name() : WINDOWED.name())
		);
	}

	@Override
	public void onLanguageUpdated() {
		refreshFullscreenModeOptions();
	}
}
