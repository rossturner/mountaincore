package technology.rocketjump.saul.invasions.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.entities.model.physical.item.ItemQuality;
import technology.rocketjump.saul.entities.model.physical.item.QuantifiedItemTypeWithMaterial;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InvasionParticipant {

	private String raceName;
	@JsonIgnore
	private Race race;
	private int basePointsCost;
	private Map<ItemQuality, Float> itemQualities;
	private List<QuantifiedItemTypeWithMaterial> fixedInventory;
	private InvasionParticipantEquipment equipmentOptions;

	public String getRaceName() {
		return raceName;
	}

	public void setRaceName(String raceName) {
		this.raceName = raceName;
	}

	public Race getRace() {
		return race;
	}

	public void setRace(Race race) {
		this.race = race;
	}

	public int getBasePointsCost() {
		return basePointsCost;
	}

	public void setBasePointsCost(int basePointsCost) {
		this.basePointsCost = basePointsCost;
	}

	public Map<ItemQuality, Float> getItemQualities() {
		return itemQualities;
	}

	public void setItemQualities(Map<ItemQuality, Float> itemQualities) {
		this.itemQualities = itemQualities;
	}

	public List<QuantifiedItemTypeWithMaterial> getFixedInventory() {
		return fixedInventory;
	}

	public void setFixedInventory(List<QuantifiedItemTypeWithMaterial> fixedInventory) {
		this.fixedInventory = fixedInventory;
	}

	public InvasionParticipantEquipment getEquipmentOptions() {
		return equipmentOptions;
	}

	public void setEquipmentOptions(InvasionParticipantEquipment equipmentOptions) {
		this.equipmentOptions = equipmentOptions;
	}
}
