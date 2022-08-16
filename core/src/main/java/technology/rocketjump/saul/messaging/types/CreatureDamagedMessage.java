package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.body.BodyPart;
import technology.rocketjump.saul.entities.model.physical.creature.body.BodyPartDamageLevel;

public class CreatureDamagedMessage {

	public final Entity targetCreature;
	public final Entity aggressorCreature;
	public final BodyPart impactedBodyPart;
	public final BodyPartDamageLevel damageLevel;

	public CreatureDamagedMessage(Entity targetCreature, Entity aggressorCreature, BodyPart impactedBodyPart, BodyPartDamageLevel damageLevel) {
		this.targetCreature = targetCreature;
		this.aggressorCreature = aggressorCreature;
		this.impactedBodyPart = impactedBodyPart;
		this.damageLevel = damageLevel;
	}
}
