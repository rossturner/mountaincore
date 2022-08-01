package technology.rocketjump.saul.entities.model.physical.creature.features;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.saul.entities.model.physical.combat.CombatDamageType;
import technology.rocketjump.saul.materials.model.GameMaterial;

import java.util.EnumMap;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SkinFeature {

	private String materialName;
	@JsonIgnore
	private GameMaterial material;
	private Map<CombatDamageType, Integer> damageReduction = new EnumMap<>(CombatDamageType.class);

	public Map<CombatDamageType, Integer> getDamageReduction() {
		return damageReduction;
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

	public void setDamageReduction(Map<CombatDamageType, Integer> damageReduction) {
		this.damageReduction = damageReduction;
	}
}
