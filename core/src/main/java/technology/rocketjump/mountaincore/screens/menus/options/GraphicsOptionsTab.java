package technology.rocketjump.mountaincore.screens.menus.options;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Graphics;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.EventListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.SystemUtils;
import technology.rocketjump.mountaincore.audio.model.SoundAsset;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.RequestSoundMessage;
import technology.rocketjump.mountaincore.persistence.UserPreferences;
import technology.rocketjump.mountaincore.rendering.camera.DisplaySettings;
import technology.rocketjump.mountaincore.rendering.camera.GlobalSettings;
import technology.rocketjump.mountaincore.screens.menus.Resolution;
import technology.rocketjump.mountaincore.ui.ViewportUtils;
import technology.rocketjump.mountaincore.ui.cursor.GameCursor;
import technology.rocketjump.mountaincore.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.mountaincore.ui.eventlistener.ClickableSoundsListener;
import technology.rocketjump.mountaincore.ui.eventlistener.TooltipFactory;
import technology.rocketjump.mountaincore.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;
import technology.rocketjump.mountaincore.ui.skins.GuiSkinRepository;
import technology.rocketjump.mountaincore.ui.widgets.WidgetFactory;

import java.util.LinkedHashMap;
import java.util.Map;

@Singleton
public class GraphicsOptionsTab implements OptionsTab {

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
	private Resolution userSelectedResolution;


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
	}

	@Override
	public void populate(Table menuTable) {
		rebuildUI();
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
			if (SystemUtils.IS_OS_MAC && fullscreenMode == UserPreferences.FullscreenMode.EXCLUSIVE_FULLSCREEN) {
				continue;
			}
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

	private void toggleResolutionSelect(UserPreferences.FullscreenMode currentlySelected) {

		if (currentlySelected == UserPreferences.FullscreenMode.EXCLUSIVE_FULLSCREEN) {
			disableResolutionSelect();
			Graphics.DisplayMode monitorDisplayMode = Gdx.graphics.getDisplayMode();
			resolutionSelect.setSelected(new Resolution(monitorDisplayMode.width, monitorDisplayMode.height));
		} else {
			enableResolutionSelect();
			resolutionSelect.setSelected(userSelectedResolution);
		}
	}

	private void enableResolutionSelect() {
		if (resolutionSelect != null) {
			resolutionSelect.setDisabled(false);
			resolutionSelect.setTouchable(Touchable.enabled);
			resolutionSelect.getColor().a = 1.0f;
		}
	}

	private void disableResolutionSelect() {
		if (resolutionSelect != null) {
			resolutionSelect.setDisabled(true);
			resolutionSelect.setTouchable(Touchable.disabled);
			resolutionSelect.getColor().a = 0.5f;
		}
	}

	public static UserPreferences.FullscreenMode getFullscreenMode(UserPreferences userPreferences) {
		UserPreferences.FullscreenMode fullscreenMode = UserPreferences.FullscreenMode.valueOf(userPreferences.getPreference(UserPreferences.PreferenceKey.FULLSCREEN_MODE));
		if (SystemUtils.IS_OS_MAC && fullscreenMode == UserPreferences.FullscreenMode.EXCLUSIVE_FULLSCREEN) {
			fullscreenMode = UserPreferences.FullscreenMode.WINDOWED;
		}
		return fullscreenMode;
	}

	public void rebuildUI() {
		fullscreenSelect = new SelectBox<>(skin);
		fullscreenSelect.setAlignment(Align.center);
		fullscreenSelect.getScrollPane().getList().setAlignment(Align.center);
		fullscreenSelectListener = new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(clickSoundAsset));
				UserPreferences.FullscreenMode selectedMode = translatedFullscreenModes.get(fullscreenSelect.getSelected());
				userPreferences.setPreference(UserPreferences.PreferenceKey.FULLSCREEN_MODE, selectedMode.name());
				toggleResolutionSelect(selectedMode);
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
		userSelectedResolution = DisplaySettings.currentResolution;
		resolutionSelect.setSelected(DisplaySettings.currentResolution);
		resolutionSelect.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(clickSoundAsset));
				Resolution selectedResolution = resolutionSelect.getSelected();
				DisplaySettings.currentResolution = selectedResolution;
				userSelectedResolution = selectedResolution;
				userPreferences.setPreference(UserPreferences.PreferenceKey.DISPLAY_RESOLUTION, selectedResolution.toString());
				if (!restartRequiredNotified) {
					//TODO: should be able to update this window without restart
					//https://libgdx.com/wiki/graphics/querying-and-configuring-graphics
					messageDispatcher.dispatchMessage(MessageType.NOTIFY_RESTART_REQUIRED);
					restartRequiredNotified = true;
				}
			}
		});
		resolutionSelect.getSelection().setProgrammaticChangeEvents(false);
		resolutionSelect.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
		resolutionSelect.addListener(new ChangeCursorOnHover(resolutionSelect, GameCursor.SELECT, messageDispatcher));
		toggleResolutionSelect(translatedFullscreenModes.get(fullscreenSelect.getSelected()));

		uiScaleLabel = new Label(i18nTranslator.translate("GUI.UI_SCALE"), skin, "options_menu_label");
		uiScaleSlider = new Slider(ViewportUtils.MIN_VIEWPORT_SCALE, ViewportUtils.MAX_VIEWPORT_SCALE, 0.01f, false, skin); //Uses Viewport domain
		uiScaleSlider.setValue(Float.parseFloat(userPreferences.getPreference(UserPreferences.PreferenceKey.UI_SCALE)));

		uiScaleSlider.setProgrammaticChangeEvents(false);

		uiScaleSlider.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				float viewportScaleValue = uiScaleSlider.getValue();
				userPreferences.setPreference(UserPreferences.PreferenceKey.UI_SCALE, String.valueOf(viewportScaleValue));

				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(sliderSoundAsset));
				messageDispatcher.dispatchMessage(MessageType.GUI_SCALE_CHANGED);
			}
			return true;
		});
		uiScaleSlider.addListener(new ChangeCursorOnHover(uiScaleSlider, GameCursor.REORDER_HORIZONTAL, messageDispatcher));

		weatherEffectsCheckbox = widgetFactory.createLeftLabelledCheckboxNoBackground("GUI.OPTIONS.GRAPHICS.WEATHER_EFFECTS", skin, 428f);
		GlobalSettings.WEATHER_EFFECTS = Boolean.parseBoolean(userPreferences.getPreference(UserPreferences.PreferenceKey.WEATHER_EFFECTS));
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
