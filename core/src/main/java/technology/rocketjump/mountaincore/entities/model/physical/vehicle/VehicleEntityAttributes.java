package technology.rocketjump.mountaincore.entities.model.physical.vehicle;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.graphics.Color;
import org.apache.commons.lang3.EnumUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.assets.entities.model.ColoringLayer;
import technology.rocketjump.mountaincore.entities.model.physical.EntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.EntityDestructionCause;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.persistence.EnumParser;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rendering.utils.HexColors;

import java.util.EnumMap;
import java.util.Map;

public class VehicleEntityAttributes implements EntityAttributes {

	protected long seed;
	protected EnumMap<GameMaterialType, GameMaterial> materials = new EnumMap<>(GameMaterialType.class);
	private EnumMap<ColoringLayer, Color> otherColors = new EnumMap<>(ColoringLayer.class);

	private VehicleType vehicleType;

	private Long assignedToEntityId;
	private int damageAmount;
	private EntityDestructionCause destructionCause;

	public VehicleEntityAttributes() {

	}

	public VehicleEntityAttributes(long seed) {
		this.seed = seed;
	}

	@Override
	public long getSeed() {
		return seed;
	}

	public void setSeed(long seed) {
		this.seed = seed;
	}

	@Override
	public EntityAttributes clone() {
		VehicleEntityAttributes cloned = new VehicleEntityAttributes(seed);
		cloned.vehicleType = this.vehicleType;
		for (Map.Entry<GameMaterialType, GameMaterial> entry : materials.entrySet()) {
			cloned.materials.put(entry.getKey(), entry.getValue());
		}
		for (Map.Entry<ColoringLayer, Color> entry : this.otherColors.entrySet()) {
			cloned.otherColors.put(entry.getKey(), entry.getValue().cpy());
		}
		cloned.assignedToEntityId = this.assignedToEntityId;
		cloned.destructionCause = this.destructionCause;
		return cloned;
	}

	@Override
	public Color getColor(ColoringLayer coloringLayer) {
		GameMaterialType materialType = coloringLayer.getLinkedMaterialType();
		if (otherColors.containsKey(coloringLayer)) {
			return otherColors.get(coloringLayer);
		} else {
			GameMaterial gameMaterial = materials.get(materialType);
			if (gameMaterial != null) {
				return gameMaterial.getColor();
			} else {
				return null;
			}
		}
	}

	public EnumMap<ColoringLayer, Color> getOtherColors() {
		return otherColors;
	}

	public VehicleType getVehicleType() {
		return vehicleType;
	}

	public void setVehicleType(VehicleType vehicleType) {
		this.vehicleType = vehicleType;
	}

	public void setColor(ColoringLayer coloringLayer, Color color) {
		if (color != null && coloringLayer != null) {
			otherColors.put(coloringLayer, color);
		}
	}

	public EnumMap<GameMaterialType, GameMaterial> getMaterials() {
		return materials;
	}

	public void setMaterials(EnumMap<GameMaterialType, GameMaterial> materials) {
		this.materials = materials;
	}

	public void setMaterial(GameMaterial material) {
		setMaterial(material, true);
	}

	public void setMaterial(GameMaterial material, boolean overrideExisting) {
		if (material != null) {
			if (materials.containsKey(material.getMaterialType()) && !overrideExisting) {
				return;
			}
			this.materials.put(material.getMaterialType(), material);
		}
	}

	public void removeMaterial(GameMaterialType gameMaterialType) {
		materials.remove(gameMaterialType);
	}


	@Override
	public String toString() {
		return "VehicleTypeEntityAttributes{" +
				"VehicleType=" + vehicleType + "}";
	}

	public Long getAssignedToEntityId() {
		return assignedToEntityId;
	}

	public void setAssignedToEntityId(Long assignedToEntityId) {
		if (this.assignedToEntityId != null && assignedToEntityId != null && !this.assignedToEntityId.equals(assignedToEntityId)) {
			Logger.error("Vehicle already assigned to " + this.assignedToEntityId + " but trying to assign to " + assignedToEntityId);
		}
		this.assignedToEntityId = assignedToEntityId;
	}

	public boolean isDestroyed() {
		return destructionCause != null;
	}

	public int getDamageAmount() {
		return damageAmount;
	}

	public void setDamageAmount(int damageAmount) {
		this.damageAmount = damageAmount;
	}

	public void setDestroyed(EntityDestructionCause cause) {
		this.destructionCause = cause;
	}

	public EntityDestructionCause getDestructionCause() {
		return destructionCause;
	}

	public GameMaterial getPrimaryMaterial() {
		return getMaterials().getOrDefault(vehicleType.getMaterialType(), GameMaterial.NULL_MATERIAL);
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("seed", seed);
		JSONObject materialsJson = new JSONObject();
		for (GameMaterial material : materials.values()) {
			materialsJson.put(material.getMaterialType().name(), material.getMaterialName());
		}
		asJson.put("materials", materialsJson);
		if (!otherColors.isEmpty()) {
			JSONObject otherColorsJson = new JSONObject(true);
			for (Map.Entry<ColoringLayer, Color> entry : otherColors.entrySet()) {
				otherColorsJson.put(entry.getKey().name(), HexColors.toHexString(entry.getValue()));
			}
			asJson.put("otherColors", otherColorsJson);
		}
		if (vehicleType != null) {
			asJson.put("vehicleType", vehicleType.getName());
		}
		if (assignedToEntityId != null) {
			asJson.put("assignedToEntityId", assignedToEntityId);
		}
		if (damageAmount > 0) {
			asJson.put("damage", damageAmount);
		}
		if (destructionCause != null) {
			asJson.put("destructionCause", this.destructionCause);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		seed = asJson.getLongValue("seed");
		if (asJson.get("materials") instanceof JSONArray) {
			throw new InvalidSaveException("Old save file structure");
		}
		JSONObject materialsJson = asJson.getJSONObject("materials");
		for (Map.Entry<String, Object> entry : materialsJson.entrySet()) {
			GameMaterial material = relatedStores.gameMaterialDictionary.getByName(entry.getValue().toString());
			if (material == null) {
				throw new InvalidSaveException("Could not find material by name " + entry.getValue().toString());
			} else {
				materials.put(GameMaterialType.valueOf(entry.getKey()), material);
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

		String vehicleTypeName = asJson.getString("vehicleType");
		if (vehicleTypeName != null) {
			vehicleType = relatedStores.vehicleTypeDictionary.getByName(vehicleTypeName);
			if (vehicleType == null) {
				throw new InvalidSaveException("Could not find vehicle type by name " + vehicleTypeName);
			}
		}
		assignedToEntityId = asJson.getLong("assignedToEntityId");

		damageAmount = asJson.getIntValue("damage");
		destructionCause = EnumParser.getEnumValue(asJson, "destructionCause", EntityDestructionCause.class, null);
	}
}
