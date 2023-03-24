package technology.rocketjump.mountaincore.entities.behaviour.furniture;

import technology.rocketjump.mountaincore.jobs.model.JobPriority;

public interface Prioritisable {

	JobPriority getPriority();

	void setPriority(JobPriority jobPriority);

}
