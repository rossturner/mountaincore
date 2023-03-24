package technology.rocketjump.mountaincore.entities.model.physical.creature.features;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SkinFeature {

	private String itemTypeName;
	@JsonIgnore
	private ItemType itemType;
	private int quantity;
	private String materialName;
	@JsonIgnore
	private GameMaterial material;

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
		if (itemType == null) {
			itemTypeName = null;
		} else {
			itemTypeName = itemType.getItemTypeName();
		}
		this.itemType = itemType;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
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
		if (GameMaterial.NULL_MATERIAL.equals(material) || material == null) {
			this.materialName = null;
		} else {
			this.materialName = material.getMaterialName();
		}
		this.material = material;
	}

}
