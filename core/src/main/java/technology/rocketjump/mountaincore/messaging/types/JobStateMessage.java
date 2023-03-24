package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.jobs.model.Job;
import technology.rocketjump.mountaincore.jobs.model.JobState;

public class JobStateMessage {

	public final Job job;
	public final JobState newState;

	public JobStateMessage(Job job, JobState newState) {
		this.job = job;
		this.newState = newState;
	}
}
