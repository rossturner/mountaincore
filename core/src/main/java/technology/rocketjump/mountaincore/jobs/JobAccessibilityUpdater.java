package technology.rocketjump.mountaincore.jobs;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.EntityStore;
import technology.rocketjump.mountaincore.entities.components.creature.SkillsComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.Updatable;
import technology.rocketjump.mountaincore.jobs.model.Job;
import technology.rocketjump.mountaincore.jobs.model.JobState;
import technology.rocketjump.mountaincore.mapping.tile.CompassDirection;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.mapping.tile.TileNeighbours;
import technology.rocketjump.mountaincore.messaging.MessageType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static technology.rocketjump.mountaincore.entities.ai.goap.actions.location.GoToLocationAction.calculatePosition;
import static technology.rocketjump.mountaincore.misc.VectorUtils.toGridPoint;

/**
 * This class is mostly responsible for switching potentially accessible jobs to assignable jobs
 */
@Singleton
public class JobAccessibilityUpdater implements Updatable {

	public static final float TIME_BETWEEN_INACCESSIBLE_RETRIES = 3.143f;

	private final JobStore jobStore;
	private final EntityStore entityStore;
	private final MessageDispatcher messageDispatcher;

	private GameContext gameContext;
	private float timeSinceLastInaccessibleUpdate = 0f;

	@Inject
	public JobAccessibilityUpdater(JobStore jobStore, EntityStore entityStore, MessageDispatcher messageDispatcher) {
		this.jobStore = jobStore;
		this.entityStore = entityStore;
		this.messageDispatcher = messageDispatcher;
	}

	/**
	 * This method works through one potentially accessible job per frame
	 * @param deltaTime
	 */
	@Override
	public void update(float deltaTime) {
		if (gameContext != null) {
			timeSinceLastInaccessibleUpdate += deltaTime;
			if (timeSinceLastInaccessibleUpdate > TIME_BETWEEN_INACCESSIBLE_RETRIES) {
				timeSinceLastInaccessibleUpdate = 0f;

				Job inaccessibleJob = jobStore.getCollectionByState(JobState.INACCESSIBLE).next();
				if (inaccessibleJob != null) {
					jobStore.switchState(inaccessibleJob, JobState.POTENTIALLY_ACCESSIBLE);
				}
			}

			checkNextPotentiallyAccessible();
		}
	}

	private void checkNextPotentiallyAccessible() {
		Job potentiallyAccessibleJob = jobStore.getCollectionByState(JobState.POTENTIALLY_ACCESSIBLE).next();
		if (potentiallyAccessibleJob == null) {
			// No outstanding potentially accessible jobs
			return;
		}
		Entity assignableEntity = getEntityToPathfindFrom(potentiallyAccessibleJob);
		if (assignableEntity == null) {
			// No entities to assign to
			return;
		}
		Vector2 entityWorldPosition = assignableEntity.getLocationComponent().getWorldOrParentPosition();

		List<GridPoint2> jobLocations = new ArrayList<>();

		if (potentiallyAccessibleJob.getHaulingAllocation() != null) {
			jobLocations.add(toGridPoint(calculatePosition(potentiallyAccessibleJob.getHaulingAllocation(), gameContext)));
		} else if (potentiallyAccessibleJob.getType().isAccessedFromAdjacentTile()) {
			TileNeighbours jobNeighbourTiles = gameContext.getAreaMap().getOrthogonalNeighbours(potentiallyAccessibleJob.getJobLocation().x, potentiallyAccessibleJob.getJobLocation().y);
			for (CompassDirection compassDirection : jobNeighbourTiles.keySet()) {
				if (!jobNeighbourTiles.get(compassDirection).isNavigable(null)) {
					jobNeighbourTiles.remove(compassDirection);
				}
			}
			if (jobNeighbourTiles.isEmpty()) {
				// None of the adjacent tiles were accessible, so this job is actually inaccessible now
				jobStore.switchState(potentiallyAccessibleJob, JobState.INACCESSIBLE);
				return;
			} else {
				for (MapTile mapTile : jobNeighbourTiles.values()) {
					jobLocations.add(mapTile.getTilePosition());
				}
				Collections.shuffle(jobLocations);
			}
		} else {
			if (potentiallyAccessibleJob.getJobLocation() == null) {
				Logger.error("Job location is null for job {}, will cancel", potentiallyAccessibleJob);
				messageDispatcher.dispatchMessage(MessageType.JOB_REMOVED, potentiallyAccessibleJob);
				return;
			}
			jobLocations.add(potentiallyAccessibleJob.getJobLocation());
		}

		if (isLocationNavigable(jobLocations, entityWorldPosition)) {
			jobStore.switchState(potentiallyAccessibleJob, JobState.ASSIGNABLE);
		} else {
			jobStore.switchState(potentiallyAccessibleJob, JobState.INACCESSIBLE);
		}
	}

	@Override
	public boolean runWhilePaused() {
		return true;
	}

	private Entity getEntityToPathfindFrom(Job job) {
		List<Entity> candidates = new ArrayList<>();
		for (Entity jobAssignableEntity : entityStore.getJobAssignableEntities()) {
			SkillsComponent skillsComponent = jobAssignableEntity.getComponent(SkillsComponent.class);
			if (skillsComponent != null && skillsComponent.hasActiveProfession(job.getRequiredProfession())) {
				candidates.add(jobAssignableEntity);
			}
		}

		if (candidates.isEmpty()) {
			return null;
		} else {
			return candidates.get(gameContext.getRandom().nextInt(candidates.size()));
		}
	}

	private boolean isLocationNavigable(List<GridPoint2> locations, Vector2 entityWorldPosition) {
		if (locations.isEmpty()) {
			return false;
		} else {
			GridPoint2 locationToTry = locations.get(gameContext.getRandom().nextInt(locations.size()));

			MapTile originTile = gameContext.getAreaMap().getTile(entityWorldPosition);
			MapTile targetTile = gameContext.getAreaMap().getTile(locationToTry);

			// Just checking if job is in same region
			return originTile != null && targetTile != null && originTile.getRegionId() == targetTile.getRegionId();
		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {
	}

}
