package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.jobs.model.Job;
import technology.rocketjump.saul.jobs.model.JobState;

public class JobStateMessage {

	public final Job job;
	public final JobState newState;

	public JobStateMessage(Job job, JobState newState) {
		this.job = job;
		this.newState = newState;
	}
}
