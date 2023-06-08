package technology.rocketjump.mountaincore.constants;

import com.badlogic.gdx.graphics.Color;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.mountaincore.rendering.utils.HexColors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WorldConstants {

	private String backgroundColor;
	@JsonIgnore
	private Color backgroundColorInstance;

	private int maxNeighbouringShrubs;
	private float attachedLightSourceTogglePoint;
	private double corpseDecayHours;
	private String corpseDecayColor;
	@JsonIgnore
	private Color corpseDecayColorInstance;

	private String stoneHarvestedItemType;
	private float stoneHarvestMinSkillChance;
	private float stoneHarvestMaxSkillChance;
	private String oreHarvestedItemType;
	private float oreHarvestMinSkillChance;
	private float oreHarvestMaxSkillChance;
	private String gemHarvestedItemType;
	private int roofSupportMaxDistance;

	public String getBackgroundColor() {
		return backgroundColor;
	}

	public Color getBackgroundColorInstance() {
		return backgroundColorInstance;
	}

	public void setBackgroundColor(String backgroundColor) {
		this.backgroundColor = backgroundColor;
		this.backgroundColorInstance = HexColors.get(backgroundColor);
	}

	public String getStoneHarvestedItemType() {
		return stoneHarvestedItemType;
	}

	public void setStoneHarvestedItemType(String stoneHarvestedItemType) {
		this.stoneHarvestedItemType = stoneHarvestedItemType;
	}

	public String getOreHarvestedItemType() {
		return oreHarvestedItemType;
	}

	public void setOreHarvestedItemType(String oreHarvestedItemType) {
		this.oreHarvestedItemType = oreHarvestedItemType;
	}

	public String getGemHarvestedItemType() {
		return gemHarvestedItemType;
	}

	public void setGemHarvestedItemType(String gemHarvestedItemType) {
		this.gemHarvestedItemType = gemHarvestedItemType;
	}

	public int getMaxNeighbouringShrubs() {
		return maxNeighbouringShrubs;
	}

	public void setMaxNeighbouringShrubs(int maxNeighbouringShrubs) {
		this.maxNeighbouringShrubs = maxNeighbouringShrubs;
	}

	public float getAttachedLightSourceTogglePoint() {
		return attachedLightSourceTogglePoint;
	}

	public void setAttachedLightSourceTogglePoint(float attachedLightSourceTogglePoint) {
		this.attachedLightSourceTogglePoint = attachedLightSourceTogglePoint;
	}

	public double getCorpseDecayHours() {
		return corpseDecayHours;
	}

	public void setCorpseDecayHours(double corpseDecayHours) {
		this.corpseDecayHours = corpseDecayHours;
	}

	public String getCorpseDecayColor() {
		return corpseDecayColor;
	}

	public void setCorpseDecayColor(String corpseDecayColor) {
		this.corpseDecayColor = corpseDecayColor;
		this.corpseDecayColorInstance = HexColors.get(corpseDecayColor);
	}

	public Color getCorpseDecayColorInstance() {
		return corpseDecayColorInstance;
	}

	public int getRoofSupportMaxDistance() {
		return roofSupportMaxDistance;
	}

	public void setRoofSupportMaxDistance(int roofSupportMaxDistance) {
		this.roofSupportMaxDistance = roofSupportMaxDistance;
	}

	public float getStoneHarvestMinSkillChance() {
		return stoneHarvestMinSkillChance;
	}

	public void setStoneHarvestMinSkillChance(float stoneHarvestMinSkillChance) {
		this.stoneHarvestMinSkillChance = stoneHarvestMinSkillChance;
	}

	public float getStoneHarvestMaxSkillChance() {
		return stoneHarvestMaxSkillChance;
	}

	public void setStoneHarvestMaxSkillChance(float stoneHarvestMaxSkillChance) {
		this.stoneHarvestMaxSkillChance = stoneHarvestMaxSkillChance;
	}

	public float getOreHarvestMinSkillChance() {
		return oreHarvestMinSkillChance;
	}

	public void setOreHarvestMinSkillChance(float oreHarvestMinSkillChance) {
		this.oreHarvestMinSkillChance = oreHarvestMinSkillChance;
	}

	public float getOreHarvestMaxSkillChance() {
		return oreHarvestMaxSkillChance;
	}

	public void setOreHarvestMaxSkillChance(float oreHarvestMaxSkillChance) {
		this.oreHarvestMaxSkillChance = oreHarvestMaxSkillChance;
	}
}
