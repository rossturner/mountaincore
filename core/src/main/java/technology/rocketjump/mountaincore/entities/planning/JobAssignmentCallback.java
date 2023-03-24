package technology.rocketjump.mountaincore.entities.planning;

import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.jobs.model.Job;

import java.util.List;

public interface JobAssignmentCallback {

	void jobCallback(List<Job> potentialJobs, GameContext gameContext);

}
