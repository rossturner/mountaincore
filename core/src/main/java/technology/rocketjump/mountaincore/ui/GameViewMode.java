package technology.rocketjump.mountaincore.ui;

import java.util.Locale;

public enum GameViewMode {

	DEFAULT,
	JOB_PRIORITY,
	ROOFING_INFO,
	PIPING,
	MECHANISMS;

	public String getButtonStyleName() {
		return "view_mode_" + name().toLowerCase(Locale.ROOT);
	}

	public String getI18nKey() {
		return "GUI.VIEW_MODE." + name();
	}

}
