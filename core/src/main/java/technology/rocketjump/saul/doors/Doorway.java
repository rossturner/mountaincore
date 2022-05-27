package technology.rocketjump.saul.doors;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.saul.assets.entities.furniture.model.DoorState;
import technology.rocketjump.saul.entities.behaviour.furniture.DoorBehaviour;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.materials.model.GameMaterialType;
import technology.rocketjump.saul.persistence.EnumParser;
import technology.rocketjump.saul.persistence.JSONUtils;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.ChildPersistable;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents all the constituent parts that make up a door in a single tile
 * e.g. the wall caps, frame and door entities
 */
public class Doorway implements ChildPersistable {

	private DoorwayOrientation orientation;
	private DoorwaySize doorwaySize = DoorwaySize.SINGLE;
	private GridPoint2 tileLocation;
	private GameMaterialType doorwayMaterialType;

	private List<Entity> wallCapEntities = new ArrayList<>(); // feel this should be a list of separate caps to support different material wall ends
	private List<Entity> frameEntities = new ArrayList<>(); // the frame should be positioned at the bottom of the tile so it overlaps everything in the tile
	private Entity doorEntity; // the door should be positioned at the top of the tile so other entities are in front of it

	public DoorState getDoorState() {
		return ((DoorBehaviour)doorEntity.getBehaviourComponent()).getState();
	}

	public DoorwayOrientation getOrientation() {
		return orientation;
	}

	public void setOrientation(DoorwayOrientation orientation) {
		this.orientation = orientation;
	}

	public GridPoint2 getTileLocation() {
		return tileLocation;
	}

	public void setTileLocation(GridPoint2 tileLocation) {
		this.tileLocation = tileLocation;
	}

	public GameMaterialType getDoorwayMaterialType() {
		return doorwayMaterialType;
	}

	public void setDoorwayMaterialType(GameMaterialType doorwayMaterialType) {
		this.doorwayMaterialType = doorwayMaterialType;
	}

	public List<Entity> getWallCapEntities() {
		return wallCapEntities;
	}

	public void setWallCapEntities(List<Entity> wallCapEntities) {
		this.wallCapEntities = wallCapEntities;
	}

	public List<Entity> getFrameEntities() {
		return frameEntities;
	}

	public void setFrameEntities(List<Entity> frameEntities) {
		this.frameEntities = frameEntities;
	}

	public Entity getDoorEntity() {
		return doorEntity;
	}

	public void setDoorEntity(Entity doorEntity) {
		this.doorEntity = doorEntity;
	}

	public DoorwaySize getDoorwaySize() {
		return doorwaySize;
	}

	public void setDoorwaySize(DoorwaySize doorwaySize) {
		this.doorwaySize = doorwaySize;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("orientation", orientation.name());
		if (!doorwaySize.equals(DoorwaySize.SINGLE)) {
			asJson.put("size", doorwaySize.name());
		}
		if (tileLocation != null) {
			asJson.put("location", JSONUtils.toJSON(tileLocation));
		}
		if (doorwayMaterialType != null) {
			asJson.put("materialType", doorwayMaterialType.name());
		}

		JSONArray wallCapIds = new JSONArray();
		for (Entity wallCapEntity : wallCapEntities) {
			wallCapEntity.writeTo(savedGameStateHolder);
			wallCapIds.add(wallCapEntity.getId());
		}
		asJson.put("wallCaps", wallCapIds);

		JSONArray frameEntityIds = new JSONArray();
		for (Entity frameEntity : frameEntities) {
			frameEntity.writeTo(savedGameStateHolder);
			frameEntityIds.add(frameEntity.getId());
		}
		asJson.put("frameEnities", frameEntityIds);

		if (doorEntity != null) {
			doorEntity.writeTo(savedGameStateHolder);
			asJson.put("doorEntity", doorEntity.getId());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.orientation = EnumParser.getEnumValue(asJson, "orientation", DoorwayOrientation.class, null);
		this.doorwaySize = EnumParser.getEnumValue(asJson, "size", DoorwaySize.class, DoorwaySize.SINGLE);
		this.tileLocation = JSONUtils.gridPoint2(asJson.getJSONObject("location"));
		this.doorwayMaterialType = EnumParser.getEnumValue(asJson, "materialType", GameMaterialType.class, null);

		JSONArray wallCapIds = asJson.getJSONArray("wallCaps");
		for (int cursor = 0; cursor < wallCapIds.size(); cursor++) {
			wallCapEntities.add(savedGameStateHolder.entities.get(wallCapIds.getLongValue(cursor)));
		}


		JSONArray frameEntityIds = asJson.getJSONArray("frameEnities");
		for (int cursor = 0; cursor < frameEntityIds.size(); cursor++) {
			frameEntities.add(savedGameStateHolder.entities.get(frameEntityIds.getLongValue(cursor)));
		}

		Long doorEntityId = asJson.getLong("doorEntity");
		if (doorEntityId != null) {
			doorEntity = savedGameStateHolder.entities.get(doorEntityId);
			if (doorEntity == null) {
				throw new InvalidSaveException("Could not find entity by ID " + doorEntityId);
			}
		}
	}
}
