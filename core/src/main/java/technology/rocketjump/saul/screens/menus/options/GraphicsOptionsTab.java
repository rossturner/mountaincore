package technology.rocketjump.saul.screens.menus.options;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.ui.SelectBox;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
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
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;

import java.util.LinkedHashMap;
import java.util.Map;

import static technology.rocketjump.saul.persistence.UserPreferences.FullscreenMode.BORDERLESS_FULLSCREEN;
import static technology.rocketjump.saul.persistence.UserPreferences.FullscreenMode.WINDOWED;
import static technology.rocketjump.saul.persistence.UserPreferences.PreferenceKey.DISPLAY_FULLSCREEN;
import static technology.rocketjump.saul.persistence.UserPreferences.PreferenceKey.FULLSCREEN_MODE;

@Singleton
public class GraphicsOptionsTab implements OptionsTab, DisplaysText {

	private final MessageDispatcher messageDispatcher;
	private final I18nTranslator i18nTranslator;
	private final UserPreferences userPreferences;
	private final Skin skin;
	private final SoundAsset clickSoundAsset;

	private SelectBox<Resolution> resolutionSelect;
	private SelectBox<String> fullscreenSelect;
	private EventListener fullscreenSelectListener;
	private boolean restartRequiredNotified;

	private final Map<String, UserPreferences.FullscreenMode> translatedFullscreenModes = new LinkedHashMap<>();

	@Inject
	public GraphicsOptionsTab(UserPreferences userPreferences, GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
							  SoundAssetDictionary soundAssetDictionary, I18nTranslator i18nTranslator) {
		this.messageDispatcher = messageDispatcher;
		this.i18nTranslator = i18nTranslator;
		this.userPreferences = userPreferences;
		this.skin = guiSkinRepository.getMenuSkin();
		this.clickSoundAsset = soundAssetDictionary.getByName("MenuClick");

		rebuildUI();
	}

	@Override
	public void populate(Table menuTable) {
		menuTable.add(fullscreenSelect).padBottom(48f).row();
		menuTable.add(resolutionSelect).padBottom(48f).row();
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
	public void rebuildUI() {
		fullscreenSelect = new SelectBox<>(skin);
		fullscreenSelect.setAlignment(Align.center);
		fullscreenSelect.getScrollPane().getList().setAlignment(Align.center);
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

		resolutionSelect = new SelectBox<>(skin);
		resolutionSelect.setAlignment(Align.center);
		resolutionSelect.getScrollPane().getList().setAlignment(Align.center);
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
}
