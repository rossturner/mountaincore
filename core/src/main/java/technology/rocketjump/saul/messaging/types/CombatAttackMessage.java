package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.combat.model.WeaponAttack;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;

public class CombatAttackMessage {

	public final Entity attackerEntity;
	public final Entity defenderEntity;

	public final WeaponAttack weaponAttack;
	public final ItemEntityAttributes ammoAttributes;

	public CombatAttackMessage(Entity attackerEntity, Entity defenderEntity, WeaponAttack weaponAttack, ItemEntityAttributes ammoAttributes) {
		this.attackerEntity = attackerEntity;
		this.defenderEntity = defenderEntity;
		this.weaponAttack = weaponAttack;
		this.ammoAttributes = ammoAttributes;
	}

}
