package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.jobs.model.Job;

public interface JobCreatedCallback {

	public void jobCreated(Job job);

}
