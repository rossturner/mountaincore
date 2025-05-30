package technology.rocketjump.mountaincore.screens.menus.options;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.audio.model.SoundAsset;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.RequestSoundMessage;
import technology.rocketjump.mountaincore.persistence.UserPreferences;
import technology.rocketjump.mountaincore.ui.cursor.GameCursor;
import technology.rocketjump.mountaincore.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.mountaincore.ui.i18n.DisplaysText;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;
import technology.rocketjump.mountaincore.ui.skins.GuiSkinRepository;

@Singleton
public class AudioOptionsTab implements OptionsTab, DisplaysText {

	private final Skin skin;
	private final UserPreferences userPreferences;
	private final MessageDispatcher messageDispatcher;
	private final I18nTranslator i18nTranslator;
	private final SoundAsset sliderSoundAsset;

	private Label musicLabel;
	private Slider musicSlider;
	private Label soundEffectLabel;
	private Slider soundEffectSlider;
	private Label ambientEffectLabel;
	private Slider ambientEffectSlider;

	@Inject
	public AudioOptionsTab(UserPreferences userPreferences, GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
						   I18nTranslator i18nTranslator, SoundAssetDictionary soundAssetDictionary) {
		this.userPreferences = userPreferences;
		this.messageDispatcher = messageDispatcher;
		this.skin = guiSkinRepository.getMenuSkin();
		this.i18nTranslator = i18nTranslator;
		this.sliderSoundAsset = soundAssetDictionary.getByName("Slider");

		rebuildUI();
	}

	@Override
	public void populate(Table menuTable) {

		// AUDIO
		menuTable.add(musicLabel).spaceBottom(30f).row();
		menuTable.add(musicSlider).spaceBottom(50f).growX().row();

		menuTable.add(soundEffectLabel).spaceBottom(30f).row();
		menuTable.add(soundEffectSlider).spaceBottom(50f).growX().row();

		menuTable.add(ambientEffectLabel).spaceBottom(30f).row();
		menuTable.add(ambientEffectSlider).spaceBottom(50f).growX().row();
	}

	@Override
	public OptionsTabName getTabName() {
		return OptionsTabName.AUDIO;
	}

	@Override
	public void rebuildUI() {

		musicLabel = new Label(i18nTranslator.translate("GUI.MUSIC_VOLUME"), skin, "options_menu_label");
		musicSlider = new Slider(0, 0.8f, 0.08f, false, skin);
		String savedVolume = userPreferences.getPreference(UserPreferences.PreferenceKey.MUSIC_VOLUME);
		musicSlider.setValue(Float.parseFloat(savedVolume));
		musicSlider.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(sliderSoundAsset));
				Float musicSliderValue = musicSlider.getValue();
				messageDispatcher.dispatchMessage(MessageType.GUI_CHANGE_MUSIC_VOLUME, musicSliderValue);
			}
			return true;
		});
		musicSlider.addListener(new ChangeCursorOnHover(musicSlider, GameCursor.REORDER_HORIZONTAL, messageDispatcher));

		soundEffectLabel = new Label(i18nTranslator.translate("GUI.SOUND_EFFECT_VOLUME"), skin, "options_menu_label");
		soundEffectSlider = new Slider(0, 1, 0.1f, false, skin);
		String savedSoundEffectVolume = userPreferences.getPreference(UserPreferences.PreferenceKey.SOUND_EFFECT_VOLUME);
		soundEffectSlider.setValue(Float.parseFloat(savedSoundEffectVolume));
		soundEffectSlider.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(sliderSoundAsset));
				Float sliderValue = soundEffectSlider.getValue();
				messageDispatcher.dispatchMessage(MessageType.GUI_CHANGE_SOUND_EFFECT_VOLUME, sliderValue);
			}
			return true;
		});
		soundEffectSlider.addListener(new ChangeCursorOnHover(soundEffectSlider, GameCursor.REORDER_HORIZONTAL, messageDispatcher));

		ambientEffectLabel = new Label(i18nTranslator.translate("GUI.AMBIENT_EFFECT_VOLUME"), skin, "options_menu_label");
		ambientEffectSlider = new Slider(0, 1, 0.1f, false, skin);
		String savedAmbientEffectVolume = userPreferences.getPreference(UserPreferences.PreferenceKey.AMBIENT_EFFECT_VOLUME);
		ambientEffectSlider.setValue(Float.parseFloat(savedAmbientEffectVolume));
		ambientEffectSlider.addListener((event) -> {
			if (event instanceof ChangeListener.ChangeEvent) {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(sliderSoundAsset));
				Float sliderValue = ambientEffectSlider.getValue();
				messageDispatcher.dispatchMessage(MessageType.GUI_CHANGE_AMBIENT_EFFECT_VOLUME, sliderValue);
			}
			return true;
		});
		ambientEffectSlider.addListener(new ChangeCursorOnHover(ambientEffectSlider, GameCursor.REORDER_HORIZONTAL, messageDispatcher));
	}
}
