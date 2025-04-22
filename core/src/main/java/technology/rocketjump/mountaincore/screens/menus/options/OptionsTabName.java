package technology.rocketjump.mountaincore.screens.menus.options;

import java.util.List;

public enum OptionsTabName {

	GRAPHICS,
	AUDIO,
	GAMEPLAY,
	TWITCH;

	public static List<OptionsTabName> displayedOptionsTabs() {
		return List.of(GRAPHICS, AUDIO, GAMEPLAY);
	}

	public String getI18nKey() {
		return "GUI.OPTIONS.TAB." + this.name();
	}

}
