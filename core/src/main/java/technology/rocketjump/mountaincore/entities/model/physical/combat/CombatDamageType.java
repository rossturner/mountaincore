package technology.rocketjump.mountaincore.entities.model.physical.combat;

import technology.rocketjump.mountaincore.materials.model.MaterialHardness;
import technology.rocketjump.mountaincore.materials.model.MaterialWeight;

public enum CombatDamageType {

	STABBING,
	SLASHING,
	CRUSHING;

	public float weaponDamageScalar(MaterialHardness hardness, MaterialWeight weight) {
		return switch (this) {
			case STABBING -> (1f / weight.majorModifier) * hardness.majorModifier;
			case SLASHING -> hardness.majorModifier * weight.minorModifier;
			case CRUSHING -> weight.majorModifier * hardness.minorModifier;
		};
	}

}
