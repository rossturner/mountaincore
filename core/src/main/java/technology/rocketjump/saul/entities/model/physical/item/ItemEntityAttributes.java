package technology.rocketjump.saul.entities.model.physical.item;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.graphics.Color;
import org.apache.commons.lang3.EnumUtils;
import technology.rocketjump.saul.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.saul.assets.entities.model.ColoringLayer;
import technology.rocketjump.saul.entities.model.physical.EntityAttributes;
import technology.rocketjump.saul.entities.model.physical.furniture.EntityDestructionCause;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.materials.model.GameMaterialType;
import technology.rocketjump.saul.persistence.EnumParser;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.rendering.utils.HexColors;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

public class ItemEntityAttributes implements EntityAttributes {

	private long seed;
	private EnumMap<GameMaterialType, GameMaterial> materials = new EnumMap<>(GameMaterialType.class);
	private EnumMap<ColoringLayer, Color> otherColors = new EnumMap<>(ColoringLayer.class); // Others such as plant_branches
	private ItemType itemType;
	private ItemPlacement itemPlacement = ItemPlacement.ON_GROUND;
	private ItemQuality itemQuality = ItemQuality.STANDARD;

	private int quantity;
	private int valuePerItem;
	private EntityDestructionCause destructionCause;

	public ItemEntityAttributes() {

	}

	public ItemEntityAttributes(long seed) {
		this.seed = seed;
	}

	@Override
	public ItemEntityAttributes clone() {
		ItemEntityAttributes cloned = new ItemEntityAttributes(seed);

		for (Map.Entry<GameMaterialType, GameMaterial> entry : this.materials.entrySet()) {
			cloned.materials.put(entry.getKey(), entry.getValue());
		}
		for (Map.Entry<ColoringLayer, Color> entry : this.otherColors.entrySet()) {
			cloned.otherColors.put(entry.getKey(), entry.getValue().cpy());
		}
		cloned.itemType = this.itemType;
		cloned.itemPlacement = this.itemPlacement;
		cloned.itemQuality = this.itemQuality;

		cloned.quantity = this.quantity;
		cloned.valuePerItem = this.valuePerItem;
		cloned.destructionCause = this.destructionCause;

		return cloned;
	}

	@Override
	public Map<GameMaterialType, GameMaterial> getMaterials() {
		return materials;
	}

	@Override
	public long getSeed() {
		return seed;
	}

	public void setSeed(long seed) {
		this.seed = seed;
	}

	@Override
	public Color getColor(ColoringLayer coloringLayer) {
		GameMaterialType materialType = coloringLayer.getLinkedMaterialType();
		if (materialType != null) {
			GameMaterial gameMaterial = materials.get(materialType);
			if (gameMaterial != null) {
				return gameMaterial.getColor();
			} else {
				return null;
			}
		} else {
			return otherColors.get(coloringLayer);
		}
	}

	public int getTotalValue() {
		return quantity * valuePerItem;
	}

	public int getValuePerItem() {
		return valuePerItem;
	}

	public void setValuePerItem(int valuePerItem) {
		this.valuePerItem = valuePerItem;
	}

	public boolean canMerge(ItemEntityAttributes other) {
		if (this.itemType.equals(other.itemType)) {
			GameMaterial primaryMaterial = this.getMaterial(this.itemType.getPrimaryMaterialType());
			GameMaterial otherPrimaryMaterial = other.getMaterial(other.itemType.getPrimaryMaterialType());
			if (primaryMaterial.equals(otherPrimaryMaterial)) {
				return this.quantity + other.quantity <= itemType.getMaxStackSize();
			} else {
				return false;
			}
		} else {
			return false;
		}
	}

	public void setColor(ColoringLayer coloringLayer, Color color) {
		if (color != null) {
			otherColors.put(coloringLayer, color);
		}
	}

	public void setMaterial(GameMaterial material) {
		if (material != null) {
			this.materials.put(material.getMaterialType(), material);
			recalculateValue();
		}
	}

	public GameMaterial getMaterial(GameMaterialType materialType) {
		return materials.get(materialType);
	}

	public void removeMaterial(GameMaterialType gameMaterialType) {
		materials.remove(gameMaterialType);
	}

	public Collection<GameMaterial> getAllMaterials() {
		return materials.values();
	}

	public ItemType getItemType() {
		return itemType;
	}

	public void setItemType(ItemType itemType) {
		this.itemType = itemType;
		recalculateValue();
	}

	public int getQuantity() {
		return quantity;
	}

