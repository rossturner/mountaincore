package technology.rocketjump.mountaincore.ui.hints.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class HintProgressDescriptor {

	private ProgressDescriptorTargetType type;
	private String targetTypeName;
	private int quantityRequired;

	private String i18nKey;
	private boolean displayAsCheckbox;

	public enum ProgressDescriptorTargetType {

		CAMERA_MOVED,
		CAMERA_ZOOMED,
		MINIMAP_CLICKED,
		GAME_PAUSED,
		NORMAL_SPEED_SELECTED,
		FAST_SPEED_SELECTED,
		DEFAULT_VIEW_MODE,
		OTHER_VIEW_MODE,
		SETTLER_MANAGEMENT,
		RESOURCE_MANAGEMENT,
		GAME_SAVED,


		ROOMS,
		ROOM_TILES,
		STOCKPILE_TILES,
		FARM_PLOT_SELECTIONS,
		PROFESSIONS_ASSIGNED,
		FURNITURE_CONSTRUCTED,
		ITEM_EXISTS

	}

	public ProgressDescriptorTargetType getType() {
		return type;
	}

	public void setType(ProgressDescriptorTargetType type) {
		this.type = type;
	}

	public String getTargetTypeName() {
		return targetTypeName;
	}

	public void setTargetTypeName(String targetTypeName) {
		this.targetTypeName = targetTypeName;
	}

	public int getQuantityRequired() {
		return quantityRequired;
	}

	public void setQuantityRequired(int quantityRequired) {
		this.quantityRequired = quantityRequired;
	}

	public String getI18nKey() {
		return i18nKey;
	}

	public void setI18nKey(String i18nKey) {
		this.i18nKey = i18nKey;
	}

	public boolean isDisplayAsCheckbox() {
		return displayAsCheckbox;
	}

	public void setDisplayAsCheckbox(boolean displayAsCheckbox) {
		this.displayAsCheckbox = displayAsCheckbox;
	}

	@Override
	public String toString() {
		return "HintProgressDescriptor{" +
				"type=" + type +
				", targetTypeName='" + targetTypeName + '\'' +
				", quantityRequired=" + quantityRequired +
				'}';
	}
}
