package technology.rocketjump.saul.constants;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.base.MoreObjects;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.materials.model.GameMaterial;

public class CurrencyDefinition {

	private String itemTypeName;
	@JsonIgnore
	private ItemType itemType;
	private String materialName;
	@JsonIgnore
	private GameMaterial material;
	private int value;

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

	public String getMaterialName() {
		return materialName;
	}

	public void setMaterialName(String materialName) {
		this.materialName = materialName;
	}

	public GameMaterial getMaterial() {
		return material;
	}

	public void setMaterial(GameMaterial material) {
		this.material = material;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("itemType", itemType)
				.add("material", material)
				.add("value", value)
				.toString();
	}
}
