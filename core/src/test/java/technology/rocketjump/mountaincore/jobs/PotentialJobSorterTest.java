package technology.rocketjump.mountaincore.jobs;

import org.fest.assertions.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.mountaincore.jobs.model.Job;
import technology.rocketjump.mountaincore.jobs.model.JobPriority;
import technology.rocketjump.mountaincore.jobs.model.JobType;
import technology.rocketjump.mountaincore.jobs.model.PotentialJob;

import java.util.ArrayList;
import java.util.List;

@RunWith(MockitoJUnitRunner.class)
public class PotentialJobSorterTest {

	@Mock
	private JobType noProfessionJobType;

	private PotentialJobSorter potentialJobSorter;

	@Before
	public void setup() {
		potentialJobSorter = new PotentialJobSorter();

	}

	@Test
	public void sortsJobsByHighestPriorityFirst() {
		List<PotentialJob> potentialJobs = new ArrayList<>(List.of(
				job(JobPriority.LOWER),
				job(JobPriority.HIGHEST),
				job(JobPriority.NORMAL)
		));

		potentialJobs.sort(potentialJobSorter);

		Assertions.assertThat(potentialJobs.get(0).job.getJobPriority().equals(JobPriority.HIGHEST));
		Assertions.assertThat(potentialJobs.get(1).job.getJobPriority().equals(JobPriority.NORMAL));
		Assertions.assertThat(potentialJobs.get(2).job.getJobPriority().equals(JobPriority.LOWER));
	}

	@Test
	public void sortsJobsByPriorityThenProfessionOrder() {
		PotentialJob last = job(JobPriority.LOWER, 0);
		PotentialJob first = job(JobPriority.HIGHEST, 1);
		PotentialJob second = job(JobPriority.HIGHEST, 2);

		List<PotentialJob> potentialJobs = new ArrayList<>(List.of(
				last,
				second,
				first
		));

		potentialJobs.sort(potentialJobSorter);

		Assertions.assertThat(potentialJobs).containsExactly(first, second, last);
	}
	@Test
	public void sortsJobsByPriorityThenProfessionThenDistanceOrder() {
		PotentialJob last = job(JobPriority.LOWER, 0);
		PotentialJob first = job(JobPriority.HIGHEST, 0, 50f);
		PotentialJob second = job(JobPriority.HIGHEST, 0, 60f);

		List<PotentialJob> potentialJobs = new ArrayList<>(List.of(
				last,
				second,
				first
		));

		potentialJobs.sort(potentialJobSorter);

		Assertions.assertThat(potentialJobs).containsExactly(first, second, last);
	}

	private PotentialJob job(JobPriority jobPriority) {
		return job(jobPriority, 0);
	}

	private PotentialJob job(JobPriority jobPriority, int skillPriority) {
		return job(jobPriority, skillPriority, 0f);
	}

	private PotentialJob job(JobPriority jobPriority, int skillPriority, float distance) {
		Job job = new Job(noProfessionJobType);
		job.setJobPriority(jobPriority);
		return new PotentialJob(job, distance, skillPriority);
	}

}