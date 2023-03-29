package technology.rocketjump.mountaincore.input;

import com.badlogic.gdx.Input;
import technology.rocketjump.mountaincore.persistence.model.KeyBinding;

import java.util.List;
import java.util.Set;

public enum CommandName {
	PAN_CAMERA_UP(Set.of(Input.Keys.W), Set.of(Input.Keys.UP)),
	PAN_CAMERA_DOWN(Set.of(Input.Keys.S), Set.of(Input.Keys.DOWN)),
	PAN_CAMERA_LEFT(Set.of(Input.Keys.A), Set.of(Input.Keys.LEFT)),
	PAN_CAMERA_RIGHT(Set.of(Input.Keys.D), Set.of(Input.Keys.RIGHT)),
	FAST_PAN(Set.of(Input.Keys.SHIFT_LEFT), Set.of(Input.Keys.SHIFT_RIGHT)),
	ZOOM_IN(Set.of(Input.Keys.E), Set.of(Input.Keys.PAGE_UP)),
	ZOOM_OUT(Set.of(Input.Keys.Q), Set.of(Input.Keys.PAGE_DOWN)),
	ROTATE(Set.of(Input.Keys.R)),
	PAUSE(Set.of(Input.Keys.SPACE)),
	GAME_SPEED_NORMAL(Set.of(Input.Keys.NUM_1)),
	GAME_SPEED_FAST(Set.of(Input.Keys.NUM_2)),
	GAME_SPEED_FASTER(Set.of(Input.Keys.NUM_3)),
	GAME_SPEED_FASTEST(Set.of(Input.Keys.NUM_4)),
	QUICKSAVE(Set.of(Input.Keys.F5)),
	QUICKLOAD(Set.of(Input.Keys.F8)),

	DEBUG_SHOW_MENU(Set.of(Input.Keys.GRAVE)),
	DEBUG_SHOW_JOB_STATUS(Set.of(Input.Keys.J)),
	DEBUG_SHOW_LIQUID_FLOW(Set.of(Input.Keys.F)),
	DEBUG_SHOW_ZONES(Set.of(Input.Keys.Z)),
	DEBUG_SHOW_PATHFINDING_NODES(Set.of(Input.Keys.T)),
	DEBUG_TOGGLE_FLOOR_OVERLAP_RENDERING(Set.of(Input.Keys.O)),
	DEBUG_HIDE_GUI(Set.of(Input.Keys.G)),
	DEBUG_GAME_SPEED_SLOW(Set.of(Input.Keys.NUM_6)),
	DEBUG_GAME_SPEED_ULTRA_FAST(Set.of(Input.Keys.NUM_5)),
	DEBUG_SHOW_INDIVIDUAL_LIGHTING_BUFFERS(Set.of(Input.Keys.L)),
	DEBUG_FRAME_BUFFER_0(Set.of(Input.Keys.CONTROL_LEFT, Input.Keys.NUM_0)),
	DEBUG_FRAME_BUFFER_1(Set.of(Input.Keys.CONTROL_LEFT, Input.Keys.NUM_1)),
	DEBUG_FRAME_BUFFER_2(Set.of(Input.Keys.CONTROL_LEFT, Input.Keys.NUM_2)),
	DEBUG_FRAME_BUFFER_3(Set.of(Input.Keys.CONTROL_LEFT, Input.Keys.NUM_3)),
	DEBUG_FRAME_BUFFER_4(Set.of(Input.Keys.CONTROL_LEFT, Input.Keys.NUM_4)),
	DEBUG_FRAME_BUFFER_5(Set.of(Input.Keys.CONTROL_LEFT, Input.Keys.NUM_5)),
	DEBUG_FRAME_BUFFER_6(Set.of(Input.Keys.CONTROL_LEFT, Input.Keys.NUM_6)),
	DEBUG_FRAME_BUFFER_7(Set.of(Input.Keys.CONTROL_LEFT, Input.Keys.NUM_7)),
	DEBUG_FRAME_BUFFER_8(Set.of(Input.Keys.CONTROL_LEFT, Input.Keys.NUM_8)),
	DEBUG_FRAME_BUFFER_9(Set.of(Input.Keys.CONTROL_LEFT, Input.Keys.NUM_9)),
	DEBUG_STORE_CAMERA_POSITION(Set.of(Input.Keys.F1)),
	DEBUG_RETRIEVE_CAMERA_POSITION(Set.of(Input.Keys.F4));

	public final List<Set<Integer>> defaultKeys;

	CommandName(Set<Integer>... defaultKeys) {
		this.defaultKeys = List.of(defaultKeys);
	}

	public static CommandName parse(String value) {
		for (CommandName commandName : values()) {
			if (commandName.name().equals(value)) {
				return commandName;
			}
		}
		return null;
	}

	public String getI18nKey() {
		return "GUI.KEY_BINDING." + name();
	}

	public List<KeyBinding> toDefaultKeybindings() {
		return defaultKeys.stream()
				.map(keys -> new KeyBinding(this, keys, defaultKeys.indexOf(keys) == 0))
				.toList();
	}
}
