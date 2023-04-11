package technology.rocketjump.mountaincore.jobs;

import com.badlogic.gdx.math.GridPoint2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.Updatable;
import technology.rocketjump.mountaincore.jobs.model.Job;
import technology.rocketjump.mountaincore.jobs.model.JobCollection;
import technology.rocketjump.mountaincore.jobs.model.JobState;
import technology.rocketjump.mountaincore.jobs.model.JobType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
public class JobStore implements Updatable {

	private static final float UPDATE_CYCLE_TIME = 0.141f;
	private final SkillDictionary skillDictionary;

	private Map<JobState, JobCollection> byState = new ConcurrentHashMap<>();
	private Map<GridPoint2, List<Job>> byLocation = new ConcurrentHashMap<>();
	private Map<Long, Job> byRelatedHaulingAllocationId = new ConcurrentHashMap<>();
	private Map<JobType, List<Job>> byType = new ConcurrentHashMap<>();
	private GameContext gameContext;

	private float timeSinceLastUpdate;

	@Inject
	public JobStore(SkillDictionary skillDictionary) {
		this.skillDictionary = skillDictionary;
		clearContextRelatedState();
	}

	public JobCollection getCollectionByState(JobState jobState) {
		return byState.get(jobState);
	}

	public void add(Job job) {
		gameContext.getJobs().put(job.getJobId(), job);
		byState.get(job.getJobState()).add(job);
		byType.computeIfAbsent(job.getType(), a -> new ArrayList<>()).add(job);
		GridPoint2 jobLocation = job.getJobLocation();
		if (jobLocation == null) {
			Logger.error("Job location should never be null");
		} else {
			byLocation.computeIfAbsent(jobLocation, k -> new ArrayList<>()).add(job);
		}

		if (job.getHaulingAllocation() != null) {
			byRelatedHaulingAllocationId.put(job.getHaulingAllocation().getHaulingAllocationId(), job);
		}
	}

	private static final List<Job> emptyList = new LinkedList<>();

	public List<Job> getJobsAtLocation(GridPoint2 location) {
		if (location == null) {
			return emptyList;
		}
		List<Job> jobs = byLocation.get(location);
		if (jobs == null) {
			return emptyList;
		} else {
			return jobs;
		}
	}

	public void remove(Job jobToRemove) {
		gameContext.getJobs().remove(jobToRemove.getJobId());
		byState.get(jobToRemove.getJobState()).remove(jobToRemove);
		byType.get(jobToRemove.getType()).remove(jobToRemove);
		getJobsAtLocation(jobToRemove.getJobLocation()).remove(jobToRemove);
		if (byLocation.get(jobToRemove.getJobLocation()) != null && byLocation.get(jobToRemove.getJobLocation()).isEmpty()) {
			byLocation.remove(jobToRemove.getJobLocation());
		}
		jobToRemove.setJobState(JobState.REMOVED);
		if (jobToRemove.getAssignedToEntityId() != null) {
			Entity assignedEntity = gameContext.getEntities().get(jobToRemove.getAssignedToEntityId());
			if (assignedEntity != null && assignedEntity.getBehaviourComponent() instanceof CreatureBehaviour behaviour) {
				behaviour.getCurrentGoal().setInterrupted(true);
			}
		}
		jobToRemove.setAssignedToEntityId(-1L);
		if (jobToRemove.getHaulingAllocation() != null) {
			byRelatedHaulingAllocationId.remove(jobToRemove.getHaulingAllocation().getHaulingAllocationId());
		}
	}

	public Job getByHaulingAllocationId(Long haulingAllocationId) {
		return byRelatedHaulingAllocationId.get(haulingAllocationId);
	}

	public void switchState(Job job, JobState targetState) {
		JobState currentState = job.getJobState();
		if (!currentState.equals(targetState)) {
			byState.get(currentState).remove(job);
			job.setJobState(targetState);
			byState.get(targetState).add(job);
		}
	}

	public Map<Long, Job> getAllJobs() {
		return gameContext.getJobs();
	}

	@Override
	public void clearContextRelatedState() {
		for (JobState jobState : JobState.values()) {
			byState.put(jobState, new JobCollection(jobState, skillDictionary));
		}
		byLocation.clear();
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;

		for (Job job : gameContext.getJobs().values()) {
			add(job);
		}
	}

	@Override
	public void update(float deltaTime) {
		if (this.gameContext == null) {
			return;
		}

		timeSinceLastUpdate += deltaTime;
		if (timeSinceLastUpdate > UPDATE_CYCLE_TIME) {
			timeSinceLastUpdate = 0f;

			Job assignedJobToCheck = byState.get(JobState.ASSIGNED).next();
			if (assignedJobToCheck == null) {
				return;
			}
			if (assignedJobToCheck.getJobState().equals(JobState.REMOVED)) {
				remove(assignedJobToCheck);
				return;
			}

			if (assignedJobToCheck.getAssignedToEntityId() == null) {
				switchState(assignedJobToCheck, JobState.ASSIGNABLE);
				return;
			}

			Entity assignedEntity = gameContext.getEntities().get(assignedJobToCheck.getAssignedToEntityId());

			if (entityNoLongerAssigned(assignedEntity, assignedJobToCheck)) {
				Logger.warn("Found a job assigned to an entity doing something else");
				assignedJobToCheck.setAssignedToEntityId(null);
				switchState(assignedJobToCheck, JobState.ASSIGNABLE);
			}
		}
	}

	private boolean entityNoLongerAssigned(Entity assignedEntity, Job assignedJobToCheck) {
		if (assignedEntity == null || assignedEntity.getBehaviourComponent() == null || !(assignedEntity.getBehaviourComponent() instanceof CreatureBehaviour)) {
			return true;
		}

		CreatureBehaviour creatureBehaviour = (CreatureBehaviour) assignedEntity.getBehaviourComponent();
		AssignedGoal currentGoal = creatureBehaviour.getCurrentGoal();

		return currentGoal.getAssignedJob() == null || !currentGoal.getAssignedJob().equals(assignedJobToCheck);
	}

	@Override
	public boolean runWhilePaused() {
		return false;
	}

	public Collection<Job> getByType(JobType jobType) {
		return byType.getOrDefault(jobType, emptyList);
	}
}
