package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;

public class ChangeWeaponSelectionMessage {

	public final Entity entity;
	public final ItemType selectedWeaponType;

	public ChangeWeaponSelectionMessage(Entity entity, ItemType selectedWeaponType) {
		this.entity = entity;
		this.selectedWeaponType = selectedWeaponType;
	}
}
