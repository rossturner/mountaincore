package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;

public class CombatAttackMessage {

	public final Entity attackerEntity;
	public final Entity defenderEntity;
	public final ItemType weaponItemType;
	public final ItemEntityAttributes ammoAttributes;

	public CombatAttackMessage(Entity attackerEntity, Entity defenderEntity) {
		this.attackerEntity = attackerEntity;
		this.defenderEntity = defenderEntity;
		this.weaponItemType = null;
		this.ammoAttributes = null;
	}

	@Deprecated // TODO delete this
	public CombatAttackMessage(Entity attackerEntity, Entity defenderEntity, ItemType weaponItemType, ItemEntityAttributes ammoAttributes) {
		this.attackerEntity = attackerEntity;
		this.defenderEntity = defenderEntity;
		this.weaponItemType = weaponItemType;
		this.ammoAttributes = ammoAttributes;
	}
}
