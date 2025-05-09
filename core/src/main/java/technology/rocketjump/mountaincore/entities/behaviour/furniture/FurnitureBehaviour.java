package technology.rocketjump.mountaincore.entities.behaviour.furniture;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.mountaincore.entities.components.BehaviourComponent;
import technology.rocketjump.mountaincore.entities.components.creature.SteeringComponent;
import technology.rocketjump.mountaincore.entities.components.furniture.ConstructedEntityComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.LocationComponent;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.jobs.model.JobPriority;
import technology.rocketjump.mountaincore.jobs.model.JobState;
import technology.rocketjump.mountaincore.jobs.model.JobType;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.misc.VectorUtils;
import technology.rocketjump.mountaincore.persistence.EnumParser;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rooms.Room;
import technology.rocketjump.mountaincore.rooms.RoomType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FurnitureBehaviour implements BehaviourComponent {

	protected LocationComponent locationComponent;
	protected MessageDispatcher messageDispatcher;
	protected Entity parentEntity;
	protected GameContext gameContext;

	protected List<ItemType> relatedItemTypes = new ArrayList<>(0);
	protected List<JobType> relatedJobTypes = new ArrayList<>(0);
	protected List<FurnitureType> relatedFurnitureTypes = new ArrayList<>(0);
	protected List<GameMaterial> relatedMaterials = new ArrayList<>(0);
	protected JobPriority priority = JobPriority.NORMAL;
	private double lastUpdateGameTime;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.locationComponent = parentEntity.getLocationComponent();
		this.messageDispatcher = messageDispatcher;
		this.parentEntity = parentEntity;
		this.gameContext = gameContext;
		this.lastUpdateGameTime = gameContext.getGameClock().getCurrentGameTime();
	}

	@Override
	public FurnitureBehaviour clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		FurnitureBehaviour cloned = new FurnitureBehaviour();
		cloned.init(parentEntity, messageDispatcher, gameContext);
		return cloned;
	}

	@Override
	public void update(float deltaTime) {
		// Do nothing, does not update every frame
	}

	@Override
	public void updateWhenPaused() {

	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		double gameTime = gameContext.getGameClock().getCurrentGameTime();
		lastUpdateGameTime = gameTime;

		if (parentEntity.isOnFire()) {
			return;
		}

		ConstructedEntityComponent constructedEntityComponent = parentEntity.getComponent(ConstructedEntityComponent.class);
		if (constructedEntityComponent != null) {
			if (constructedEntityComponent.getDeconstructionJob() != null) {
				if (constructedEntityComponent.getDeconstructionJob().getJobState().equals(JobState.INACCESSIBLE)) {
					List<GridPoint2> furnitureLocations = getFurnitureLocations();
					constructedEntityComponent.getDeconstructionJob().setJobLocation(furnitureLocations.get(gameContext.getRandom().nextInt(furnitureLocations.size())));
				} else if (constructedEntityComponent.getDeconstructionJob().getJobState().equals(JobState.REMOVED)) {
					// job was removed, retry deconstruction job
					messageDispatcher.dispatchMessage(MessageType.JOB_REMOVED, constructedEntityComponent.getDeconstructionJob());
					constructedEntityComponent.setDeconstructionJob(null);
					messageDispatcher.dispatchMessage(MessageType.REQUEST_FURNITURE_REMOVAL, parentEntity);
					if (constructedEntityComponent.getDeconstructionJob() != null) {
						constructedEntityComponent.getDeconstructionJob().setJobPriority(this.priority);
					}
				}
			} else if (!constructedEntityComponent.isBeingDeconstructed() && constructedEntityComponent.canBeDeconstructed()) {
				// Check to see if this furniture is in an illegal placement

				FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
				if (!attributes.getFurnitureType().isPlaceAnywhere()) {
					// Need to check we're in a valid room
					Set<RoomType> validRoomTypes = attributes.getFurnitureType().getValidRoomTypes();

					Room matchedRoom = null;
					List<GridPoint2> locationsToCheck = getFurnitureLocations();

					for (GridPoint2 locationToCheck : locationsToCheck) {
						MapTile tile = gameContext.getAreaMap().getTile(locationToCheck);
						if (tile == null || !tile.hasRoom()) {
							matchedRoom = null;
							break;
						}
						Room room = tile.getRoomTile().getRoom();
						if (matchedRoom == null) {
							matchedRoom = room;
						} else if (!room.equals(matchedRoom)) {
							// Split across different rooms which is not valid
							matchedRoom = null;
							break;
						} // else everything is okay so go on to the next tile
					}

					if (matchedRoom == null || !validRoomTypes.contains(matchedRoom.getRoomType())) {
						messageDispatcher.dispatchMessage(MessageType.REQUEST_FURNITURE_REMOVAL, parentEntity);
					}
				}
			}
		}

	}

	private List<GridPoint2> getFurnitureLocations() {
		FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		List<GridPoint2> locations = new ArrayList<>(attributes.getCurrentLayout().getExtraTiles().size() + 1);
		GridPoint2 worldLocation = VectorUtils.toGridPoint(locationComponent.getWorldPosition());
		locations.add(worldLocation);
		for (GridPoint2 offset : attributes.getCurrentLayout().getExtraTiles()) {
			locations.add(worldLocation.cpy().add(offset));
		}
		return locations;
	}


	@Override
	public SteeringComponent getSteeringComponent() {
		return null;
	}

	@Override
	public boolean isUpdateEveryFrame() {
		return false;
	}

	@Override
	public boolean isUpdateInfrequently() {
		return true;
	}

	@Override
	public boolean isJobAssignable() {
		return false;
	}

	public void setRelatedItemTypes(List<ItemType> relatedItemTypes) {
		this.relatedItemTypes = relatedItemTypes;
	}

	public void setRelatedJobTypes(List<JobType> relatedJobTypes) {
		this.relatedJobTypes = relatedJobTypes;
	}

	public void setRelatedFurnitureTypes(List<FurnitureType> relatedFurnitureTypes) {
		this.relatedFurnitureTypes = relatedFurnitureTypes;
	}

	public void setRelatedMaterials(List<GameMaterial> relatedMaterials) {
		this.relatedMaterials = relatedMaterials;
	}

	public List<ItemType> getRelatedItemTypes() {
		return relatedItemTypes;
	}

	public JobPriority getPriority() {
		return priority;
	}

	public void setPriority(JobPriority jobPriority) {
		this.priority = jobPriority;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (!relatedItemTypes.isEmpty()) {
			JSONArray relatedItemTypesJson = new JSONArray();
			for (ItemType relatedItemType : relatedItemTypes) {
				relatedItemTypesJson.add(relatedItemType.getItemTypeName());
			}
			asJson.put("relatedItemTypes", relatedItemTypesJson);
		}
		if (!relatedJobTypes.isEmpty()) {
			JSONArray relatedJobTypesJson = new JSONArray();
			for (JobType relatedJobType : relatedJobTypes) {
				relatedJobTypesJson.add(relatedJobType.getName());
			}
			asJson.put("relatedJobTypes", relatedJobTypesJson);
		}
		if (!relatedFurnitureTypes.isEmpty()) {
			JSONArray relatedFurnitureTypesJson = new JSONArray();
			for (FurnitureType furnitureType : relatedFurnitureTypes) {
				relatedFurnitureTypesJson.add(furnitureType.getName());
			}
			asJson.put("relatedFurnitureTypes", relatedFurnitureTypesJson);
		}
		if (!relatedMaterials.isEmpty()) {
			JSONArray relatedMaterialsJson = new JSONArray();
			for (GameMaterial material : relatedMaterials) {
				relatedMaterialsJson.add(material.getMaterialName());
			}
			asJson.put("relatedMaterials", relatedMaterialsJson);
		}
		if (!priority.equals(JobPriority.NORMAL)) {
			asJson.put("priority", priority.name());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		JSONArray relatedItemTypesJson = asJson.getJSONArray("relatedItemTypes");
		if (relatedItemTypesJson != null) {
			for (int cursor = 0; cursor < relatedItemTypesJson.size(); cursor++) {
				ItemType relatedItemType = relatedStores.itemTypeDictionary.getByName(relatedItemTypesJson.getString(cursor));
				if (relatedItemType == null) {
					throw new InvalidSaveException("Could not find item type with name " + relatedItemTypesJson.getString(cursor));
				} else {
					relatedItemTypes.add(relatedItemType);
				}
			}
		}
		JSONArray relatedJobTypesJson = asJson.getJSONArray("relatedJobTypes");
		if (relatedJobTypesJson != null) {
			for (int cursor = 0; cursor < relatedJobTypesJson.size(); cursor++) {
				JobType relatedJobType = relatedStores.jobTypeDictionary.getByName(relatedJobTypesJson.getString(cursor));
				if (relatedJobType == null) {
					throw new InvalidSaveException("Could not find job type with name " + relatedJobTypesJson.getString(cursor));
				} else {
					relatedJobTypes.add(relatedJobType);
				}
			}
		}
		JSONArray relatedFurnitureTypesJson = asJson.getJSONArray("relatedFurnitureTypes");
		if (relatedFurnitureTypesJson != null) {
			for (int cursor = 0; cursor < relatedFurnitureTypesJson.size(); cursor++) {
				FurnitureType relatedFurnitureType = relatedStores.furnitureTypeDictionary.getByName(relatedFurnitureTypesJson.getString(cursor));
				if (relatedFurnitureType == null) {
					throw new InvalidSaveException("Could not find furniture type with name " + relatedFurnitureTypesJson.getString(cursor));
				} else {
					relatedFurnitureTypes.add(relatedFurnitureType);
				}
			}
		}
		JSONArray relatedMaterialsJson = asJson.getJSONArray("relatedMaterials");
		if (relatedMaterialsJson != null) {
			for (int cursor = 0; cursor < relatedMaterialsJson.size(); cursor++) {
				GameMaterial relatedMaterial = relatedStores.gameMaterialDictionary.getByName(relatedMaterialsJson.getString(cursor));
				if (relatedMaterial == null) {
					throw new InvalidSaveException("Could not find material type with name " + relatedMaterialsJson.getString(cursor));
				} else {
					relatedMaterials.add(relatedMaterial);
				}
			}
		}
		this.priority = EnumParser.getEnumValue(asJson, "priority", JobPriority.class, JobPriority.NORMAL);
	}

}