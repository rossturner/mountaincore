package technology.rocketjump.mountaincore.jobs;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Disposable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.entities.components.Faction;
import technology.rocketjump.mountaincore.entities.components.FactionComponent;
import technology.rocketjump.mountaincore.entities.components.creature.SkillsComponent;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.Updatable;
import technology.rocketjump.mountaincore.jobs.model.Job;
import technology.rocketjump.mountaincore.jobs.model.JobPriority;
import technology.rocketjump.mountaincore.jobs.model.JobState;
import technology.rocketjump.mountaincore.jobs.model.PotentialJob;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.JobRequestMessage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class deals with dishing out jobs to entities requesting them
 */
@Singleton
public class JobRequestHandler implements Updatable, Telegraph, Disposable {

	private final MessageDispatcher messageDispatcher;
	private final JobStore jobStore;

	private GameContext gameContext;
	private final Comparator<? super PotentialJob> potentialJobSorter = new PotentialJobSorter();

	@Inject
	public JobRequestHandler(MessageDispatcher messageDispatcher, JobStore jobStore) {
		this.messageDispatcher = messageDispatcher;
		this.jobStore = jobStore;

		messageDispatcher.addListener(this, MessageType.JOB_REQUESTED);
	}

	@Override
	public void dispose() {
		messageDispatcher.removeListener(this, MessageType.JOB_REQUESTED);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.JOB_REQUESTED: {
				JobRequestMessage jobRequestMessage = (JobRequestMessage) msg.extraInfo;
				if (jobRequestMessage.getRequestingEntity().getComponent(FactionComponent.class).getFaction().equals(Faction.SETTLEMENT)) {
					gameContext.getJobRequestQueue().addLast(jobRequestMessage);
				}
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this + ", " + msg);
		}
	}

	@Override
	public void update(float deltaTime) {
		int outstandingRequests = gameContext.getJobRequestQueue().size();
		int requestsToProcess = Math.max(2, outstandingRequests / 2); // either 2 or half of outstanding requests

		for (int processed = 0; processed < requestsToProcess; processed++) {
			if (!gameContext.getJobRequestQueue().isEmpty()) {
				JobRequestMessage jobRequest = gameContext.getJobRequestQueue().pop();
				if (!jobRequest.isCancelled()) {
					handle(jobRequest);
				}
			}
		}
	}

	private boolean handle(JobRequestMessage jobRequestMessage) {
		SkillsComponent skillsComponent = jobRequestMessage.getRequestingEntity().getComponent(SkillsComponent.class);
		Vector2 entityWorldPosition = jobRequestMessage.getRequestingEntity().getLocationComponent().getWorldPosition();
		GridPoint2 requesterLocation = new GridPoint2((int)Math.floor(entityWorldPosition.x), (int)Math.floor(entityWorldPosition.y));

		List<PotentialJob> allPotentialJobs = new ArrayList<>();

		int skillPriority = 0;
		for (SkillsComponent.QuantifiedSkill professionToFindJobFor : skillsComponent.getActiveProfessions()) {
			Collection<Job> byProfession = jobStore.getCollectionByState(JobState.ASSIGNABLE).getByProfession(professionToFindJobFor.getSkill()).values();
			if (byProfession.isEmpty()) {
				continue;
			}

			for (Job job : byProfession) {
				if (job.getAssignedToEntityId() == null && !job.getJobPriority().equals(JobPriority.DISABLED)) {
					float distanceToJob = job.getJobLocation().dst(requesterLocation);
					allPotentialJobs.add(new PotentialJob(job, distanceToJob, skillPriority));
				}
			}
			skillPriority++;
		}
		allPotentialJobs.sort(potentialJobSorter);

		// FIXME Should maybe prioritise jobs that need equipment so they are worked on when a settler has the item,
		// rather than picking up the item and then going and working on something else
		jobRequestMessage.getCallback().jobCallback(allPotentialJobs.stream().map(p -> p.job).collect(Collectors.toList()), gameContext);
		return true;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}

	@Override
	public boolean runWhilePaused() {
		return true;
	}
}
