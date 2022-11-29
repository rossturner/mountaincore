package technology.rocketjump.saul.entities.ai.goap;

public enum EntityNeed {

	FOOD,
	DRINK,
	SLEEP;

	public String getI18nKey() {
		return "NEEDS." + name();
	}

	public String iconName() {
		return "icon_" + name().toLowerCase();
	}
}
