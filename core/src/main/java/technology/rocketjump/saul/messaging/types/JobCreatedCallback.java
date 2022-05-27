package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.jobs.model.Job;

public interface JobCreatedCallback {

	public void jobCreated(Job job);

}
