package technology.rocketjump.mountaincore.assets.entities.furniture.model;

import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetType;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.misc.Name;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FurnitureEntityAssetDescriptor {

	@Name
	private String uniqueName;
	private EntityAssetType type;

	private String furnitureTypeName;
	private String furnitureLayoutName;
	private List<GameMaterialType> validMaterialTypes;

	private Map<String, List<String>> tags = new HashMap<>();

	public boolean matches(FurnitureEntityAttributes entityAttributes) {
		if (furnitureTypeName != null && !furnitureTypeName.equals(entityAttributes.getFurnitureType().getName())) {
			return false;
		}
		if (furnitureLayoutName != null && !furnitureLayoutName.equals(entityAttributes.getCurrentLayout().getUniqueName())) {
			return false;
		}
		if (validMaterialTypes != null && !validMaterialTypes.isEmpty() && validMaterialTypes.contains(entityAttributes.getPrimaryMaterialType())) {
			return false;
		}
		return true;
	}

	public String getUniqueName() {
		return uniqueName;
	}

	public void setUniqueName(String uniqueName) {
		this.uniqueName = uniqueName;
	}

	public EntityAssetType getType() {
		return type;
	}

	public void setType(EntityAssetType type) {
		this.type = type;
	}

	public String getFurnitureTypeName() {
		return furnitureTypeName;
	}

	public void setFurnitureTypeName(String furnitureTypeName) {
		this.furnitureTypeName = furnitureTypeName;
	}

	public String getFurnitureLayoutName() {
		return furnitureLayoutName;
	}

	public void setFurnitureLayoutName(String furnitureLayoutName) {
		this.furnitureLayoutName = furnitureLayoutName;
	}

	public List<GameMaterialType> getValidMaterialTypes() {
		return validMaterialTypes;
	}

	public void setValidMaterialTypes(List<GameMaterialType> validMaterialTypes) {
		this.validMaterialTypes = validMaterialTypes;
	}

	@Override
	public String toString() {
		return "FurnitureEntityAssetDescriptor{" +
				"uniqueName='" + uniqueName + '\'' +
				", type=" + type +
				", furnitureTypeName='" + furnitureTypeName + '\'' +
				", furnitureLayoutName='" + furnitureLayoutName + '\'' +
				", validMaterialTypes=" + validMaterialTypes +
				'}';
	}

	public Map<String, List<String>> getTags() {
		return tags;
	}

	public void setTags(Map<String, List<String>> tags) {
		this.tags = tags;
	}
}
