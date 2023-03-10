package technology.rocketjump.saul.screens.menus.options;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
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
import technology.rocketjump.saul.rendering.camera.GlobalSettings;
import technology.rocketjump.saul.screens.menus.Resolution;
import technology.rocketjump.saul.ui.ViewportUtils;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.ClickableSoundsListener;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.WidgetFactory;

import java.util.LinkedHashMap;
import java.util.Map;

import static technology.rocketjump.saul.persistence.UserPreferences.FullscreenMode.BORDERLESS_FULLSCREEN;
import static technology.rocketjump.saul.persistence.UserPreferences.FullscreenMode.WINDOWED;
import static technology.rocketjump.saul.persistence.UserPreferences.PreferenceKey.*;

@Singleton
public class GraphicsOptionsTab implements OptionsTab, DisplaysText {

	private final MessageDispatcher messageDispatcher;
	private final SoundAssetDictionary soundAssetDictionary;
	private final I18nTranslator i18nTranslator;
	private final UserPreferences userPreferences;
	private final Skin skin;
	private final SoundAsset clickSoundAsset;
	private final SoundAsset sliderSoundAsset;
	private final WidgetFactory widgetFactory;
	private final TooltipFactory tooltipFactory;

	private SelectBox<Resolution> resolutionSelect;
	private SelectBox<String> fullscreenSelect;
	private Label uiScaleLabel;
	private Slider uiScaleSlider;
	private CheckBox weatherEffectsCheckbox;
	private EventListener fullscreenSelectListener;
	private boolean restartRequiredNotified;


	private final Map<String, UserPreferences.FullscreenMode> translatedFullscreenModes = new LinkedHashMap<>();

	@Inject
	public GraphicsOptionsTab(UserPreferences userPreferences, GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
							  SoundAssetDictionary soundAssetDictionary, I18nTranslator i18nTranslator, WidgetFactory widgetFactory, TooltipFactory tooltipFactory) {
		this.messageDispatcher = messageDispatcher;
		this.soundAssetDictionary = soundAssetDictionary;
		this.i18nTranslator = i18nTranslator;
		this.userPreferences = userPreferences;
		this.skin = guiSkinRepository.getMenuSkin();
		this.clickSoundAsset = soundAssetDictionary.getByName("MenuClick");
		this.sliderSoundAsset = soundAssetDictionary.getByName("Slider");
		this.widgetFactory = widgetFactory;
		this.tooltipFactory = tooltipFactory;

		rebuildUI();
	}

	@Override
	public void populate(Table menuTable) {
		menuTable.add(fullscreenSelect).padBottom(48f).row();
		menuTable.add(resolutionSelect).padBottom(48f).row();
		menuTable.add(uiScaleLabel).spaceBottom(30f).row();
		menuTable.add(uiScaleSlider).spaceBottom(48f).growX().row();
		menuTable.add(weatherEffectsCheckbox).padBottom(48f).row();
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
		fullscreenSelect.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
		fullscreenSelect.addListener(new ChangeCursorOnHover(fullscreenSelect, GameCursor.SELECT, messageDispatcher));
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
		resolutionSelect.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
		resolutionSelect.addListener(new ChangeCursorOnHover(resolutionSelect, GameCursor.SELECT, messageDispatcher));

		uiScaleLabel = new Label(i18nTranslator.translate("GUI.UI_SCALE"), skin, "options_menu_label");
		uiScaleSlider = new Slider(ViewportUtils.MIN_VIEWPORT_SCALE, ViewportUtils.MAX_VIEWPORT_SCALE, 0.01f, false, skin); //Uses Viewport domain
		uiScaleSlider.setValue(Float.parseFloat(userPreferences.getPreference(UI_SCALE, "1.0")));

		uiScaleSlider.setProgrammaticChangeEvents(false);

		uiScaleSlider.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				float viewportScaleValue = uiScaleSlider.getValue();
				userPreferences.setPreference(UI_SCALE, String.valueOf(viewportScaleValue));

				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(sliderSoundAsset));
				messageDispatcher.dispatchMessage(MessageType.GUI_SCALE_CHANGED);
			}
			return true;
		});
		uiScaleSlider.addListener(new ChangeCursorOnHover(uiScaleSlider, GameCursor.REORDER_HORIZONTAL, messageDispatcher));

		weatherEffectsCheckbox = widgetFactory.createLeftLabelledCheckboxNoBackground("GUI.OPTIONS.GRAPHICS.WEATHER_EFFECTS", skin, 428f);
		GlobalSettings.WEATHER_EFFECTS = Boolean.parseBoolean(userPreferences.getPreference(UserPreferences.PreferenceKey.WEATHER_EFFECTS, "true"));
		weatherEffectsCheckbox.setChecked(GlobalSettings.WEATHER_EFFECTS);
		weatherEffectsCheckbox.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(clickSoundAsset));
				GlobalSettings.WEATHER_EFFECTS = weatherEffectsCheckbox.isChecked();
				userPreferences.setPreference(UserPreferences.PreferenceKey.WEATHER_EFFECTS, String.valueOf(GlobalSettings.WEATHER_EFFECTS));
			}
			return true;
		});

		tooltipFactory.simpleTooltip(weatherEffectsCheckbox, "GUI.OPTIONS.GRAPHICS.WEATHER_EFFECTS_TOOLTIP", TooltipLocationHint.ABOVE);
	}
}
