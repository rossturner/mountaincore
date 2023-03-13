package technology.rocketjump.saul.ui.hints.model;

public class HintOnShowEvent {

	private HintOnShowEventType type;
	private String value;

	public HintOnShowEventType getType() {
		return type;
	}

	public void setType(HintOnShowEventType type) {
		this.type = type;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public enum HintOnShowEventType {

		HIDE_ALL_GUI_AREAS,
		SHOW_GUI_AREA

	}
}
