package technology.rocketjump.saul.settlement;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.entities.components.ItemAllocationComponent;
import technology.rocketjump.saul.entities.components.furniture.ConstructedEntityComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.tags.DeceasedContainerTag;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.Updatable;
import technology.rocketjump.saul.jobs.JobStore;
import technology.rocketjump.saul.jobs.JobTypeDictionary;
import technology.rocketjump.saul.jobs.model.Job;
import technology.rocketjump.saul.jobs.model.JobPriority;
import technology.rocketjump.saul.jobs.model.JobType;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rooms.HaulingAllocation;
import technology.rocketjump.saul.rooms.HaulingAllocationBuilder;

import static technology.rocketjump.saul.entities.behaviour.furniture.CraftingStationBehaviour.getAnyNavigableWorkspace;
import static technology.rocketjump.saul.entities.model.EntityType.CREATURE;
import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;

/**
 * This class is responsible for assigning and moving corpses to graves
 */
@Singleton
public class GraveManager implements Updatable {

	private static final float TIME_BETWEEN_INFREQUENT_UPDATE_SECONDS = 3.4f;

	private final MessageDispatcher messageDispatcher;
	private final SettlementFurnitureTracker settlementFurnitureTracker;
	private final SettlerTracker settlerTracker;
	private final JobStore jobStore;
	private final JobType haulingJobType;

	private GameContext gameContext;
	private float timeSinceLastUpdate = 0f;

	@Inject
	public GraveManager(MessageDispatcher messageDispatcher, SettlementFurnitureTracker settlementFurnitureTracker,
						SettlerTracker settlerTracker, JobStore jobStore, JobTypeDictionary jobTypeDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.settlementFurnitureTracker = settlementFurnitureTracker;
		this.settlerTracker = settlerTracker;
		this.jobStore = jobStore;
		this.haulingJobType = jobTypeDictionary.getByName("HAULING");
	}

	@Override
	public void update(float deltaTime) {
		timeSinceLastUpdate += deltaTime;
		if (gameContext != null && timeSinceLastUpdate > TIME_BETWEEN_INFREQUENT_UPDATE_SECONDS) {
			timeSinceLastUpdate = 0f;

			for (Entity deceased : settlerTracker.getDead()) {
				if (deceased.getLocationComponent().getContainerEntity() == null && !hasCorpseHaulingJob(deceased) && isUnallocated(deceased)) {
					Entity deceasedContainer = findAvailableDeceasedContainer();

					if (deceasedContainer == null) {
						// No graves/sarcophagi available, try again next update
						break;
					} else {
						assignAndCreateHaulingJob(deceased, deceasedContainer);
					}
				}
			}
		}
	}

	private boolean isUnallocated(Entity deceased) {
		ItemAllocationComponent itemAllocationComponent = deceased.getComponent(ItemAllocationComponent.class);
		return  itemAllocationComponent == null || itemAllocationComponent.getNumUnallocated() > 0;
	}

	private boolean hasCorpseHaulingJob(Entity deceased) {
		GridPoint2 location = toGridPoint(deceased.getLocationComponent().getWorldOrParentPosition());
		if (location == null) {
			// FIXME When does this happen? Returning true for now so nothing else tries to access this corpse
			return true;
		}

		for (Job jobAtLocation : jobStore.getJobsAtLocation(location)) {
			if (jobAtLocation.getType().equals(haulingJobType) && jobAtLocation.getHaulingAllocation().getHauledEntityType().equals(CREATURE)) {
				return true;
			}
		}
		return false;
	}

	private Entity findAvailableDeceasedContainer() {
		for (Entity deceasedContainer : settlementFurnitureTracker.findByTag(DeceasedContainerTag.class, true)) {
			ConstructedEntityComponent constructedEntityComponent = deceasedContainer.getComponent(ConstructedEntityComponent.class);
			if (constructedEntityComponent != null && constructedEntityComponent.isBeingDeconstructed()) {
				continue;
			}
			if (getAnyNavigableWorkspace(deceasedContainer, gameContext.getAreaMap()) != null) {
				return deceasedContainer;
			}
		}
		return null;
	}

	private void assignAndCreateHaulingJob(Entity deceased, Entity deceasedContainer) {

		GridPoint2 deceasedPosition = toGridPoint(deceased.getLocationComponent().getWorldPosition());
		MapTile deceasedTile = gameContext.getAreaMap().getTile(deceasedPosition);
		if (deceasedTile == null || !deceasedTile.isNavigable(null)) {
			// Skip while not navigable
			return;
		}

		FurnitureEntityAttributes furnitureAttributes = (FurnitureEntityAttributes) deceasedContainer.getPhysicalEntityComponent().getAttributes();
		furnitureAttributes.setAssignedToEntityId(deceased.getId());

		HaulingAllocation allocation = HaulingAllocationBuilder.createToHaulCreature(deceased)
				.toEntity(deceasedContainer);

		Job haulingJob = new Job(haulingJobType);
		haulingJob.setJobPriority(JobPriority.HIGHER);// Might prefer to set priority based on grave room priority
		haulingJob.setTargetId(allocation.getHauledEntityId());
		haulingJob.setHaulingAllocation(allocation);
		haulingJob.setJobLocation(allocation.getSourcePosition());
		messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, haulingJob);
	}

	@Override
	public boolean runWhilePaused() {
		return false;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {
		gameContext = null;
		timeSinceLastUpdate = 0f;
	}
}
