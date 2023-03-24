package technology.rocketjump.mountaincore.rooms.components.behaviour;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.Prioritisable;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.mountaincore.entities.tags.DeceasedContainerTag;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.jobs.JobStore;
import technology.rocketjump.mountaincore.jobs.model.Job;
import technology.rocketjump.mountaincore.jobs.model.JobPriority;
import technology.rocketjump.mountaincore.jobs.model.JobType;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rooms.Room;
import technology.rocketjump.mountaincore.rooms.RoomTile;
import technology.rocketjump.mountaincore.rooms.components.RoomComponent;
import technology.rocketjump.mountaincore.rooms.constructions.Construction;
import technology.rocketjump.mountaincore.rooms.constructions.ConstructionState;

import java.util.HashMap;
import java.util.Map;

import static technology.rocketjump.mountaincore.misc.VectorUtils.toGridPoint;

public class GraveyardBehaviour extends RoomBehaviourComponent implements Prioritisable {

	private Map<Long, Entity> deceasedContainerEntities = new HashMap<>();
	private Map<Long, Construction> graveConstructions = new HashMap<>();

	private JobStore jobStore;
	private FurnitureTypeDictionary furnitureTypeDictionary;
	private JobType diggingJobType;
	private JobType fillGraveJobType;

	public GraveyardBehaviour(Room parent, MessageDispatcher messageDispatcher) {
		super(parent, messageDispatcher);
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
	}

	@Override
	public RoomComponent clone(Room newParent) {
		GraveyardBehaviour cloned = new GraveyardBehaviour(newParent, this.messageDispatcher);
		cloned.jobStore = this.jobStore;
		cloned.furnitureTypeDictionary = this.furnitureTypeDictionary;
		cloned.diggingJobType = this.diggingJobType;
		cloned.fillGraveJobType = this.fillGraveJobType;
		return cloned;
	}

	@Override
	public void mergeFrom(RoomComponent otherComponent) {
		// No state to merge, either static or refreshed on update
	}

	@Override
	public void setPriority(JobPriority jobPriority) {
		super.setPriority(jobPriority);
		for (Construction construction : graveConstructions.values()) {
			construction.setPriority(jobPriority, messageDispatcher);
		}
	}

	@Override
	public void infrequentUpdate(GameContext gameContext, MessageDispatcher messageDispatcher) {
		refreshConstructionsAndFurniture(gameContext);

		for (Construction graveConstruction : graveConstructions.values()) {
			createDiggingJobIfRequired(graveConstruction);
		}

		for (Entity deceasedContainer : deceasedContainerEntities.values()) {
			doUpdate(deceasedContainer);
		}
	}

	private void doUpdate(Entity deceasedContainer) {
		InventoryComponent inventoryComponent = deceasedContainer.getOrCreateComponent(InventoryComponent.class);
		boolean containsDeceased = false;
		for (InventoryComponent.InventoryEntry inventoryEntry : inventoryComponent.getInventoryEntries()) {
			if (inventoryEntry.entity.getType().equals(EntityType.CREATURE)) {
				containsDeceased = true;
				break;
			}
		}

		if (containsDeceased) {
			DeceasedContainerTag deceasedContainerTag = deceasedContainer.getTag(DeceasedContainerTag.class);
			FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) deceasedContainer.getPhysicalEntityComponent().getAttributes();
			FurnitureType transformationType = null;
			if (!deceasedContainerTag.getArgs().isEmpty()) {
				transformationType = furnitureTypeDictionary.getByName(deceasedContainerTag.getArgs().get(0));
			}
			boolean requiresTransformation = transformationType != null && !attributes.getFurnitureType().equals(transformationType);

			if (requiresTransformation) {
				createFillingJobIfNoneExisting(deceasedContainer);
			}
		}
	}

	private void createDiggingJobIfRequired(Construction graveConstruction) {
		if (!graveConstruction.getState().equals(ConstructionState.WAITING_FOR_COMPLETION)) {
			return;
		}
		for (Job existingJob : jobStore.getJobsAtLocation(graveConstruction.getPrimaryLocation())) {
			if (existingJob != null && existingJob.getTargetId() != null && existingJob.getTargetId() == graveConstruction.getId()) {
				return;
			}
		}

		Job diggingJob = new Job(diggingJobType);
		diggingJob.setJobPriority(priority);
		diggingJob.setTargetId(graveConstruction.getId());
		diggingJob.setJobLocation(graveConstruction.getPrimaryLocation());
		messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, diggingJob);
	}

	private void createFillingJobIfNoneExisting(Entity deceasedContainer) {
		GridPoint2 location = toGridPoint(deceasedContainer.getLocationComponent().getWorldOrParentPosition());
		for (Job jobAtLocation : jobStore.getJobsAtLocation(location)) {
			if (jobAtLocation.getType().equals(fillGraveJobType)) {
				return;
			}
		}

		Job fillingJob = new Job(fillGraveJobType);
		fillingJob.setJobPriority(priority);
		fillingJob.setTargetId(deceasedContainer.getId());
		fillingJob.setJobLocation(location);
		messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, fillingJob);
	}

	public void setJobTypes(JobType diggingJobType, JobType fillGraveJobType) {
		this.diggingJobType = diggingJobType;
		this.fillGraveJobType = fillGraveJobType;
	}

	private void refreshConstructionsAndFurniture(GameContext gameContext) {
		graveConstructions.clear();
		for (RoomTile roomTile : parent.getRoomTiles().values()) {
			Construction construction = roomTile.getTile().getConstruction();
			if (construction != null && construction.getRequirements().isEmpty()) {
				graveConstructions.put(construction.getId(), construction);
			}

			for (Entity entity : roomTile.getTile().getEntities()) {
				if (entity.getType().equals(EntityType.FURNITURE) && entity.getTag(DeceasedContainerTag.class) != null) {
					deceasedContainerEntities.put(entity.getId(), entity);
				}
			}
		}
	}

	public void setJobStore(JobStore jobStore) {
		this.jobStore = jobStore;
	}

	public void setFurnitureTypeDictionary(FurnitureTypeDictionary furnitureTypeDictionary) {
		this.furnitureTypeDictionary = furnitureTypeDictionary;
	}

	@Override
	public void tileRemoved(GridPoint2 location) {
		// Don't need to do anything, list of furniture entities is updated every cycle
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);
		asJson.put("diggingJobType", diggingJobType.getName());
		asJson.put("fillGraveJobType", fillGraveJobType.getName());
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);
		jobStore = relatedStores.jobStore;
		furnitureTypeDictionary = relatedStores.furnitureTypeDictionary;

		diggingJobType = relatedStores.jobTypeDictionary.getByName(asJson.getString("diggingJobType"));
		if (diggingJobType == null) {
			throw new InvalidSaveException("Could not find job type " + asJson.getString("diggingJobType") + " for " + getClass().getSimpleName());
		}
		fillGraveJobType = relatedStores.jobTypeDictionary.getByName(asJson.getString("fillGraveJobType"));
		if (fillGraveJobType == null) {
			throw new InvalidSaveException("Could not find job type " + asJson.getString("fillGraveJobType") + " for " + getClass().getSimpleName());
		}
	}
}
