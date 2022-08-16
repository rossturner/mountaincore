package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.jobs.model.Skill;

public class ChangeProfessionMessage {

	public final Entity entity;
	public final Skill professionToReplace;
	public final Skill newProfession;

	public ChangeProfessionMessage(Entity entity, Skill professionToReplace, Skill newProfession) {
		this.entity = entity;
		this.professionToReplace = professionToReplace;
		this.newProfession = newProfession;
	}
}
