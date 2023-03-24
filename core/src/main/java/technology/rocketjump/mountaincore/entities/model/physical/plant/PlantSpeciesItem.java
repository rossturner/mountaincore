package technology.rocketjump.mountaincore.entities.model.physical.plant;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.ChildPersistable;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

public class PlantSpeciesItem implements ChildPersistable {

	private String itemTypeName;
	private String materialName;
	private int quantity = 1;
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
	}

	public float getChance() {
		return chance;
	}

	public void setChance(float chance) {
		this.chance = chance;
	}
}
