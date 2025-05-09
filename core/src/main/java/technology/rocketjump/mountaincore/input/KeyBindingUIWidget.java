package technology.rocketjump.mountaincore.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.persistence.UserPreferences;
import technology.rocketjump.mountaincore.rendering.camera.GlobalSettings;
import technology.rocketjump.mountaincore.ui.cursor.GameCursor;
import technology.rocketjump.mountaincore.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.mountaincore.ui.eventlistener.ClickableSoundsListener;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;

import java.util.*;
import java.util.function.Consumer;

public class KeyBindingUIWidget extends Table {

	private final Skin skin;
	private final UserPreferences userPreferences;
	private final MessageDispatcher messageDispatcher;
	private final SoundAssetDictionary soundAssetDictionary;
	private final Map<CommandName, TextButton> primaryButtons = new HashMap<>();
	private final Map<CommandName, TextButton> secondaryButtons = new HashMap<>();

	public KeyBindingUIWidget(Skin skin, UserPreferences userPreferences, I18nTranslator i18nTranslator, MessageDispatcher messageDispatcher, SoundAssetDictionary soundAssetDictionary) {
		this.skin = skin;
		this.userPreferences = userPreferences;
		this.messageDispatcher = messageDispatcher;
		this.soundAssetDictionary = soundAssetDictionary;

		defaults().padRight(30f).padLeft(30f);

		for (CommandName commandName : CommandName.values()) {
			String name = commandName.name();
			if (!GlobalSettings.DEV_MODE && name.startsWith("DEBUG_")) {

			} else {
				Label actionLabel = new Label(i18nTranslator.getTranslatedString(commandName.getI18nKey()).toString(), this.skin, "options_menu_label");
				TextButton primaryKey = createTextButton(commandName, true, userPreferences);
				TextButton secondaryKey = createTextButton(commandName, false, userPreferences);
				add(actionLabel).growX();
				add(primaryKey);
				add(secondaryKey);
				row();
				primaryButtons.put(commandName, primaryKey);
				secondaryButtons.put(commandName, secondaryKey);
			}
		}
	}

	public void resetToDefaultSettings() {
		userPreferences.resetToDefaultKeyBindings();
		resetTextButtons();
	}

	private TextButton createTextButton(CommandName action, boolean isPrimary, UserPreferences userPreferences) {
		TextButton textButton = new TextButton(this.userPreferences.getInputKeyDescriptionFor(action, isPrimary), this.skin, "btn_key_bindings_key");
		textButton.addListener(new ClickListener(){
			@Override
			public void clicked(InputEvent event, float x, float y) {
				textButton.setText("...");
				InputProcessor currentInputProcessor = Gdx.input.getInputProcessor();
				Gdx.input.setInputProcessor(new KeyBindingInputProcessor(currentInputProcessor, keyboardKeys -> {
					textButton.setChecked(false);
					userPreferences.assignInput(action, keyboardKeys, isPrimary);
					resetTextButtons();
				}));

			}
		});

		textButton.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
		textButton.addListener(new ChangeCursorOnHover(textButton, GameCursor.SELECT, messageDispatcher));
		return textButton;
	}

	private void resetTextButtons() {
		primaryButtons.forEach((key, value) -> {
			String inputDescription = userPreferences.getInputKeyDescriptionFor(key, true);
			value.setText(inputDescription);
		});
		secondaryButtons.forEach((key, value) -> {
			String inputDescription = userPreferences.getInputKeyDescriptionFor(key, false);
			value.setText(inputDescription);
		});
	}

	public static class KeyBindingInputProcessor extends InputAdapter {
		private final InputProcessor currentInputProcessor;
		private final Consumer<Set<Integer>> keyboardCapture;
		private final Set<Integer> keysPressed = new HashSet<>();

		public KeyBindingInputProcessor(InputProcessor currentInputProcessor, Consumer<Set<Integer>> keyboardCapture) {
			this.currentInputProcessor = currentInputProcessor;
			this.keyboardCapture = keyboardCapture;
		}

		@Override
		public boolean keyDown(int keycode) {
			keysPressed.add(keycode);
			return true;
		}

		@Override
		public boolean keyUp(int keycode) {
			if (keysPressed.contains(Input.Keys.ESCAPE)) {
				keyboardCapture.accept(Collections.emptySet());
			} else {
				keyboardCapture.accept(keysPressed);
			}
			Gdx.input.setInputProcessor(currentInputProcessor);
			return true;
		}

	}
}
