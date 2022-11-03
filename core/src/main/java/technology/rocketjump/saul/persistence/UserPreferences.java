package technology.rocketjump.saul.persistence;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.Input;
import com.google.inject.Inject;
import com.google.inject.ProvidedBy;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.input.CommandName;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

@Singleton
@ProvidedBy(UserPreferencesProvider.class)
public class UserPreferences {

	private record KeyBinding(CommandName commandName, Set<Integer> keys, boolean isMouse, boolean isPrimary) {
		public String getPropertyKey() {
			if (isPrimary) {
				return commandName.name() + "_PRIMARY";
			} else {
				return commandName.name() + "_SECONDARY";
			}
		}

		public String getPropertyValue() {
			if (isMouse) {
				return "MOUSE_" + keys;
			} else {
				return "KEYBOARD_" + keys;
			}
		}

		public String getInputKeyDescription() {
			StringJoiner keyDescription = new StringJoiner("+");


			for (Integer key : keys) {
				if (isMouse) {
					return switch (key) {
						case Input.Buttons.LEFT -> "LMB";
						case Input.Buttons.MIDDLE ->"MMB";
						case Input.Buttons.RIGHT ->"RMB";
						case Input.Buttons.FORWARD ->"Forward";
						case Input.Buttons.BACK ->"Backward";
						default -> null;
					};
				} else {
					keyDescription.add(Input.Keys.toString(key));
				}
			}

			return keyDescription.toString();
		}
	}

	private final File propertiesFile;
	private final Properties properties = new Properties();
	private List<KeyBinding> keyBindings = new ArrayList<>();

	public static String preferencesJson; // for CrashHandler to use statically

	@Inject
	public UserPreferences(UserFileManager userFileManager) {
		propertiesFile = userFileManager.getOrCreateFile("preferences.properties");
		FileInputStream inputStream = null;
		try {
			inputStream = FileUtils.openInputStream(propertiesFile);
			properties.load(inputStream);


			preferencesJson = JSONObject.toJSONString(properties);
		} catch (IOException e) {
			Logger.error(e, "Failed to load " + propertiesFile.getAbsolutePath());
		} finally {
			if (inputStream != null) {
				IOUtils.closeQuietly(inputStream);
			}
		}
	}

	//TODO: not keen on this design, need to be careful as otherPressedKeys build up from a queue, so might want to avoid duplicate commands returned
	public Set<CommandName> getCommandsFor(int keycode, Set<Integer> otherPressedKeys) {
		Set<CommandName> commandNames = new HashSet<>();
		for (KeyBinding keyBinding : keyBindings) {
			Set<Integer> chordPressed = new HashSet<>(keyBinding.keys());
			chordPressed.removeAll(otherPressedKeys);

			if (!keyBinding.isMouse && chordPressed.size() == 1 && chordPressed.contains(keycode)) {
				commandNames.add(keyBinding.commandName);
			}
		}

		return commandNames;
	}

	//TODO: maybe better datastructure?
	public String getInputFor(CommandName action, boolean isPrimary) {
		for (KeyBinding keyBinding : keyBindings) {
			if (keyBinding.commandName == action && keyBinding.isPrimary == isPrimary) {
				return keyBinding.getInputKeyDescription();
			}
		}
		return null;
	}

	//TODO: collisions
	public void assignInput(CommandName commandName, boolean isMouse, Set<Integer> keys, boolean isPrimary) {

		//TODO: this needs to propagate out somehow to tell to clear?
		Optional<KeyBinding> existingAllocationForInput = keyBindings.stream().filter(allocation -> allocation.isMouse == isMouse && allocation.keys.equals(keys)).findFirst();
		existingAllocationForInput.ifPresent(a -> {
			removePreference(a.getPropertyKey());
			keyBindings.remove(a);
		});

		Optional<KeyBinding> toReplace = keyBindings.stream().filter(allocation -> allocation.commandName == commandName && allocation.isPrimary == isPrimary).findFirst();
		toReplace.ifPresent(a -> {
			removePreference(a.getPropertyKey());
			keyBindings.remove(a);
		});

		KeyBinding newAllocation = new KeyBinding(commandName, keys, isMouse, isPrimary);
		keyBindings.add(newAllocation);
		setPreference(newAllocation.getPropertyKey(), newAllocation.getPropertyValue());
	}

	/**
	 * Best not to rename these as any existing saved preferences will be lost
	 */
	public enum PreferenceKey {

		MUSIC_VOLUME,
		AMBIENT_EFFECT_VOLUME,
		SOUND_EFFECT_VOLUME,
		SAVE_LOCATION,
		CRASH_REPORTING,
		LANGUAGE,
		DISPLAY_RESOLUTION,
		DISPLAY_FULLSCREEN,
		FULLSCREEN_MODE,
		EDGE_SCROLLING,
		ZOOM_TO_CURSOR,
		TREE_TRANSPARENCY,
		PAUSE_FOR_NOTIFICATIONS,
		ACTIVE_MODS,
		ALLOW_HINTS,
		ENABLE_TUTORIAL,

		TWITCH_TOKEN,
		TWITCH_INTEGRATION_ENABLED,
		TWITCH_VIEWERS_AS_SETTLER_NAMES,
		TWITCH_PRIORITISE_SUBSCRIBERS;

	}
	private static final List<PreferenceKey> ALWAYS_PERSIST_KEYS = Arrays.asList(PreferenceKey.SAVE_LOCATION, PreferenceKey.FULLSCREEN_MODE);

	public String getPreference(PreferenceKey key, String defaultValue) {
		String property = properties.getProperty(key.name());
		if (property == null) {
			// Force saving of some preferences to always expose them for modification
			if (ALWAYS_PERSIST_KEYS.contains(key)) {
				setPreference(key, defaultValue);
			}
			return defaultValue;
		} else {
			return property;
		}
	}

	public void setPreference(PreferenceKey preferenceKey, String value) {
		setPreference(preferenceKey.name(), value);
	}

	public void removePreference(PreferenceKey preferenceKey) {
		removePreference(preferenceKey.name());
	}

	private void setPreference(String propertyKey, String value) {
		properties.setProperty(propertyKey, value);
		persist();
	}

	private void removePreference(String propertyKey) {
		properties.remove(propertyKey);
		persist();
	}

	private void persist() {
		FileOutputStream outputStream = null;
		try {
			outputStream = FileUtils.openOutputStream(propertiesFile);
			properties.store(outputStream, "This is a list of user preferences, which is managed by the King under the Mountain game client");
		} catch (IOException e) {
			Logger.error(e, "Failed to load " + propertiesFile.getAbsolutePath());
		} finally {
			if (outputStream != null) {
				IOUtils.closeQuietly(outputStream);
			}
		}

		preferencesJson = JSONObject.toJSONString(properties);
	}

	public enum FullscreenMode {

		WINDOWED("GUI.OPTIONS.GRAPHICS.FULLSCREENMODE.WINDOWED"),
		BORDERLESS_FULLSCREEN("GUI.OPTIONS.GRAPHICS.FULLSCREENMODE.BORDERLESS_FULLSCREEN"),
		EXCLUSIVE_FULLSCREEN("GUI.OPTIONS.GRAPHICS.FULLSCREENMODE.EXCLUSIVE_FULLSCREEN");

		public final String i18nKey;

		FullscreenMode(String i18nKey) {
			this.i18nKey = i18nKey;
		}
	}

}
