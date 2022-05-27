package technology.rocketjump.saul.entities.behaviour.furniture;

import technology.rocketjump.saul.jobs.model.JobPriority;

public interface Prioritisable {

	JobPriority getPriority();

	void setPriority(JobPriority jobPriority);

}
