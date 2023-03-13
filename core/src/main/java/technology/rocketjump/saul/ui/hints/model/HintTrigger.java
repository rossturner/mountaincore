package technology.rocketjump.saul.ui.hints.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Used to specify when a hint should be displayed
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HintTrigger {

	private HintTriggerType triggerType;
	private String relatedTypeName;
	private int quantity;

	public HintTriggerType getTriggerType() {
		return triggerType;
	}

	public void setTriggerType(HintTriggerType triggerType) {
		this.triggerType = triggerType;
	}

	public String getRelatedTypeName() {
		return relatedTypeName;
	}

	public void setRelatedTypeName(String relatedTypeName) {
		this.relatedTypeName = relatedTypeName;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public enum HintTriggerType {

		ON_START_WITH_TUTORIAL,

		ON_GAME_START,
		ON_SETTLEMENT_SPAWNED,
		ITEM_AMOUNT,
		GUI_SWITCH_VIEW,
		ONGOING_EFFECT_TRIGGERED

	}

}
