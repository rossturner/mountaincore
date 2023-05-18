package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.jobs.model.Skill;

public class ChangeProfessionMessage {

	public final Entity entity;
	public final int activeSkillIndex;
	public final Skill newProfession;

	public ChangeProfessionMessage(Entity entity, int activeSkillIndex, Skill newProfession) {
		this.entity = entity;
		this.activeSkillIndex = activeSkillIndex;
		this.newProfession = newProfession;
	}
}
