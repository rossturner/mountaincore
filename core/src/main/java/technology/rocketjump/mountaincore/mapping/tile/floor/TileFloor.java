package technology.rocketjump.mountaincore.mapping.tile.floor;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.mountaincore.assets.model.FloorType;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.ChildPersistable;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rendering.utils.HexColors;
import technology.rocketjump.mountaincore.rooms.Bridge;

import java.util.ArrayList;
import java.util.List;

public class TileFloor implements ChildPersistable {

	private FloorType type;
	private GameMaterial material;

	private RiverTile riverTile;

	private Bridge bridge;
	private BridgeTile bridgeTile;

	private final List<FloorOverlap> overlaps = new ArrayList<>();
	private final List<FloorOverlap> transitoryOverlaps = new ArrayList<>();

	private final Color[] vertexColors = new Color[4]; // Affected by floor/wall type

	public TileFloor() {
		setVertexColors(Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE);
	}

	public TileFloor(FloorType type, GameMaterial material) {
		this();
		this.type = type;
		this.material = material;
	}

	public FloorType getFloorType() {
		return type;
	}

	public void setFloorType(FloorType floorType) {
		this.type = floorType;
	}

	public void setMaterial(GameMaterial material) {
		this.material = material;
	}

	public List<FloorOverlap> getOverlaps() {
		return overlaps;
	}

	public List<FloorOverlap> getTransitoryOverlaps() {
		return transitoryOverlaps;
	}

	public GameMaterial getMaterial() {
		return material;
	}

	public boolean isRiverTile() {
		return riverTile != null;
	}

	public RiverTile getRiverTile() {
		return riverTile;
	}

	public void setRiverTile(RiverTile riverTile) {
		this.riverTile = riverTile;
	}

	public boolean hasBridge() {
		return bridgeTile != null;
	}

	public Bridge getBridge() {
		return bridge;
	}

	public BridgeTile getBridgeTile() {
		return bridgeTile;
	}

	public void setBridgeTile(Bridge bridge, BridgeTile bridgeTile) {
		this.bridge = bridge;
		this.bridgeTile = bridgeTile;
	}

	@Override
	public String toString() {
		return material.getMaterialName() + " floor";
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
		asJson.put("type", type.getFloorTypeName());
		asJson.put("material", material.getMaterialName());

		if (riverTile != null) {
			JSONObject riverJson = new JSONObject(true);
			riverTile.writeTo(riverJson, savedGameStateHolder);
			asJson.put("river", riverJson);
		}
		if (bridge != null && bridgeTile != null) {
			bridge.writeTo(savedGameStateHolder);
			asJson.put("bridge", bridge.getBridgeId());

			JSONObject bridgeJson = new JSONObject(true);
			bridgeTile.writeTo(bridgeJson, savedGameStateHolder);
			asJson.put("bridgeTile", bridgeJson);
		}

		if (!overlaps.isEmpty()) {
			JSONArray overlapsJson = new JSONArray();
			for (FloorOverlap overlap : overlaps) {
				JSONObject overlapJson = new JSONObject(true);
				overlap.writeTo(overlapJson, savedGameStateHolder);
				overlapsJson.add(overlapJson);
			}
			asJson.put("overlaps", overlapsJson);
		}


		if (!transitoryOverlaps.isEmpty()) {
			JSONArray transitoryOverlapsJson = new JSONArray();
			for (FloorOverlap overlap : transitoryOverlaps) {
				JSONObject overlapJson = new JSONObject(true);
				overlap.writeTo(overlapJson, savedGameStateHolder);
				transitoryOverlapsJson.add(overlapJson);
			}
			asJson.put("transitoryOverlaps", transitoryOverlapsJson);
		}

		if (vertexColors[0].equals(vertexColors[1]) && vertexColors[0].equals(vertexColors[2]) && vertexColors[0].equals(vertexColors[3])) {
			// All vertexColors are same
			if (!vertexColors[0].equals(Color.WHITE)) {
				asJson.put("vertexColor", HexColors.toHexString(vertexColors[0]));
			}
		} else {
			JSONArray vertexColorJson = new JSONArray();
			for (int cursor = 0; cursor < vertexColors.length; cursor++) {
				vertexColorJson.add(HexColors.toHexString(vertexColors[cursor]));
			}
			asJson.put("vertexColors", vertexColorJson);
		}


	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.type = relatedStores.floorTypeDictionary.getByFloorTypeName(asJson.getString("type"));
		if (this.type == null) {
			throw new InvalidSaveException("Could not find floor type by name " + asJson.getString("type"));
		}
		this.material = relatedStores.gameMaterialDictionary.getByName(asJson.getString("material"));
		if (this.material == null) {
			throw new InvalidSaveException("Could not find material by name " + asJson.getString("material"));
		}

		JSONObject riverJson = asJson.getJSONObject("river");
		if (riverJson != null) {
			this.riverTile = new RiverTile(false);
			this.riverTile.readFrom(riverJson, savedGameStateHolder, relatedStores);
		}
		JSONObject bridgeJson = asJson.getJSONObject("bridgeTile");
		if (bridgeJson != null) {
			this.bridge = savedGameStateHolder.bridges.get(asJson.getLongValue("bridge"));
			if (this.bridge == null) {
				throw new InvalidSaveException("Could not find bridge with ID " + asJson.getLongValue("bridge") + " in " + getClass().getSimpleName());
			}
			this.bridgeTile = new BridgeTile();
			this.bridgeTile.readFrom(bridgeJson, savedGameStateHolder, relatedStores);
		}

		JSONArray overlapsJson = asJson.getJSONArray("overlaps");
		if (overlapsJson != null) {
			for (int cursor = 0; cursor < overlapsJson.size(); cursor++) {
				JSONObject overlapJson = overlapsJson.getJSONObject(cursor);
				FloorOverlap overlap = new FloorOverlap();
				overlap.readFrom(overlapJson, savedGameStateHolder, relatedStores);
				this.overlaps.add(overlap);
			}
		}

		JSONArray transitoryOverlaps = asJson.getJSONArray("transitoryOverlaps");
		if (transitoryOverlaps != null) {
			for (int cursor = 0; cursor < transitoryOverlaps.size(); cursor++) {
				JSONObject overlapJson = transitoryOverlaps.getJSONObject(cursor);
				FloorOverlap overlap = new FloorOverlap();
				overlap.readFrom(overlapJson, savedGameStateHolder, relatedStores);
				this.transitoryOverlaps.add(overlap);
			}
		}

		String vertexColorHex = asJson.getString("vertexColor");
		if (vertexColorHex != null) {
			Color vertexColor = HexColors.get(vertexColorHex);
			setVertexColors(vertexColor, vertexColor, vertexColor, vertexColor);
		} else {
			JSONArray vertexColorJson = asJson.getJSONArray("vertexColors");
			if (vertexColorJson != null) {
				setVertexColors(
						HexColors.get(vertexColorJson.getString(0)),
						HexColors.get(vertexColorJson.getString(1)),
						HexColors.get(vertexColorJson.getString(2)),
						HexColors.get(vertexColorJson.getString(3))
				);
			}
		}

	}

	public boolean isBridgeNavigable() {
		if (bridgeTile == null) {
			return false;
		} else {
			return bridgeTile.isNavigable(bridge);
		}
	}

}
