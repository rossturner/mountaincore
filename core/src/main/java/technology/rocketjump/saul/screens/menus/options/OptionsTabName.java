package technology.rocketjump.saul.screens.menus.options;

public enum OptionsTabName {

	GRAPHICS,
	AUDIO,
	GAMEPLAY,
	TWITCH;

	public String getI18nKey() {
		return "GUI.OPTIONS.TAB." + this.name();
	}

}
