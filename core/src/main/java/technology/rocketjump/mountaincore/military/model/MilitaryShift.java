package technology.rocketjump.mountaincore.military.model;

public enum MilitaryShift {

	DAYTIME,
	NIGHTTIME;

	public MilitaryShift toggle() {
		return switch (this) {
			case DAYTIME -> NIGHTTIME;
			case NIGHTTIME -> DAYTIME;
		};
	}

	public String getI18nKey() {
		return "MILITARY.SQUAD.SHIFT." + this.name();
	}

}
