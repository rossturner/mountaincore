package technology.rocketjump.mountaincore.mapping.tile.roof;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.MoreObjects;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.persistence.EnumParser;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.ChildPersistable;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

public class TileRoof implements ChildPersistable {

	private TileRoofState state = TileRoofState.MOUNTAIN_ROOF;
	private GameMaterial roofMaterial = GameMaterial.NULL_MATERIAL;
	private RoofConstructionState constructionState = RoofConstructionState.NONE;

	public TileRoof() {

	}

	public TileRoof(TileRoofState roofState, GameMaterial material) {
		this.state = roofState;
		this.roofMaterial = material;
	}

	public TileRoofState getState() {
		return state;
	}

	public void setState(TileRoofState state) {
		this.state = state;
	}

	public GameMaterial getRoofMaterial() {
		return roofMaterial;
	}

	public void setRoofMaterial(GameMaterial roofMaterial) {
		this.roofMaterial = roofMaterial;
	}

	public RoofConstructionState getConstructionState() {
		return constructionState;
	}

	public void setConstructionState(RoofConstructionState constructionState) {
		this.constructionState = constructionState;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {


		if (!state.equals(TileRoofState.MOUNTAIN_ROOF)) {
			asJson.put("state", state.name());
		}
		if (!roofMaterial.equals(GameMaterial.NULL_MATERIAL)) {
			asJson.put("roofMaterial", roofMaterial.getMaterialName());
		}
		if (!constructionState.equals(RoofConstructionState.NONE)) {
			asJson.put("constructionState", constructionState.name());
		}

	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.state = EnumParser.getEnumValue(asJson, "state", TileRoofState.class, TileRoofState.MOUNTAIN_ROOF);

		String roofMaterialName = asJson.getString("roofMaterial");
		if (roofMaterialName != null) {
			this.roofMaterial = relatedStores.gameMaterialDictionary.getByName(roofMaterialName);
			if (this.roofMaterial == null) {
				throw new InvalidSaveException("Could not find material with name " + roofMaterialName);
			}
		}

		this.constructionState = EnumParser.getEnumValue(asJson, "constructionState", RoofConstructionState.class, RoofConstructionState.NONE);
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("state", state)
				.add("roofMaterial", roofMaterial)
				.add("constructionState", constructionState)
				.toString();
	}
}
