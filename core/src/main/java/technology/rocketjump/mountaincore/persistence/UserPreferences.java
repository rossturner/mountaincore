package technology.rocketjump.mountaincore.persistence;

import com.alibaba.fastjson.JSONObject;
import com.google.inject.Inject;
import com.google.inject.ProvidedBy;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.input.CommandName;
import technology.rocketjump.mountaincore.persistence.model.KeyBinding;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;

import static technology.rocketjump.mountaincore.modding.LocalModRepository.DEFAULT_ACTIVE_MODS;
import static technology.rocketjump.mountaincore.modding.LocalModRepository.MOD_NAME_SEPARATOR;
import static technology.rocketjump.mountaincore.persistence.UserPreferences.FullscreenMode.BORDERLESS_FULLSCREEN;

@Singleton
@ProvidedBy(UserPreferencesProvider.class)
public class UserPreferences {


	private final File propertiesFile;
	private final Properties properties = new Properties();
	private final List<KeyBinding> keyBindings = new ArrayList<>();

	public static String preferencesJson; // for CrashHandler to use statically

	@Inject
	public UserPreferences(UserFileManager userFileManager) {
		propertiesFile = userFileManager.getOrCreateFile("preferences.properties");
		FileInputStream inputStream = null;
		try {
			inputStream = FileUtils.openInputStream(propertiesFile);
			properties.load(inputStream);
			loadKeyBindings();

			preferencesJson = JSONObject.toJSONString(properties);
		} catch (IOException e) {
			Logger.error(e, "Failed to load " + propertiesFile.getAbsolutePath());
		} finally {
			if (inputStream != null) {
				IOUtils.closeQuietly(inputStream);
			}
		}
	}

	public Set<CommandName> getCommandsFor(Set<Integer> pressedKeys) {
		Set<CommandName> commandNames = new HashSet<>();
		for (KeyBinding keyBinding : keyBindings) {
			if (pressedKeys.containsAll(keyBinding.keys())) {
				commandNames.add(keyBinding.commandName());
			}
		}
		return commandNames;
	}

	public String getInputKeyDescriptionFor(CommandName action, boolean isPrimary) {
		for (KeyBinding keyBinding : keyBindings) {
			if (keyBinding.commandName() == action && keyBinding.isPrimary() == isPrimary) {
				return keyBinding.getInputKeyDescription();
			}
		}
		return null;
	}

	public void assignInput(CommandName commandName, Set<Integer> keys, boolean isPrimary) {
		Optional<KeyBinding> existingAllocationForInput = keyBindings.stream().filter(allocation -> allocation.keys().equals(keys)).findFirst();
		existingAllocationForInput.ifPresent(a -> {
			removePreference(a.getPropertyKey());
			keyBindings.remove(a);
		});

		Optional<KeyBinding> toReplace = keyBindings.stream().filter(allocation -> allocation.commandName() == commandName && allocation.isPrimary() == isPrimary).findFirst();
		toReplace.ifPresent(a -> {
			removePreference(a.getPropertyKey());
			keyBindings.remove(a);
		});
		KeyBinding newAllocation = new KeyBinding(commandName, keys, isPrimary);
		keyBindings.add(newAllocation);
		// TODO figure out what default would be and remove preference if it is that

		if (commandName.toDefaultKeybindings().contains(newAllocation)) {
			removePreference(newAllocation.getPropertyKey());
		} else {
			setPreference(newAllocation.getPropertyKey(), newAllocation.getPropertyValue());
		}
	}

	/**
	 * Best not to rename these as any existing saved preferences will be lost
	 */
	public enum PreferenceKey {

		MUSIC_VOLUME("0.24"),
		AMBIENT_EFFECT_VOLUME("0.5"),
		SOUND_EFFECT_VOLUME("0.6"),
		SAVE_LOCATION(null),
		CRASH_REPORTING(null),
		LANGUAGE("en-gb"),
		DISPLAY_RESOLUTION(null),
		FULLSCREEN_MODE(BORDERLESS_FULLSCREEN.name()),
		UI_SCALE("1.0"),
		EDGE_SCROLLING("true"),
		ZOOM_TO_CURSOR("true"),
		TREE_TRANSPARENCY("true"),
		ACTIVE_MODS(StringUtils.join(DEFAULT_ACTIVE_MODS, MOD_NAME_SEPARATOR)),
		ENABLE_TUTORIAL("true"),
		WEATHER_EFFECTS("true"),
		TWITCH_TOKEN(null),
		TWITCH_INTEGRATION_ENABLED("false"),
		TWITCH_VIEWERS_AS_SETTLER_NAMES("false"),
		TWITCH_PRIORITISE_SUBSCRIBERS("false"),
		MODIO_ACCESS_TOKEN(null),
		MODIO_ACCESS_TOKEN_EXPIRY("0");

		public final String defaultValue;

		PreferenceKey(String defaultValue) {
			this.defaultValue = defaultValue;
		}


	}
	public String getPreference(PreferenceKey key) {
		String property = properties.getProperty(key.name());
		if (property == null) {
			// Force saving of some preferences to always expose them for modification
			return key.defaultValue;
		} else {
			return property;
		}
	}

	public boolean hasPreference(PreferenceKey preferenceKey) {
		return properties.getProperty(preferenceKey.name()) != null;
	}

	public void setPreference(PreferenceKey preferenceKey, String value) {
		if (value.equals(preferenceKey.defaultValue)) {
			removePreference(preferenceKey);
		} else {
			setPreference(preferenceKey.name(), value);
		}
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

	public void resetToDefaultKeyBindings() {
		for (KeyBinding keyBinding : keyBindings) {
			removePreference(keyBinding.getPropertyKey());
		}

		keyBindings.clear();

		for (CommandName commandName : CommandName.values()) {
			boolean isPrimary = true;
			for (Set<Integer> keybinding : commandName.defaultKeys) {
				assignInput(commandName, keybinding, isPrimary);
				isPrimary = false;
			}
		}
	}

	private void loadKeyBindings() {
		for (Map.Entry<Object, Object> entry : properties.entrySet()) {
			String key = entry.getKey().toString();
			String value = entry.getValue().toString();

			Matcher keyMatcher = KeyBinding.KEY_PATTERN.matcher(key);
			Matcher integerMatcher = KeyBinding.INTEGER_PATTERN.matcher(value);
			if (keyMatcher.matches() && value.startsWith(KeyBinding.VALUE_PREFIX)) {
				Set<Integer> keys = new HashSet<>();
				while (integerMatcher.find()) {
					keys.add(Integer.parseInt(integerMatcher.group()));
				}
				CommandName commandName = CommandName.parse(keyMatcher.group(1));
				boolean isPrimary = KeyBinding.PRIMARY_SUFFIX.equals(keyMatcher.group(2));
				if (commandName != null && !keys.isEmpty()) {
					keyBindings.add(new KeyBinding(commandName, keys, isPrimary));
				}
			}
		}
		if (keyBindings.isEmpty()) {
			resetToDefaultKeyBindings();
		}
	}


}
