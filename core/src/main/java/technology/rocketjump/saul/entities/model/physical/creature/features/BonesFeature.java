package technology.rocketjump.saul.entities.model.physical.creature.features;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.saul.materials.model.GameMaterial;

@JsonIgnoreProperties(ignoreUnknown = true)
public class BonesFeature {

	private String materialName;
	@JsonIgnore
	private GameMaterial material;

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
