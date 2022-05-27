package technology.rocketjump.saul.entities.model.physical.plant;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.saul.assets.entities.item.model.ItemSize;
import technology.rocketjump.saul.assets.entities.item.model.ItemStyle;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.persistence.EnumParser;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.ChildPersistable;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

public class PlantSpeciesItem implements ChildPersistable {

	private String itemTypeName;
	private String materialName;
	private int quantity = 1;
	private ItemSize itemSize = ItemSize.AVERAGE;
	private ItemStyle itemStyle = ItemStyle.DEFAULT;
	private float chance = 1;

	@JsonIgnore
	private ItemType itemType;
	@JsonIgnore
	private GameMaterial material;

	public String getItemTypeName() {
		return itemTypeName;
	}

	public void setItemTypeName(String itemTypeName) {
		this.itemTypeName = itemTypeName;
	}

	public String getMaterialName() {
		return materialName;
	}

	public void setMaterialName(String materialName) {
		this.materialName = materialName;
	}

	public int getQuantity() {
		return quantity;
	}

	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public ItemSize getItemSize() {
		return itemSize;
	}

	public void setItemSize(ItemSize itemSize) {
		this.itemSize = itemSize;
	}

	public ItemStyle getItemStyle() {
		return itemStyle;
	}

	public void setItemStyle(ItemStyle itemStyle) {
		this.itemStyle = itemStyle;
	}

	public ItemType getItemType() {
		return itemType;
	}

	public void setItemType(ItemType itemType) {
		this.itemType = itemType;
	}

	public GameMaterial getMaterial() {
		return material;
	}

	public void setMaterial(GameMaterial material) {
		this.material = material;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("itemType", itemType.getItemTypeName());
		asJson.put("material", material.getMaterialName());
		asJson.put("quantity", quantity);
		if (!itemSize.equals(ItemSize.AVERAGE)) {
			asJson.put("size", itemSize.name());
		}
		if (!itemStyle.equals(ItemStyle.DEFAULT)) {
			asJson.put("style", itemStyle.name());
		}
	}
	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		itemTypeName = asJson.getString("itemType");
		itemType = relatedStores.itemTypeDictionary.getByName(itemTypeName);
		if (this.itemType == null) {
			throw new InvalidSaveException("Could not find item type by name " + itemTypeName + " in " + getClass().getSimpleName());
		}
		materialName = asJson.getString("material");
		material = relatedStores.gameMaterialDictionary.getByName(materialName);
		if (material == null) {
			throw new InvalidSaveException("Could not find material by name " + materialName + " in " + getClass().getSimpleName());
		}
		quantity = asJson.getIntValue("quantity");
		itemSize = EnumParser.getEnumValue(asJson, "size", ItemSize.class, ItemSize.AVERAGE);
		itemStyle = EnumParser.getEnumValue(asJson, "style", ItemStyle.class, ItemStyle.DEFAULT);
	}

	public float getChance() {
		return chance;
	}

	public void setChance(float chance) {
		this.chance = chance;
	}
}
