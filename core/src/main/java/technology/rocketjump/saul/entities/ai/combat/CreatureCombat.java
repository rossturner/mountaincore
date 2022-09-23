package technology.rocketjump.saul.entities.ai.combat;

import technology.rocketjump.saul.entities.components.creature.SkillsComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.combat.CombatDamageType;
import technology.rocketjump.saul.entities.model.physical.combat.DefenseInfo;
import technology.rocketjump.saul.entities.model.physical.combat.WeaponInfo;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemQuality;

/**
 * Class to represent and lookup the attack and defense of an entity in combat
 */
public class CreatureCombat {

	private Entity parentEntity;
	private WeaponInfo equippedWeapon; //TODO: refresh this if weapon no longer equipped
	private ItemEntityAttributes equippedWeaponAttributes;
	private ItemQuality equippedWeaponQuality = ItemQuality.STANDARD;

	private DefenseInfo racialDefense = DefenseInfo.NONE;
	private DefenseInfo equippedShield = DefenseInfo.NONE;
	private ItemQuality equippedShieldQuality = ItemQuality.STANDARD;
	private DefenseInfo equippedArmour;
	private ItemQuality equippedArmourQuality = ItemQuality.STANDARD;


	public CreatureCombat(Entity parentEntity) {
		if (!parentEntity.getType().equals(EntityType.CREATURE)) {
			throw new IllegalArgumentException("Creating " + getClass().getSimpleName() + " with entity of type " + parentEntity.getType());
		}
		this.parentEntity = parentEntity;

		CreatureEntityAttributes attributes = (CreatureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		DefenseInfo racialDefense = attributes.getRace().getFeatures().getDefense();
		if (racialDefense != null) {
			this.racialDefense = racialDefense;
		}

		equippedWeapon = attributes.getRace().getFeatures().getUnarmedWeapon();
		if (equippedWeapon == null) {
			equippedWeapon = WeaponInfo.UNARMED;
		}

		equippedArmour = attributes.getRace().getFeatures().getDefense();
		if (equippedArmour == null) {
			equippedArmour = DefenseInfo.NONE;
		}

		EquippedItemComponent equippedItemComponent = parentEntity.getComponent(EquippedItemComponent.class);
		if (equippedItemComponent != null) {
			Entity mainHandItem = equippedItemComponent.getMainHandItem();
			if (mainHandItem != null && mainHandItem.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes mainHandItemAttributes) {
				if (mainHandItemAttributes.getItemType().getWeaponInfo() != null) {
					equippedWeapon = mainHandItemAttributes.getItemType().getWeaponInfo();
					equippedWeaponAttributes = mainHandItemAttributes;
					equippedWeaponQuality = mainHandItemAttributes.getItemQuality();
				}
			}
			Entity offHandItem = equippedItemComponent.getOffHandItem();
			if (offHandItem != null && offHandItem.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes offHandItemAttributes) {
				if (offHandItemAttributes.getItemType().getDefenseInfo() != null) {
					equippedShield = offHandItemAttributes.getItemType().getDefenseInfo();
					equippedShieldQuality = offHandItemAttributes.getItemQuality();
				}
			}
			Entity equippedClothing = equippedItemComponent.getEquippedClothing();
			if (equippedClothing != null && equippedClothing.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes clothingAttributes) {
				if (clothingAttributes.getItemType().getDefenseInfo() != null) {
					equippedArmour = clothingAttributes.getItemType().getDefenseInfo();
					equippedArmourQuality = clothingAttributes.getItemQuality();
				}
			}
		}
	}

	public WeaponInfo getEquippedWeapon() {
		return equippedWeapon;
	}

	public int maxDefensePool() {
		int total = 0;
		int weaponSkillLevel = getSkillLevel(equippedWeapon);

		if (racialDefense.getMaxDefensePoints() > 0) {
			total += scaleForSkillLevel(racialDefense.getMaxDefensePoints(), weaponSkillLevel, ItemQuality.STANDARD);
		}
		if (equippedShield.getMaxDefensePoints() > 0) {
			total += scaleForSkillLevel(equippedShield.getMaxDefensePoints(), weaponSkillLevel, equippedShieldQuality);
		}
		if (equippedArmour.getMaxDefensePoints() > 0) {
			total += scaleForSkillLevel(equippedArmour.getMaxDefensePoints(), weaponSkillLevel, equippedArmourQuality);
		}
		return total;
	}

	public int defensePoolRegainedPerDefensiveRound() {
		int total = 0;
		int weaponSkillLevel = getSkillLevel(equippedWeapon);

		if (racialDefense.getMaxDefenseRegainedPerRound() > 0) {
			total += scaleForSkillLevel(racialDefense.getMaxDefenseRegainedPerRound(), weaponSkillLevel, ItemQuality.STANDARD);
		}
		if (equippedShield.getMaxDefenseRegainedPerRound() > 0) {
			total += scaleForSkillLevel(equippedShield.getMaxDefenseRegainedPerRound(), weaponSkillLevel, equippedShieldQuality);
		}
		if (equippedArmour.getMaxDefenseRegainedPerRound() > 0) {
			total += scaleForSkillLevel(equippedArmour.getMaxDefenseRegainedPerRound(), weaponSkillLevel, equippedArmourQuality);
		}
		return total;
	}

	private int scaleForSkillLevel(Integer maximumValue, int weaponSkillLevel, ItemQuality itemQuality) {
		return Math.max(1, Math.round((float) maximumValue * ((float) weaponSkillLevel / 100f) * itemQuality.combatMultiplier));
	}

	private int getSkillLevel(WeaponInfo equippedWeapon) {
		SkillsComponent skillsComponent = parentEntity.getComponent(SkillsComponent.class);
		if (skillsComponent != null) {
			return skillsComponent.getSkillLevel(equippedWeapon.getCombatSkill());
		} else {
			// Default to reasonable-to-low skill level
			return 30;
		}
	}

	public ItemEntityAttributes getEquippedWeaponAttributes() {
		return equippedWeaponAttributes;
	}

	public ItemQuality getEquippedWeaponQuality() {
		return equippedWeaponQuality;
	}

	public int getDamageReduction(CombatDamageType damageType) {
		int totalDamageReduction = 0;
		if (equippedShield != null) {
			totalDamageReduction += equippedShield.getDamageReduction().getOrDefault(damageType, 0);
		}
		if (equippedArmour != null) {
			totalDamageReduction += equippedArmour.getDamageReduction().getOrDefault(damageType, 0);
		}
		if (racialDefense != null) {
			totalDamageReduction += racialDefense.getDamageReduction().getOrDefault(damageType, 0);
		}
		return totalDamageReduction;
	}
}
