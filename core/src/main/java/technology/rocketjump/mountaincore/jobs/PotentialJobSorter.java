package technology.rocketjump.mountaincore.jobs;

import technology.rocketjump.mountaincore.jobs.model.PotentialJob;

import java.util.Comparator;

/**
 * This comparator sorts by priority, then having or not having a required profession, then distance
 */
public class PotentialJobSorter implements Comparator<PotentialJob> {

	private static final Comparator<PotentialJob> JOB_PRIORITY = Comparator.comparing(potentialJob -> potentialJob.job.getJobPriority());
	private static final Comparator<PotentialJob> PROFESSION_PRIORITY = Comparator.comparing(potentialJob -> potentialJob.skillPriority);
	private static final Comparator<PotentialJob> DISTANCE_PRIORITY = Comparator.comparing(potentialJob -> potentialJob.distance);
	private static final Comparator<PotentialJob> PRIORITY_CHAIN = JOB_PRIORITY.thenComparing(PROFESSION_PRIORITY).thenComparing(DISTANCE_PRIORITY);


	@Override
	public int compare(PotentialJob o1, PotentialJob o2) {
		return PRIORITY_CHAIN.compare(o1, o2);
	}

}