	// TODO get this to be only called from one place which also manages ItemAllocations
	public void setQuantity(int quantity) {
		this.quantity = quantity;
	}

	public ItemPlacement getItemPlacement() {
		return itemPlacement;
	}

	public void setItemPlacement(ItemPlacement itemPlacement) {
		this.itemPlacement = itemPlacement;
	}

	public ItemQuality getItemQuality() {
		return itemQuality;
	}

	public void setItemQuality(ItemQuality itemQuality) {
		this.itemQuality = itemQuality;
		recalculateValue();
	}

	public boolean isDestroyed() {
		return destructionCause != null;
	}

	public void setDestroyed(EntityDestructionCause cause) {
		this.destructionCause = cause;
	}

	public EntityDestructionCause getDestructionCause() {
		return destructionCause;
	}

	private void recalculateValue() {
		if (itemType != null) {
			GameMaterial primaryMaterial = getPrimaryMaterial();
			if (primaryMaterial != null) {
				this.valuePerItem = Math.max(1, Math.round(primaryMaterial.getValueMultiplier() * itemQuality.valueMultiplier * itemType.getBaseValuePerItem()));
			}
		}
	}

	@Override
	public String toString() {
		return "ItemEntityAttributes{" +
				"itemType=" + itemType +
				", itemPlacement=" + itemPlacement +
				", itemQuality=" + itemQuality +
				", quantity=" + quantity +
				'}';
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("seed", seed);
		JSONArray materialsJson = new JSONArray();
		for (GameMaterial material : materials.values()) {
			materialsJson.add(material.getMaterialName());
		}
		asJson.put("materials", materialsJson);
		if (!otherColors.isEmpty()) {
			JSONObject otherColorsJson = new JSONObject(true);
			for (Map.Entry<ColoringLayer, Color> entry : otherColors.entrySet()) {
				otherColorsJson.put(entry.getKey().name(), HexColors.toHexString(entry.getValue()));
			}
			asJson.put("otherColors", otherColorsJson);
		}
		asJson.put("type", itemType.getItemTypeName());
		if (!itemPlacement.equals(ItemPlacement.ON_GROUND)) {
			asJson.put("placement", itemPlacement.name());
		}
		if (!itemQuality.equals(ItemQuality.STANDARD)) {
			asJson.put("quality", itemQuality.name());
		}
		if (quantity != 1) {
			asJson.put("quantity", quantity);
		}
		if (valuePerItem != 0) {
			asJson.put("valuePerItem", valuePerItem);
		}
		if (destructionCause != null) {
			asJson.put("destructionCause", this.destructionCause);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		seed = asJson.getLongValue("seed");
		JSONArray materialsJson = asJson.getJSONArray("materials");
		for (int cursor = 0; cursor < materialsJson.size(); cursor++) {
			GameMaterial material = relatedStores.gameMaterialDictionary.getByName(materialsJson.getString(cursor));
			if (material == null) {
				throw new InvalidSaveException("Could not find material by name " + materialsJson.getString(cursor));
			} else {
				materials.put(material.getMaterialType(), material);
			}
		}
		JSONObject otherColorsJson = asJson.getJSONObject("otherColors");
		if (otherColorsJson != null) {
			for (String coloringLayerName : otherColorsJson.keySet()) {
				ColoringLayer coloringLayer = EnumUtils.getEnum(ColoringLayer.class, coloringLayerName);
				if (coloringLayer == null) {
					throw new InvalidSaveException("Could not find coloring layer by name " + coloringLayerName);
				}
				Color color = HexColors.get(otherColorsJson.getString(coloringLayerName));
				otherColors.put(coloringLayer, color);
			}
		}

		itemType = relatedStores.itemTypeDictionary.getByName(asJson.getString("type"));
		if (itemType == null) {
			throw new InvalidSaveException("Could not find item type by name " + asJson.getString("type"));
		}
		itemPlacement = EnumParser.getEnumValue(asJson, "placement", ItemPlacement.class, ItemPlacement.ON_GROUND);
		itemQuality = EnumParser.getEnumValue(asJson, "quality", ItemQuality.class, ItemQuality.STANDARD);
		Integer quantity = asJson.getInteger("quantity");
		if (quantity == null) {
			this.quantity = 1;
		} else {
			this.quantity = quantity;
		}
		this.valuePerItem = asJson.getIntValue("valuePerItem");

		destructionCause = EnumParser.getEnumValue(asJson, "destructionCause", EntityDestructionCause.class, null);
	}

	public GameMaterial getPrimaryMaterial() {
		return this.getMaterial(this.getItemType().getPrimaryMaterialType());
	}
}
