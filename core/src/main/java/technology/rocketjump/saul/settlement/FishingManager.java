package technology.rocketjump.saul.settlement;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.GridPoint2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.constants.ConstantsRepo;
import technology.rocketjump.saul.constants.SettlementConstants;
import technology.rocketjump.saul.entities.components.creature.ProfessionsComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.Updatable;
import technology.rocketjump.saul.jobs.JobStore;
import technology.rocketjump.saul.jobs.JobTypeDictionary;
import technology.rocketjump.saul.jobs.ProfessionDictionary;
import technology.rocketjump.saul.jobs.model.Job;
import technology.rocketjump.saul.jobs.model.JobType;
import technology.rocketjump.saul.jobs.model.Profession;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.mapping.tile.floor.BridgeTile;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rooms.constructions.BridgeConstruction;
import technology.rocketjump.saul.rooms.constructions.Construction;
import technology.rocketjump.saul.rooms.constructions.ConstructionType;
import technology.rocketjump.saul.settlement.notifications.Notification;
import technology.rocketjump.saul.settlement.notifications.NotificationType;
import technology.rocketjump.saul.zones.Zone;
import technology.rocketjump.saul.zones.ZoneClassification;
import technology.rocketjump.saul.zones.ZoneTile;

import java.util.*;

import static technology.rocketjump.saul.jobs.LiquidMessageHandler.pickTileInZone;

@Singleton
public class FishingManager implements Updatable, Telegraph {

	private final MessageDispatcher messageDispatcher;
	private final SettlerTracker settlerTracker;
	private final SettlementConstants settlementConstants;
	private final JobStore jobStore;
	private final JobType fishingJobType;
	private final Profession fishingProfession;
	private GameContext gameContext;

	private float timeSinceLastUpdate;

	@Inject
	public FishingManager(MessageDispatcher messageDispatcher, SettlerTracker settlerTracker, ConstantsRepo constantsRepo,
						  JobStore jobStore, JobTypeDictionary jobTypeDictionary, ProfessionDictionary professionDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.settlerTracker = settlerTracker;
		this.settlementConstants = constantsRepo.getSettlementConstants();
		this.jobStore = jobStore;
		this.fishingJobType = jobTypeDictionary.getByName(settlementConstants.getFishingJobType());
		this.fishingProfession = professionDictionary.getByName("FISHER");

		messageDispatcher.addListener(this, MessageType.YEAR_ELAPSED);
		messageDispatcher.addListener(this, MessageType.FISH_HARVESTED_FROM_RIVER);
		messageDispatcher.addListener(this, MessageType.CONSTRUCTION_COMPLETED);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.YEAR_ELAPSED: {
				cancelAllOutstandingFishingJobs();
				gameContext.getSettlementState().setFishRemainingInRiver(settlementConstants.getNumAnnualFish());
				return true;
			}
			case MessageType.FISH_HARVESTED_FROM_RIVER: {
				gameContext.getSettlementState().setFishRemainingInRiver(gameContext.getSettlementState().getFishRemainingInRiver() - 1);
				if (gameContext.getSettlementState().getFishRemainingInRiver() <= 0) {
					cancelAllOutstandingFishingJobs();
					messageDispatcher.dispatchMessage(MessageType.POST_NOTIFICATION, new Notification(NotificationType.FISH_EXHAUSTED, null));
				}
				return true;
			}
			case MessageType.CONSTRUCTION_COMPLETED: {
				Construction construction = (Construction) msg.extraInfo;
				if (construction != null && construction.getConstructionType().equals(ConstructionType.BRIDGE_CONSTRUCTION)) {
					for (Map.Entry<GridPoint2, BridgeTile> bridgeEntry : ((BridgeConstruction) construction).getBridge().entrySet()) {
						GridPoint2 bridgeLocation = bridgeEntry.getKey();
						for (Job jobAtLocation : new ArrayList<>(jobStore.getJobsAtLocation(bridgeLocation))) {
							if (jobAtLocation.getType().equals(fishingJobType)) {
								messageDispatcher.dispatchMessage(MessageType.JOB_REMOVED, jobAtLocation);
							}
						}
					}
				}
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	@Override
	public void update(float deltaTime) {
		timeSinceLastUpdate += deltaTime;
		if (timeSinceLastUpdate > 3.471f) {
			timeSinceLastUpdate = 0f;
			doUpdate();
		}
	}

	private void doUpdate() {
		if (gameContext.getSettlementState().getFishRemainingInRiver() <= 0) {
			return;
		}

		Entity fisherSettler = null;
		for (Entity entity : settlerTracker.getAll()) {
			ProfessionsComponent professionsComponent = entity.getComponent(ProfessionsComponent.class);
			if (professionsComponent != null) {
				if (professionsComponent.hasActiveProfession(fishingProfession)) {
					fisherSettler = entity;
					break;
				}
			}
		}

		if (fisherSettler != null) {
			MapTile location = gameContext.getAreaMap().getTile(fisherSettler.getLocationComponent().getWorldOrParentPosition());
			int regionId = location.getRegionId();

			List<Zone> zonesInRegion = new ArrayList<>(gameContext.getAreaMap().getZonesInRegion(regionId));
			Collections.shuffle(zonesInRegion, gameContext.getRandom());
			for (Zone zone : zonesInRegion) {
				if (zone.getClassification().getZoneType().equals(ZoneClassification.ZoneType.LIQUID_SOURCE) &&
					zone.isActive() && !zone.getClassification().isConstructed()) {
					// Is a natural active liquid source

					boolean zoneHasExistingFishingJob = false;
					Iterator<ZoneTile> iterator = zone.iterator();
					while (iterator.hasNext()) {
						ZoneTile tile = iterator.next();
						zoneHasExistingFishingJob = jobStore.getJobsAtLocation(tile.getTargetTile()).stream()
								.anyMatch(j-> j.getType().equals(fishingJobType));
						if (zoneHasExistingFishingJob) {
							break;
						}
					}

					if (!zoneHasExistingFishingJob) {
						ZoneTile zoneTile = pickTileInZone(zone, gameContext.getRandom(), gameContext.getAreaMap());
						if (zoneTile != null) {
							MapTile targetTile = gameContext.getAreaMap().getTile(zoneTile.getTargetTile());
							if (!targetTile.getFloor().hasBridge()) {
								createFishingJob(zoneTile);
								break;
							}
						}
					}

				}
			}
		}
	}

	private void createFishingJob(ZoneTile zoneTile) {
		Job fishingJob = new Job(fishingJobType);
		fishingJob.setJobLocation(zoneTile.getTargetTile());
		fishingJob.setSecondaryLocation(zoneTile.getAccessLocation());
		messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, fishingJob);
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
		return false;
	}


	private void cancelAllOutstandingFishingJobs() {
		List<Job> fishingJobs = new ArrayList<>(jobStore.getByType(fishingJobType));
		for (Job fishingJob : fishingJobs) {
			messageDispatcher.dispatchMessage(MessageType.JOB_REMOVED, fishingJob);
		}
	}
}
