package technology.rocketjump.mountaincore.jobs.model;

import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.mountaincore.rendering.utils.HexColors;

public enum JobPriority {

	// These are expected to be in order of highest to lowest priority
	HIGHEST("#265aaa", "PRIORITY.HIGHEST", "btn_crafting_priority_5", "btn_priority_5"),
	HIGHER("#269caa", "PRIORITY.HIGHER", "btn_crafting_priority_4", "btn_priority_4"),
	NORMAL("#27aa5e", "PRIORITY.NORMAL", "btn_crafting_priority_3", "btn_priority_3"),
	LOWER("#bab524", "PRIORITY.LOWER", "btn_crafting_priority_2", "btn_priority_2"),
	LOWEST("#aa8026", "PRIORITY.LOWEST", "btn_crafting_priority_1", "btn_priority_1"),
	DISABLED("#D4534C", "PRIORITY.DISABLED", "btn_crafting_priority_0", "btn_priority_0");

	@Deprecated
	public final Color color;
	@Deprecated
	public final String iconName;
	public final String i18nKey;
	public final String craftingDrawableName;
	public final String buttonDrawableName;

	JobPriority(String hexColor, String i18nKey, String craftingDrawableName, String buttonDrawableName) {
		this.color = HexColors.get(hexColor);
		this.buttonDrawableName = buttonDrawableName;
		this.iconName = "priority_"+name().toLowerCase();
		this.i18nKey = i18nKey;
		this.craftingDrawableName = craftingDrawableName;
	}

}
