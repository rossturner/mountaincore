package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.jobs.model.Profession;

public class ChangeProfessionMessage {

	public final Entity entity;
	public final Profession professionToReplace;
	public final Profession newProfession;

	public ChangeProfessionMessage(Entity entity, Profession professionToReplace, Profession newProfession) {
		this.entity = entity;
		this.professionToReplace = professionToReplace;
		this.newProfession = newProfession;
	}
}
