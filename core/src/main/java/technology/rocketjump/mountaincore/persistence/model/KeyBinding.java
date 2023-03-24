package technology.rocketjump.mountaincore.persistence.model;

import com.badlogic.gdx.Input;
import technology.rocketjump.mountaincore.input.CommandName;

import java.util.*;
import java.util.regex.Pattern;

public record KeyBinding(CommandName commandName, Set<Integer> keys, boolean isPrimary) {

	public static final Pattern KEY_PATTERN = Pattern.compile("(\\w+)(_(PRIMARY|SECONDARY))");
	public static final Pattern INTEGER_PATTERN = Pattern.compile("[0-9]+");
	public static final String VALUE_PREFIX = "KEYBOARD_";
	public static final String PRIMARY_SUFFIX = "_PRIMARY";

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
