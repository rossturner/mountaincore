package technology.rocketjump.mountaincore.crafting.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.mountaincore.jobs.model.CraftingType;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.misc.Name;

import java.util.List;

public class CraftingRecipe {

	@Name
	private String recipeName;
	private String verbOverrideI18nKey;

	private String craftingTypeName;
	@JsonIgnore
	private CraftingType craftingType;

	private String itemTypeRequiredName;
	@JsonIgnore
	private ItemType itemTypeRequired;

	private List<QuantifiedItemTypeWithMaterial> input;
	private QuantifiedItemTypeWithMaterial output;

	private CraftingRecipeValueConversion valueConversion = CraftingRecipeValueConversion.DEFAULT;

	private List<GameMaterialType> materialTypesToCopyOver;
	private Double extraGameHoursToComplete; // Only used in automated conversion process (for now)
	private Float minimumTimeToCompleteCrafting;
	private Float maximumTimeToCompleteCrafting;

	public String getRecipeName() {
		return recipeName;
	}

	public void setRecipeName(String name) {
		this.recipeName = name;
	}

	public String getVerbOverrideI18nKey() {
		return verbOverrideI18nKey;
	}

	public void setVerbOverrideI18nKey(String verbOverrideI18nKey) {
		this.verbOverrideI18nKey = verbOverrideI18nKey;
	}

	public String getCraftingTypeName() {
		return craftingTypeName;
	}

	public void setCraftingTypeName(String craftingTypeName) {
		this.craftingTypeName = craftingTypeName;
	}

	public CraftingType getCraftingType() {
		return craftingType;
	}

	public void setCraftingType(CraftingType craftingType) {
		this.craftingType = craftingType;
	}

	public List<QuantifiedItemTypeWithMaterial> getInput() {
		return input;
	}

	public void setInput(List<QuantifiedItemTypeWithMaterial> input) {
		this.input = input;
	}

	public QuantifiedItemTypeWithMaterial getOutput() {
		return output;
	}

	public void setOutput(QuantifiedItemTypeWithMaterial output) {
		this.output = output;
	}

	public List<GameMaterialType> getMaterialTypesToCopyOver() {
		return materialTypesToCopyOver;
	}

	public void setMaterialTypesToCopyOver(List<GameMaterialType> materialTypesToCopyOver) {
		this.materialTypesToCopyOver = materialTypesToCopyOver;
	}

	public String getItemTypeRequiredName() {
		return itemTypeRequiredName;
	}

	public void setItemTypeRequiredName(String itemTypeRequiredName) {
		this.itemTypeRequiredName = itemTypeRequiredName;
	}

	public ItemType getItemTypeRequired() {
		return itemTypeRequired;
	}

	public void setItemTypeRequired(ItemType itemTypeRequired) {
		this.itemTypeRequired = itemTypeRequired;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		CraftingRecipe that = (CraftingRecipe) o;
		return recipeName.equals(that.recipeName);
	}

	@Override
	public int hashCode() {
		return recipeName.hashCode();
	}

	public Double getExtraGameHoursToComplete() {
		return extraGameHoursToComplete;
	}

	public void setExtraGameHoursToComplete(Double extraGameHoursToComplete) {
		this.extraGameHoursToComplete = extraGameHoursToComplete;
	}

	public Float getMinimumTimeToCompleteCrafting() {
		return minimumTimeToCompleteCrafting;
	}

	public void setMinimumTimeToCompleteCrafting(Float minimumTimeToCompleteCrafting) {
		this.minimumTimeToCompleteCrafting = minimumTimeToCompleteCrafting;
	}

	public Float getMaximumTimeToCompleteCrafting() {
		return maximumTimeToCompleteCrafting;
	}

	public void setMaximumTimeToCompleteCrafting(Float maximumTimeToCompleteCrafting) {
		this.maximumTimeToCompleteCrafting = maximumTimeToCompleteCrafting;
	}

	public float getTimeToCompleteCrafting(int skillLevelForProfession) {
		float timeMultiplier = ((100f - (float)skillLevelForProfession) / 100f);
		float variableTime = getMaximumTimeToCompleteCrafting() - getMinimumTimeToCompleteCrafting();
		return getMinimumTimeToCompleteCrafting() + (timeMultiplier * variableTime);
	}

	public CraftingRecipeValueConversion getValueConversion() {
		return valueConversion;
	}

	public void setValueConversion(CraftingRecipeValueConversion valueConversion) {
		this.valueConversion = valueConversion;
	}

	@Override
	public String toString() {
		return recipeName;
	}
}
