package technology.rocketjump.saul.messaging.types;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.saul.doors.DoorwayOrientation;
import technology.rocketjump.saul.doors.DoorwaySize;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.materials.model.GameMaterialType;
import technology.rocketjump.saul.persistence.EnumParser;
import technology.rocketjump.saul.persistence.JSONUtils;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.ChildPersistable;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

public class DoorwayPlacementMessage implements ChildPersistable {

	private DoorwaySize doorwaySize;
	private DoorwayOrientation orientation;
	private GameMaterialType doorwayMaterialType;
	private GameMaterial doorwayMaterial;
	private GridPoint2 tilePosition;

	public DoorwayPlacementMessage() {

	}

	public DoorwayPlacementMessage(DoorwaySize doorwaySize, DoorwayOrientation orientation, GameMaterialType doorwayMaterialType,
								   GameMaterial doorwayMaterial, GridPoint2 tilePosition) {
		this.doorwaySize = doorwaySize;
		this.orientation = orientation;
		this.doorwayMaterialType = doorwayMaterialType;
		this.doorwayMaterial = doorwayMaterial;
		this.tilePosition = tilePosition;
	}

	public DoorwaySize getDoorwaySize() {
		return doorwaySize;
	}

	public DoorwayOrientation getOrientation() {
		return orientation;
	}

	public GameMaterial getDoorwayMaterial() {
		return doorwayMaterial;
	}

	public GameMaterialType getDoorwayMaterialType() {
		return doorwayMaterialType;
	}

	public GridPoint2 getTilePosition() {
		return tilePosition;
	}

	public void setDoorwayMaterial(GameMaterial doorwayMaterial) {
		this.doorwayMaterial = doorwayMaterial;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (doorwaySize != null) {
			asJson.put("size", doorwaySize.name());
		}
		if (orientation != null) {
			asJson.put("orientation", orientation.name());
		}
		if (doorwayMaterial != null) {
			asJson.put("material", doorwayMaterial.getMaterialName());
		}
		if (doorwayMaterialType != null) {
			asJson.put("materialType", doorwayMaterialType.name());
		}
		if (tilePosition != null) {
			asJson.put("position", JSONUtils.toJSON(tilePosition));
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.doorwaySize = EnumParser.getEnumValue(asJson, "size", DoorwaySize.class, null);
		this.orientation = EnumParser.getEnumValue(asJson, "orientation", DoorwayOrientation.class, null);
		String materialName = asJson.getString("material");
		if (materialName != null) {
			this.doorwayMaterial = relatedStores.gameMaterialDictionary.getByName(materialName);
			if (this.doorwayMaterial == null) {
				throw new InvalidSaveException("Could not find material by name " + materialName);
			}
		}

		this.doorwayMaterialType = EnumParser.getEnumValue(asJson, "materialType", GameMaterialType.class, null);
		this.tilePosition = JSONUtils.gridPoint2(asJson.getJSONObject("position"));
	}
}
