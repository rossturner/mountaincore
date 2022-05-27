package technology.rocketjump.saul.entities.planning;

import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.model.Job;

import java.util.List;

public interface JobAssignmentCallback {

	void jobCallback(List<Job> potentialJobs, GameContext gameContext);

}
