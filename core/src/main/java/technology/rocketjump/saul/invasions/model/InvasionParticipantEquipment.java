package technology.rocketjump.saul.invasions.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InvasionParticipantEquipment {

	private List<InvasionEquipmentDescriptor> weapons;
	private List<InvasionEquipmentDescriptor> shield;
	private List<InvasionEquipmentDescriptor> armor;

	public List<InvasionEquipmentDescriptor> getWeapons() {
		return weapons;
	}

	public void setWeapons(List<InvasionEquipmentDescriptor> weapons) {
		this.weapons = weapons;
	}

	public List<InvasionEquipmentDescriptor> getShield() {
		return shield;
	}

	public void setShield(List<InvasionEquipmentDescriptor> shield) {
		this.shield = shield;
	}

	public List<InvasionEquipmentDescriptor> getArmor() {
		return armor;
	}

	public void setArmor(List<InvasionEquipmentDescriptor> armor) {
		this.armor = armor;
	}
}
