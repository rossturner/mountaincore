package technology.rocketjump.mountaincore.entities.model.physical.creature.features;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.mountaincore.entities.model.physical.combat.DefenseInfo;
import technology.rocketjump.mountaincore.entities.model.physical.combat.WeaponInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RaceFeatures {

	private DefenseInfo defense;
	private WeaponInfo unarmedWeapon;
	private SkinFeature skin;
	private MeatFeature meat;
	private BonesFeature bones;
	private BloodFeature blood;

	public DefenseInfo getDefense() {
		return defense;
	}

	public void setDefense(DefenseInfo defense) {
		this.defense = defense;
	}

	public WeaponInfo getUnarmedWeapon() {
		return unarmedWeapon;
	}

	public void setUnarmedWeapon(WeaponInfo unarmedWeapon) {
		this.unarmedWeapon = unarmedWeapon;
	}

	public SkinFeature getSkin() {
		return skin;
	}

	public void setSkin(SkinFeature skin) {
		this.skin = skin;
	}

	public BonesFeature getBones() {
		return bones;
	}

	public void setBones(BonesFeature bones) {
		this.bones = bones;
	}

	public BloodFeature getBlood() {
		return blood;
	}

	public void setBlood(BloodFeature blood) {
		this.blood = blood;
	}

	public MeatFeature getMeat() {
		return meat;
	}

	public void setMeat(MeatFeature meat) {
		this.meat = meat;
	}
}
