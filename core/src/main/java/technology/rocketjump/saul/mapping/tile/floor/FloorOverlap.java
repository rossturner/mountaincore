package technology.rocketjump.saul.mapping.tile.floor;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.saul.assets.model.FloorType;
import technology.rocketjump.saul.mapping.tile.MapVertex;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.ChildPersistable;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.rendering.utils.HexColors;

public class FloorOverlap implements ChildPersistable {

	private OverlapLayout layout;
	private FloorType floorType;
	private GameMaterial material;
	private Color[] vertexColors = new Color[4]; // Affected by floor/wall type

	public FloorOverlap() {

	}

	public FloorOverlap(OverlapLayout layout, FloorType floorType, GameMaterial material, MapVertex[] vertexNeighboursOfCell) {
		this.layout = layout;
		this.floorType = floorType;
		this.material = material;

		if (floorType.isUseMaterialColor()) {
			Color floorMaterialColor = material.getColor();
			setVertexColors(floorMaterialColor, floorMaterialColor, floorMaterialColor, floorMaterialColor);
		} else {
			setVertexColors(
					floorType.getColorForHeightValue(vertexNeighboursOfCell[0].getHeightmapValue()),
					floorType.getColorForHeightValue(vertexNeighboursOfCell[1].getHeightmapValue()),
					floorType.getColorForHeightValue(vertexNeighboursOfCell[2].getHeightmapValue()),
					floorType.getColorForHeightValue(vertexNeighboursOfCell[3].getHeightmapValue())
			);
		}
	}

	public OverlapLayout getLayout() {
		return layout;
	}

	public GameMaterial getMaterial() {
		return material;
	}

	public FloorType getFloorType() {
		return floorType;
	}

	public Color[] getVertexColors() {
		return vertexColors;
	}

	public void setVertexColors(Color first, Color second, Color third, Color fourth) {
		vertexColors[0] = first.cpy();
		vertexColors[1] = second.cpy();
		vertexColors[2] = third.cpy();
		vertexColors[3] = fourth.cpy();
	}


	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("layout", layout.getId());
		asJson.put("type", floorType.getFloorTypeName());
		asJson.put("material", material.getMaterialName());

		if (vertexColors[0] == vertexColors[1] && vertexColors[0] == vertexColors[2] && vertexColors[0] == vertexColors[3]) {
			asJson.put("color", HexColors.toHexString(vertexColors[0]));
		} else {
			asJson.put("color0", HexColors.toHexString(vertexColors[0]));
			asJson.put("color1", HexColors.toHexString(vertexColors[1]));
			asJson.put("color2", HexColors.toHexString(vertexColors[2]));
			asJson.put("color3", HexColors.toHexString(vertexColors[3]));
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.layout = new OverlapLayout(asJson.getIntValue("layout"));
		this.floorType = relatedStores.floorTypeDictionary.getByFloorTypeName(asJson.getString("type"));
		if (this.floorType == null) {
			throw new InvalidSaveException("Could not find floor type with name " + asJson.getString("type"));
		}
		this.material = relatedStores.gameMaterialDictionary.getByName(asJson.getString("material"));
		if (this.material == null) {
			throw new InvalidSaveException("Could not find material by name " + asJson.getString("material"));
		}

		String singleColorHex = asJson.getString("color");
		if (singleColorHex == null) {
			setVertexColors(
					HexColors.get(asJson.getString("color0")),
					HexColors.get(asJson.getString("color1")),
					HexColors.get(asJson.getString("color2")),
					HexColors.get(asJson.getString("color3"))
			);
		} else {
			Color singleColor = HexColors.get(singleColorHex);
			setVertexColors(singleColor, singleColor, singleColor, singleColor);
		}
	}
}
