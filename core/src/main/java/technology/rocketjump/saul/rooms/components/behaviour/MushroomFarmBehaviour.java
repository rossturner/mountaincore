package technology.rocketjump.saul.rooms.components.behaviour;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.behaviour.furniture.MushroomShockTankBehaviour;
import technology.rocketjump.saul.entities.behaviour.furniture.Prioritisable;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.model.*;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.TransformConstructionMessage;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.rooms.HaulingAllocation;
import technology.rocketjump.saul.rooms.HaulingAllocationBuilder;
import technology.rocketjump.saul.rooms.Room;
import technology.rocketjump.saul.rooms.RoomTile;
import technology.rocketjump.saul.rooms.components.RoomComponent;
import technology.rocketjump.saul.rooms.constructions.Construction;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static technology.rocketjump.saul.entities.behaviour.furniture.CraftingStationBehaviour.getAnyNavigableWorkspace;
import static technology.rocketjump.saul.entities.behaviour.furniture.MushroomShockTankBehaviour.MushrooomShockTankState.ASSIGNED;
import static technology.rocketjump.saul.entities.behaviour.furniture.MushroomShockTankBehaviour.MushrooomShockTankState.AVAILABLE;

/**
 * This class organises the transfer of innoculated mushroom logs to shock tanks,
 * and back again to shocked mushroom log constructions,
 * as well as swapping innoculated mushroom log constructions to shocked mushroom log constructions
 */
public class MushroomFarmBehaviour extends RoomBehaviourComponent implements Prioritisable {

	private final Set<Entity> innoculatedLogFurnitureEntities = new HashSet<>();
	private final Set<Entity> shockTankFurnitureEntities = new HashSet<>();
	private final List<Job> haulingJobs = new ArrayList<>();
	private FurnitureType innoculatedLogFurnitureType;
	private FurnitureType shockedLogFurnitureType;
	private JobType haulingJobType;
	private Skill mushroomFarmingProfession;

	public MushroomFarmBehaviour(Room parent, MessageDispatcher messageDispatcher) {
		super(parent, messageDispatcher);
	}

	@Override
	public void setPriority(JobPriority jobPriority) {
		super.setPriority(jobPriority);
		for (Job haulingJob : haulingJobs) {
			haulingJob.setJobPriority(jobPriority);
		}
	}

	@Override
	public void infrequentUpdate(GameContext gameContext, MessageDispatcher messageDispatcher) {
		refreshFurnitureEntities(gameContext);
		haulingJobs.removeIf(job -> job.getJobState().equals(JobState.REMOVED));

		for (Entity shockTank : shockTankFurnitureEntities) {
			MushroomShockTankBehaviour shockTankBehaviour = (MushroomShockTankBehaviour) shockTank.getBehaviourComponent();
			if (shockTankBehaviour.getState().equals(AVAILABLE)) {
				for (Entity innoculatedLogFurniture : innoculatedLogFurnitureEntities) {
					if (!haulingJobExists(innoculatedLogFurniture)) {
						haulLogToShockTank(innoculatedLogFurniture, shockTank, gameContext);
						break;
					}
				}
			}
		}

	}

