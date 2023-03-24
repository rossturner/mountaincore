package technology.rocketjump.mountaincore.entities.model.physical.effect;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.mountaincore.assets.entities.model.ColoringLayer;
import technology.rocketjump.mountaincore.entities.model.physical.EntityAttributes;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import java.util.Map;

public class OngoingEffectAttributes implements EntityAttributes {

	private long seed;
	private OngoingEffectType effectType;
	private float effectRadius = 0.4f;

	public OngoingEffectAttributes() {

	}

	public OngoingEffectAttributes(long seed, OngoingEffectType effectType) {
		this.seed = seed;
		this.effectType = effectType;
	}

	public OngoingEffectType getType() {
		return effectType;
	}

	@Override
	public long getSeed() {
		return seed;
	}

	@Override
	public Color getColor(ColoringLayer coloringLayer) {
		return null;
	}

	@Override
	public EntityAttributes clone() {
		return null;
	}

	@Override
	public Map<GameMaterialType, GameMaterial> getMaterials() {
		return Map.of();
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("seed", seed);
		asJson.put("type", effectType.getName());
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		seed = asJson.getLongValue("seed");
		effectType = relatedStores.ongoingEffectTypeDictionary.getByName(asJson.getString("type"));
		if (effectType == null) {
			throw new InvalidSaveException("Could not find ongoing effect type with name " + asJson.getString("type"));
		}
	}

	public float getEffectRadius() {
		return effectRadius;
	}

	public void setEffectRadius(float effectRadius) {
		this.effectRadius = effectRadius;
	}
}
