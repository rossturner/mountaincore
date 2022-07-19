package technology.rocketjump.saul.crafting.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.saul.entities.model.physical.item.ItemQuality;
import technology.rocketjump.saul.misc.Name;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CraftingOutputQuality {

	@Name
	private String name;
	private int minSkillLevel;
	private int maxSkillLevel;
	private Map<ItemQuality, Float> outputQuality;

	public int getMinSkillLevel() {
		return minSkillLevel;
	}

	public void setMinSkillLevel(int minSkillLevel) {
		this.minSkillLevel = minSkillLevel;
	}

	public int getMaxSkillLevel() {
		return maxSkillLevel;
	}

	public void setMaxSkillLevel(int maxSkillLevel) {
		this.maxSkillLevel = maxSkillLevel;
	}

	public Map<ItemQuality, Float> getOutputQuality() {
		return outputQuality;
	}

	public void setOutputQuality(Map<ItemQuality, Float> outputQuality) {
		this.outputQuality = outputQuality;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
