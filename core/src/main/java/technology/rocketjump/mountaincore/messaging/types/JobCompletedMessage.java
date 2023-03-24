package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.entities.components.creature.SkillsComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.jobs.model.Job;

public class JobCompletedMessage {

	private final Job job;
	private final SkillsComponent completedBy;
	private final Entity completedByEntity;

	public JobCompletedMessage(Job job, SkillsComponent completedBy, Entity completedByEntity) {
		this.job = job;
		this.completedBy = completedBy;
		this.completedByEntity = completedByEntity;
	}

	public Job getJob() {
		return job;
	}

	public SkillsComponent getCompletedBy() {
		return completedBy;
	}

	public Entity getCompletedByEntity() {
		return completedByEntity;
	}
}
