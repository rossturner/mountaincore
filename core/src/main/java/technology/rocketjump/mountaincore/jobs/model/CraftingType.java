package technology.rocketjump.mountaincore.jobs.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.misc.Name;
import technology.rocketjump.mountaincore.particles.model.ParticleEffectType;

import java.util.ArrayList;
import java.util.List;

public class CraftingType implements Comparable<CraftingType> {

	@Name
	private String name;
	private String i18nKey;
	private String professionRequiredName;
	private String defaultItemTypeName;
	private GameMaterialType constructsFurniture;
	@JsonIgnore
	private Skill professionRequired;
	@JsonIgnore
	private ItemType defaultItemType;
	private boolean usesWorkstationTool = false;
	private List<String> particleEffectNames;
	@JsonIgnore
	private List<ParticleEffectType> particleEffectTypes = new ArrayList<>();
	private Float mightStartFire;
	private String workOnJobAnimation;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getI18nKey() {
		return i18nKey;
	}

	public void setI18nKey(String i18nKey) {
		this.i18nKey = i18nKey;
	}

	public String getProfessionRequiredName() {
		return professionRequiredName;
	}

	public void setProfessionRequiredName(String professionRequiredName) {
		this.professionRequiredName = professionRequiredName;
	}

	public Skill getProfessionRequired() {
		return professionRequired;
	}

	public void setProfessionRequired(Skill professionRequired) {
		this.professionRequired = professionRequired;
	}

	public String getDefaultItemTypeName() {
		return defaultItemTypeName;
	}

	public void setDefaultItemTypeName(String defaultItemTypeName) {
		this.defaultItemTypeName = defaultItemTypeName;
	}

	public ItemType getDefaultItemType() {
		return defaultItemType;
	}

	public void setDefaultItemType(ItemType defaultItemType) {
		this.defaultItemType = defaultItemType;
	}

	public List<String> getParticleEffectNames() {
		return particleEffectNames;
	}

	public void setParticleEffectNames(List<String> particleEffectNames) {
		this.particleEffectNames = particleEffectNames;
	}

	public List<ParticleEffectType> getParticleEffectTypes() {
		return particleEffectTypes;
	}

	public void setParticleEffectTypes(List<ParticleEffectType> particleEffectTypes) {
		this.particleEffectTypes = particleEffectTypes;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CraftingType that = (CraftingType) o;
		return name != null ? name.equals(that.name) : that.name == null;
	}

	@Override
	public int hashCode() {
		return name != null ? name.hashCode() : 0;
	}

	@Override
	public String toString() {
		return name;
	}

	public GameMaterialType getConstructsFurniture() {
		return constructsFurniture;
	}

	public void setConstructsFurniture(GameMaterialType constructsFurniture) {
		this.constructsFurniture = constructsFurniture;
	}

	public boolean isUsesWorkstationTool() {
		return usesWorkstationTool;
	}

	public void setUsesWorkstationTool(boolean usesWorkstationTool) {
		this.usesWorkstationTool = usesWorkstationTool;
	}

	@Override
	public int compareTo(CraftingType o) {
		return this.name.compareTo(o.name);
	}

	public Float getMightStartFire() {
		return mightStartFire;
	}

	public void setMightStartFire(Float mightStartFire) {
		this.mightStartFire = mightStartFire;
	}

	public String getWorkOnJobAnimation() {
		return workOnJobAnimation;
	}

	public void setWorkOnJobAnimation(String workOnJobAnimation) {
		this.workOnJobAnimation = workOnJobAnimation;
	}
}
