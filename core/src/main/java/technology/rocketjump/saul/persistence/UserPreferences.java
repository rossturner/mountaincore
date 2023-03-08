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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
@ProvidedBy(UserPreferencesProvider.class)
public class UserPreferences {


	private record KeyBinding(CommandName commandName, Set<Integer> keys, boolean isPrimary) {
		static final Pattern KEY_PATTERN = Pattern.compile("(\\w+)(_(PRIMARY|SECONDARY))");
		static final Pattern INTEGER_PATTERN = Pattern.compile("[0-9]+");
		static final String VALUE_PREFIX = "KEYBOARD_";
		static final String PRIMARY_SUFFIX = "_PRIMARY";

		public String getPropertyKey() {
			if (isPrimary) {
				return commandName.name() + PRIMARY_SUFFIX;
			} else {
				return commandName.name() + "_SECONDARY";
			}
		}

		public String getPropertyValue() {
			return VALUE_PREFIX + keys;
		}

		public String getInputKeyDescription() {
			StringJoiner keyDescription = new StringJoiner("+");
			List<String> terms = new ArrayList<>();

			for (Integer key : keys) {
				terms.add(Input.Keys.toString(key));
			}
			terms.sort(Comparator.comparing(String::length).reversed());

			for (String term : terms) {
				keyDescription.add(term);
			}

			return keyDescription.toString();
		}
	}

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
				commandNames.add(keyBinding.commandName);
			}
		}
		return commandNames;
	}

	public String getInputKeyDescriptionFor(CommandName action, boolean isPrimary) {
		for (KeyBinding keyBinding : keyBindings) {
			if (keyBinding.commandName == action && keyBinding.isPrimary == isPrimary) {
				return keyBinding.getInputKeyDescription();
			}
		}
		return null;
	}

	public void assignInput(CommandName commandName, Set<Integer> keys, boolean isPrimary) {
		Optional<KeyBinding> existingAllocationForInput = keyBindings.stream().filter(allocation -> allocation.keys.equals(keys)).findFirst();
		existingAllocationForInput.ifPresent(a -> {
			removePreference(a.getPropertyKey());
			keyBindings.remove(a);
		});


		Optional<KeyBinding> toReplace = keyBindings.stream().filter(allocation -> allocation.commandName == commandName && allocation.isPrimary == isPrimary).findFirst();
		toReplace.ifPresent(a -> {
			removePreference(a.getPropertyKey());
			keyBindings.remove(a);
		});
		KeyBinding newAllocation = new KeyBinding(commandName, keys, isPrimary);
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
		ACTIVE_MODS,
		ENABLE_TUTORIAL,

		WEATHER_EFFECTS,

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

	public void resetToDefaultKeyBindings() {
		for (KeyBinding keyBinding : keyBindings) {
			removePreference(keyBinding.getPropertyKey());
		}

		keyBindings.clear();


		assignInput(CommandName.PAN_CAMERA_UP, Set.of(Input.Keys.W), true);
		assignInput(CommandName.PAN_CAMERA_UP, Set.of(Input.Keys.UP), false);
		assignInput(CommandName.PAN_CAMERA_DOWN, Set.of(Input.Keys.S), true);
		assignInput(CommandName.PAN_CAMERA_DOWN, Set.of(Input.Keys.DOWN), false);
		assignInput(CommandName.PAN_CAMERA_LEFT, Set.of(Input.Keys.A), true);
		assignInput(CommandName.PAN_CAMERA_LEFT, Set.of(Input.Keys.LEFT), false);
		assignInput(CommandName.PAN_CAMERA_RIGHT, Set.of(Input.Keys.D), true);
		assignInput(CommandName.PAN_CAMERA_RIGHT, Set.of(Input.Keys.RIGHT), false);
		assignInput(CommandName.FAST_PAN, Set.of(Input.Keys.SHIFT_LEFT), true);
		assignInput(CommandName.FAST_PAN, Set.of(Input.Keys.SHIFT_RIGHT), false);
		assignInput(CommandName.ZOOM_IN, Set.of(Input.Keys.E), true);
		assignInput(CommandName.ZOOM_IN, Set.of(Input.Keys.PAGE_UP), false);
		assignInput(CommandName.ZOOM_OUT, Set.of(Input.Keys.Q), true);
		assignInput(CommandName.ZOOM_OUT, Set.of(Input.Keys.PAGE_DOWN), false);

		assignInput(CommandName.ROTATE, Set.of(Input.Keys.R), true);
		assignInput(CommandName.PAUSE, Set.of(Input.Keys.SPACE), true);
		assignInput(CommandName.GAME_SPEED_NORMAL, Set.of(Input.Keys.NUM_1), true);
		assignInput(CommandName.GAME_SPEED_FAST, Set.of(Input.Keys.NUM_2), true);
		assignInput(CommandName.GAME_SPEED_FASTER, Set.of(Input.Keys.NUM_3), true);
		assignInput(CommandName.GAME_SPEED_FASTEST, Set.of(Input.Keys.NUM_4), true);
		assignInput(CommandName.DEBUG_GAME_SPEED_ULTRA_FAST, Set.of(Input.Keys.NUM_5), true);
		assignInput(CommandName.DEBUG_GAME_SPEED_SLOW, Set.of(Input.Keys.NUM_6), true);

		assignInput(CommandName.QUICKSAVE, Set.of(Input.Keys.F5), true);
		assignInput(CommandName.QUICKLOAD, Set.of(Input.Keys.F8), true);

		assignInput(CommandName.DEBUG_SHOW_MENU, Set.of(Input.Keys.GRAVE), true);
		assignInput(CommandName.DEBUG_SHOW_JOB_STATUS, Set.of(Input.Keys.J), true);
		assignInput(CommandName.DEBUG_SHOW_LIQUID_FLOW, Set.of(Input.Keys.F), true);
		assignInput(CommandName.DEBUG_SHOW_ZONES, Set.of(Input.Keys.Z), true);
		assignInput(CommandName.DEBUG_SHOW_PATHFINDING_NODES, Set.of(Input.Keys.T), true);
		assignInput(CommandName.DEBUG_TOGGLE_FLOOR_OVERLAP_RENDERING, Set.of(Input.Keys.O), true);
		assignInput(CommandName.DEBUG_HIDE_GUI, Set.of(Input.Keys.G), true);
		assignInput(CommandName.DEBUG_SHOW_INDIVIDUAL_LIGHTING_BUFFERS, Set.of(Input.Keys.L), true);
		assignInput(CommandName.DEBUG_FRAME_BUFFER_0, Set.of(Input.Keys.CONTROL_LEFT, Input.Keys.NUM_0), true);
		assignInput(CommandName.DEBUG_FRAME_BUFFER_1, Set.of(Input.Keys.CONTROL_LEFT, Input.Keys.NUM_1), true);
		assignInput(CommandName.DEBUG_FRAME_BUFFER_2, Set.of(Input.Keys.CONTROL_LEFT, Input.Keys.NUM_2), true);
		assignInput(CommandName.DEBUG_FRAME_BUFFER_3, Set.of(Input.Keys.CONTROL_LEFT, Input.Keys.NUM_3), true);
		assignInput(CommandName.DEBUG_FRAME_BUFFER_4, Set.of(Input.Keys.CONTROL_LEFT, Input.Keys.NUM_4), true);
		assignInput(CommandName.DEBUG_FRAME_BUFFER_5, Set.of(Input.Keys.CONTROL_LEFT, Input.Keys.NUM_5), true);
		assignInput(CommandName.DEBUG_FRAME_BUFFER_6, Set.of(Input.Keys.CONTROL_LEFT, Input.Keys.NUM_6), true);
		assignInput(CommandName.DEBUG_FRAME_BUFFER_7, Set.of(Input.Keys.CONTROL_LEFT, Input.Keys.NUM_7), true);
		assignInput(CommandName.DEBUG_FRAME_BUFFER_8, Set.of(Input.Keys.CONTROL_LEFT, Input.Keys.NUM_8), true);
		assignInput(CommandName.DEBUG_FRAME_BUFFER_9, Set.of(Input.Keys.CONTROL_LEFT, Input.Keys.NUM_9), true);
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
