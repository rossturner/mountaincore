package technology.rocketjump.saul.entities.ai.combat;

import org.apache.commons.lang3.NotImplementedException;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.combat.DefenseInfo;
import technology.rocketjump.saul.entities.model.physical.combat.WeaponInfo;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemQuality;

/**
 * Class to represent and lookup the attack and defense of an entity in combat
 */
public class CreatureCombatStats {

	private WeaponInfo equippedWeapon;
	private ItemQuality equippedWeaponQuality = ItemQuality.STANDARD;

	private DefenseInfo racialDefense = DefenseInfo.NONE;
	private DefenseInfo equippedShield = DefenseInfo.NONE;
	private ItemQuality equippedShieldQuality = ItemQuality.STANDARD;
	private DefenseInfo equippedArmour = DefenseInfo.NONE;
	private ItemQuality equippedArmourQuality = ItemQuality.STANDARD;


	public CreatureCombatStats(Entity parentEntity) {
		if (!parentEntity.getType().equals(EntityType.CREATURE)) {
			throw new IllegalArgumentException("Creating " + getClass().getSimpleName() + " with entity of type " + parentEntity.getType());
		}

		CreatureEntityAttributes attributes = (CreatureEntityAttributes)parentEntity.getPhysicalEntityComponent().getAttributes();
		equippedWeapon = attributes.getRace().getFeatures().getUnarmedWeapon();
		if (equippedWeapon == null) {
			equippedWeapon = WeaponInfo.UNARMED;
		}

		EquippedItemComponent equippedItemComponent = parentEntity.getComponent(EquippedItemComponent.class);
		if (equippedItemComponent != null && equippedItemComponent.getMainHandItem() != null) {
			Entity mainHandItem = equippedItemComponent.getMainHandItem();
			if (mainHandItem.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes mainHandItemAttributes) {
				if (mainHandItemAttributes.getItemType().getWeaponInfo() != null) {
					equippedWeapon = mainHandItemAttributes.getItemType().getWeaponInfo();
					equippedWeaponQuality = mainHandItemAttributes.getItemQuality();
				}
			}
		}
	}

	public int getWeaponRangeAsInt() {
		return (int)Math.max(1, equippedWeapon.getRange());
	}

	public int maxDefensePool() {
		throw new NotImplementedException("Add up defensive stats");
	}

	public int defensePoolRegainedPerDefensiveRound() {
		throw new NotImplementedException("Add up from defensive stats");
	}
}
