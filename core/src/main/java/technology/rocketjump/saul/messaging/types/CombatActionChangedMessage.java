package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.ai.combat.CombatAction;
import technology.rocketjump.saul.entities.model.Entity;

public class CombatActionChangedMessage {

	public final Entity entity;
	public final CombatAction previousAction;
	public final CombatAction newAction;

	public CombatActionChangedMessage(Entity entity, CombatAction previousAction, CombatAction newAction) {
		this.entity = entity;
		this.previousAction = previousAction;
		this.newAction = newAction;
	}
}
