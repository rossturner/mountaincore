package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.DeathReason;
import technology.rocketjump.mountaincore.entities.model.physical.creature.status.StatusEffect;

public class StatusMessage {

	public final Entity entity;
	public final Class<? extends StatusEffect> statusClass;
	public final DeathReason deathReason;
	public final Entity inflictedBy; // This should only be used for other entities which have inflicted something which can lead to death, this will cause them to show as the killer

	public StatusMessage(Entity entity, Class<? extends StatusEffect> statusClass, DeathReason deathReason, Entity inflictedBy) {
		this.entity = entity;
		this.statusClass = statusClass;
		this.deathReason = deathReason;
		this.inflictedBy = inflictedBy;
	}

}
