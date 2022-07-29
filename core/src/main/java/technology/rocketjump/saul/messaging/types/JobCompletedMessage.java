package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.components.creature.ProfessionsComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.jobs.model.Job;

public class JobCompletedMessage {

	private final Job job;
	private final ProfessionsComponent completedBy;
	private final Entity completedByEntity;

	public JobCompletedMessage(Job job, ProfessionsComponent completedBy, Entity completedByEntity) {
		this.job = job;
		this.completedBy = completedBy;
		this.completedByEntity = completedByEntity;
	}

	public Job getJob() {
		return job;
	}

	public ProfessionsComponent getCompletedBy() {
		return completedBy;
	}

	public Entity getCompletedByEntity() {
		return completedByEntity;
	}
}
