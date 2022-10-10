package technology.rocketjump.saul.invasions.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InvasionEquipmentDescriptor {

	private boolean none; // Used to indicate this equipment slot is empty i.e. no armour or shield
	private String itemTypeName;
	@JsonIgnore
	private ItemType itemType;
	private int standardPointsCost;

	public boolean isNone() {
		return none;
	}

	public void setNone(boolean none) {
		this.none = none;
	}

	public String getItemTypeName() {
		return itemTypeName;
	}

	public void setItemTypeName(String itemTypeName) {
		this.itemTypeName = itemTypeName;
	}

	public ItemType getItemType() {
		return itemType;
	}

	public void setItemType(ItemType itemType) {
		this.itemType = itemType;
	}

	public int getStandardPointsCost() {
		return standardPointsCost;
	}

	public void setStandardPointsCost(int standardPointsCost) {
		this.standardPointsCost = standardPointsCost;
	}
}
