package technology.rocketjump.mountaincore.mapping;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.constants.ConstantsRepo;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.mechanism.MechanismEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.mechanism.MechanismType;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.GameContextAware;
import technology.rocketjump.mountaincore.jobs.JobStore;
import technology.rocketjump.mountaincore.jobs.JobTypeDictionary;
import technology.rocketjump.mountaincore.jobs.model.Job;
import technology.rocketjump.mountaincore.jobs.model.JobType;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.mapping.tile.underground.UnderTile;
import technology.rocketjump.mountaincore.messaging.MessageType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.stream.Collectors;

@Singleton
public class MechanismConstructionManager implements GameContextAware {
	private final MessageDispatcher messageDispatcher;

	private GameContext gameContext;
	private final JobType constructMechanismJobType;
	private final JobType deconstructMechanismJobType;
	private final JobStore jobStore;

	@Inject
	public MechanismConstructionManager(ConstantsRepo constantsRepo, JobTypeDictionary jobTypeDictionary,
										MessageDispatcher messageDispatcher, JobStore jobStore) {
		this.messageDispatcher = messageDispatcher;
		this.jobStore = jobStore;

		constructMechanismJobType = jobTypeDictionary.getByName(constantsRepo.getSettlementConstants().getConstructMechanismJobType());
		if (constructMechanismJobType == null) {
			throw new RuntimeException("Could not find job with name " + constantsRepo.getSettlementConstants().getConstructMechanismJobType() + " from " + constantsRepo.getSettlementConstants().getClass().getSimpleName());
		}

		deconstructMechanismJobType = jobTypeDictionary.getByName(constantsRepo.getSettlementConstants().getDeconstructMechanismJobType());
		if (deconstructMechanismJobType == null) {
			throw new RuntimeException("Could not find job with name " + constantsRepo.getSettlementConstants().getDeconstructMechanismJobType() + " from " + constantsRepo.getSettlementConstants().getClass().getSimpleName());
		}
	}

	public void mechanismConstructionAdded(MapTile mapTile, MechanismType mechanismType) {
		UnderTile underTile = mapTile.getOrCreateUnderTile();

		if (underTile.getQueuedMechanismType() != null) {
			if (underTile.getQueuedMechanismType().equals(mechanismType)) {
				return;
			}
			mechanismConstructionRemoved(mapTile);
		}

		underTile.setQueuedMechanismType(mechanismType);

		Job constructionJob = new Job(constructMechanismJobType);
		constructionJob.setRequiredItemType(mechanismType.getRelatedItemType());
		constructionJob.setRequiredProfession(mechanismType.getRelatedProfession());
		constructionJob.setJobLocation(mapTile.getTilePosition());
		messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, constructionJob);
	}

	public void mechanismConstructionRemoved(MapTile mapTile) {
		removeEitherJobType(mapTile);

		mapTile.getOrCreateUnderTile().setQueuedMechanismType(null);
	}

	public void mechanismDeconstructionAdded(MapTile mapTile) {
		UnderTile underTile = mapTile.getUnderTile();
		if (underTile != null) {
			Entity powerMechanismEntity = underTile.getPowerMechanismEntity();
			if (powerMechanismEntity != null) {
				MechanismEntityAttributes attributes = (MechanismEntityAttributes) powerMechanismEntity.getPhysicalEntityComponent().getAttributes();

				Job deconstructJob = new Job(deconstructMechanismJobType);
				deconstructJob.setRequiredProfession(attributes.getMechanismType().getRelatedProfession());
				deconstructJob.setJobLocation(mapTile.getTilePosition());
				messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, deconstructJob);
			}
		}
	}

	public void mechanismDeconstructionRemoved(MapTile parentTile) {
		removeEitherJobType(parentTile);
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}

	private void removeEitherJobType(MapTile mapTile) {
		jobStore.getJobsAtLocation(mapTile.getTilePosition())
				.stream()
				.filter(j -> j.getType().equals(constructMechanismJobType) || j.getType().equals(deconstructMechanismJobType))
				.collect(Collectors.toList())// avoids ConcurrentModificationException
				.forEach(job -> {
					messageDispatcher.dispatchMessage(MessageType.JOB_REMOVED, job);
				});
	}
}