	private void haulLogToShockTank(Entity innoculatedLog, Entity shockTank, GameContext gameContext) {
		HaulingAllocation haulingAllocation = HaulingAllocationBuilder.createToHaulFurniture(innoculatedLog)
						.toEntity(shockTank);

		FurnitureLayout.Workspace navigableWorkspace = getAnyNavigableWorkspace(innoculatedLog, gameContext.getAreaMap());
		if (navigableWorkspace != null) {
			Job haulingJob = new Job(haulingJobType);
			haulingJob.setJobPriority(priority);
			haulingJob.setTargetId(innoculatedLog.getId());
			haulingJob.setJobLocation(navigableWorkspace.getAccessedFrom());
			haulingJob.setHaulingAllocation(haulingAllocation);
			haulingJob.setRequiredProfession(mushroomFarmingProfession);

			this.haulingJobs.add(haulingJob);
			messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, haulingJob);

			MushroomShockTankBehaviour shockTankBehaviour = (MushroomShockTankBehaviour) shockTank.getBehaviourComponent();
			shockTankBehaviour.getHaulingJobs().add(haulingJob);
			shockTankBehaviour.setState(ASSIGNED);
		} else {
			Logger.warn("Can not haul log to mushroom shock tank - no navigable workspaces");
		}
	}


	@Override
	public RoomComponent clone(Room newParent) {
		MushroomFarmBehaviour cloned = new MushroomFarmBehaviour(newParent, messageDispatcher);
		cloned.haulingJobs.addAll(this.haulingJobs);
		cloned.innoculatedLogFurnitureType = this.innoculatedLogFurnitureType;
		cloned.shockedLogFurnitureType = this.shockedLogFurnitureType;
		cloned.haulingJobType = this.haulingJobType;
		cloned.mushroomFarmingProfession = this.mushroomFarmingProfession;
		return cloned;
	}

	@Override
	public void mergeFrom(RoomComponent otherComponent) {
		if (otherComponent instanceof MushroomFarmBehaviour) {
			MushroomFarmBehaviour other = (MushroomFarmBehaviour) otherComponent;
			this.haulingJobs.addAll(other.haulingJobs);
		}
	}

	@Override
	public void tileRemoved(GridPoint2 location) {
		// Don't need to do anything, list of furniture entities is updated every cycle
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {

	}

	private boolean haulingJobExists(Entity innoculationLog) {
		for (Job haulingJob : haulingJobs) {
			if (haulingJob.getHaulingAllocation().getHauledEntityId() == innoculationLog.getId()) {
				return true;
			}
		}
		return false;
	}

	private void refreshFurnitureEntities(GameContext gameContext) {
		innoculatedLogFurnitureEntities.clear();
		shockTankFurnitureEntities.clear();

		for (RoomTile roomTile : parent.getRoomTiles().values()) {
			for (Entity entity : roomTile.getTile().getEntities()) {
				if (entity.getType().equals(EntityType.FURNITURE)) {
					FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
					if (attributes.getFurnitureType().equals(innoculatedLogFurnitureType)) {
						innoculatedLogFurnitureEntities.add(entity);
					} else if (entity.getBehaviourComponent() instanceof MushroomShockTankBehaviour) {
						shockTankFurnitureEntities.add(entity);
					}
				}
			}

			Construction construction = roomTile.getTile().getConstruction();
			if (construction != null && construction.getEntity() != null && construction.getEntity().getType().equals(EntityType.FURNITURE)) {
				FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) construction.getEntity().getPhysicalEntityComponent().getAttributes();
				if (attributes.getFurnitureType().equals(innoculatedLogFurnitureType)) {
					messageDispatcher.dispatchMessage(MessageType.TRANSFORM_CONSTRUCTION, new TransformConstructionMessage(construction, shockedLogFurnitureType));
				}
			}
		}
	}

	public void setMushroomFarmingProfession(Skill profession) {
		this.mushroomFarmingProfession = profession;
	}

	public void setFurnitureTypes(FurnitureType innoculatedLog, FurnitureType shockedLog) {
		this.innoculatedLogFurnitureType = innoculatedLog;
		this.shockedLogFurnitureType = shockedLog;
	}

	public void setJobTypes(JobType haulingJobType) {
		this.haulingJobType = haulingJobType;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);

		if (!haulingJobs.isEmpty()) {
			JSONArray jobsJson = new JSONArray();
			for (Job job : haulingJobs) {
				job.writeTo(savedGameStateHolder);
				jobsJson.add(job.getJobId());
			}
			asJson.put("jobs", jobsJson);
		}

		asJson.put("haulingJobType", haulingJobType.getName());
		asJson.put("innoculatedLogFurnitureType", innoculatedLogFurnitureType.getName());
		asJson.put("shockedLogFurnitureType", shockedLogFurnitureType.getName());

		if (mushroomFarmingProfession != null) {
			asJson.put("profession", mushroomFarmingProfession.getName());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);
		JSONArray jobsJson = asJson.getJSONArray("jobs");
		if (jobsJson != null) {
			for (int cursor = 0; cursor < jobsJson.size(); cursor++) {
				Job job = savedGameStateHolder.jobs.get(jobsJson.getLongValue(cursor));
				if (job == null) {
					throw new InvalidSaveException("Could not find job by ID " + jobsJson.getLongValue(cursor));
				}
				haulingJobs.add(job);
			}
		}

		this.haulingJobType = relatedStores.jobTypeDictionary.getByName(asJson.getString("haulingJobType"));
		if (haulingJobType == null) {
			throw new InvalidSaveException("Could not find job type with name " + asJson.getString("haulingJobType"));
		}
		this.innoculatedLogFurnitureType = relatedStores.furnitureTypeDictionary.getByName(asJson.getString("innoculatedLogFurnitureType"));
		if (this.innoculatedLogFurnitureType == null) {
			throw new InvalidSaveException("Could not find furniture type by name " + asJson.getString("innoculatedLogFurnitureType") + " in " + this.getClass().getSimpleName());
		}
		this.shockedLogFurnitureType = relatedStores.furnitureTypeDictionary.getByName(asJson.getString("shockedLogFurnitureType"));
		if (this.shockedLogFurnitureType == null) {
			throw new InvalidSaveException("Could not find furniture type by name " + asJson.getString("shockedLogFurnitureType") + " in " + this.getClass().getSimpleName());
		}

		String requiredProfessionName = asJson.getString("profession");
		if (requiredProfessionName != null) {
			this.mushroomFarmingProfession = relatedStores.skillDictionary.getByName(requiredProfessionName);
			if (this.mushroomFarmingProfession == null) {
				throw new InvalidSaveException("Could not find profession by name " + requiredProfessionName);
			}
		}
	}
}
