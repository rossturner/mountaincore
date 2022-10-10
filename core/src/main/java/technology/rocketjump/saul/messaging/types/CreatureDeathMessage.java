package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.DeathReason;

public class CreatureDeathMessage {

	public final Entity deceased;
	public final DeathReason reason;
	public final Entity killer;

	public CreatureDeathMessage(Entity deceased, DeathReason reason, Entity killer) {
		this.deceased = deceased;
		this.killer = killer;
		if (reason == null) {
			this.reason = DeathReason.UNKNOWN;
		} else {
			this.reason = reason;
		}
	}

}
