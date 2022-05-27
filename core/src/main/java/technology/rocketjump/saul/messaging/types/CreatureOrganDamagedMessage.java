package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.body.BodyPart;
import technology.rocketjump.saul.entities.model.physical.creature.body.BodyPartOrgan;
import technology.rocketjump.saul.entities.model.physical.creature.body.organs.OrganDamageLevel;

public class CreatureOrganDamagedMessage {

	public final Entity targetEntity;
	public final BodyPart impactedBodyPart;
	public final BodyPartOrgan impactedOrgan;
	public final OrganDamageLevel organDamageLevel;

	public CreatureOrganDamagedMessage(Entity targetEntity, BodyPart impactedBodyPart, BodyPartOrgan impactedOrgan, OrganDamageLevel organDamageLevel) {
		this.targetEntity = targetEntity;
		this.impactedBodyPart = impactedBodyPart;
		this.impactedOrgan = impactedOrgan;
		this.organDamageLevel = organDamageLevel;
	}
}
