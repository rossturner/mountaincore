package technology.rocketjump.saul.jobs.model;

import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.saul.rendering.utils.HexColors;

public enum JobPriority {

	// These are expected to be in order of highest to lowest priority
	HIGHEST("#265aaa", "fast-forward-button-up", "PRIORITY.HIGHEST", "btn_crafting_priority_5", "btn_priority_5"),
	HIGHER("#269caa", "play-button-up", "PRIORITY.HIGHER", "btn_crafting_priority_4", "btn_priority_4"),
	NORMAL("#27aa5e", "play-button", "PRIORITY.NORMAL", "btn_crafting_priority_3", "btn_priority_3"),
	LOWER("#bab524", "play-button-down", "PRIORITY.LOWER", "btn_crafting_priority_2", "btn_priority_2"),
	LOWEST("#aa8026", "fast-forward-button-down", "PRIORITY.LOWEST", "btn_crafting_priority_1", "btn_priority_1"),
	DISABLED("#D4534C", "cancel", "PRIORITY.DISABLED", "btn_crafting_priority_0", "btn_priority_0");

	@Deprecated
	public final Color color;
	@Deprecated
	public final Color semiTransparentColor;
	@Deprecated
	public final String iconName;
	public final String i18nKey;
	public final String craftingDrawableName;
	public final String buttonDrawableName;

	JobPriority(String hexColor, String iconName, String i18nKey, String craftingDrawableName, String buttonDrawableName) {
		this.color = HexColors.get(hexColor);
		this.buttonDrawableName = buttonDrawableName;
		this.semiTransparentColor = this.color.cpy();
		this.semiTransparentColor.a = 0.8f;
		this.iconName = iconName;
		this.i18nKey = i18nKey;
		this.craftingDrawableName = craftingDrawableName;
	}

}
