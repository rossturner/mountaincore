package technology.rocketjump.saul.ui;

public enum GameViewMode {

	DEFAULT,
	JOB_PRIORITY,
	ROOFING_INFO,
	PIPING,
	MECHANISMS;

	public String getButtonStyleName() {
		return "view_mode_" + name().toLowerCase();
	}

	public String getI18nKey() {
		return "GUI.VIEW_MODE." + name();
	}

}
