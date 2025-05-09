package technology.rocketjump.mountaincore.entities.model.physical;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.apache.commons.lang3.NotImplementedException;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAsset;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetType;
import technology.rocketjump.mountaincore.entities.components.EntityComponent;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.effect.OngoingEffectAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.mechanism.MechanismEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.vehicle.VehicleEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.persistence.EnumParser;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import java.util.HashMap;
import java.util.Map;

public class PhysicalEntityComponent implements EntityComponent {

	private EntityAttributes attributes;
	private EntityAsset baseAsset;
	private final Map<EntityAssetType, EntityAsset> typeMap = new HashMap<>();

	private float animationProgress; // Interpolates to range of frames for animated assets

	@Override
	public PhysicalEntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		PhysicalEntityComponent cloned = new PhysicalEntityComponent();
		cloned.attributes = this.attributes.clone();
		cloned.baseAsset = this.baseAsset;
		for (Map.Entry<EntityAssetType, EntityAsset> entry : typeMap.entrySet()) {
			cloned.typeMap.put(entry.getKey(), entry.getValue());
		}
		return cloned;
	}

	public Map<EntityAssetType, EntityAsset> getTypeMap() {
		return typeMap;
	}

	public EntityAsset getBaseAsset() {
		return baseAsset;
	}

	public void setBaseAsset(EntityAsset baseAsset) {
		this.baseAsset = baseAsset;
		typeMap.clear();
	}

	public EntityAttributes getAttributes() {
		return attributes;
	}

	public void setAttributes(EntityAttributes attributes) {
		this.attributes = attributes;
	}

	public float getAnimationProgress() {
		return animationProgress;
	}

	public void setAnimationProgress(float animationProgress) {
		this.animationProgress = animationProgress;
	}

	@Override
	public String toString() {
		return attributes.toString();
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (attributes instanceof CreatureEntityAttributes) {
			asJson.put("entityType", EntityType.CREATURE.name());
		} else if (attributes instanceof ItemEntityAttributes) {
			asJson.put("entityType", EntityType.ITEM.name());
		} else  if (attributes instanceof FurnitureEntityAttributes) {
			asJson.put("entityType", EntityType.FURNITURE.name());
		} else if (attributes instanceof PlantEntityAttributes) {
			asJson.put("entityType", EntityType.PLANT.name());
		} else if (attributes instanceof OngoingEffectAttributes) {
			asJson.put("entityType", EntityType.ONGOING_EFFECT.name());
		} else if (attributes instanceof MechanismEntityAttributes) {
			asJson.put("entityType", EntityType.MECHANISM.name());
		} else if (attributes instanceof VehicleEntityAttributes) {
			asJson.put("entityType", EntityType.VEHICLE.name());
		} else {
			throw new NotImplementedException("Not yet implemented: " + attributes.getClass().getSimpleName());
		}

		JSONObject attributesJson = new JSONObject(true);
		attributes.writeTo(attributesJson, savedGameStateHolder);
		asJson.put("attributes", attributesJson);

		if (baseAsset != null) {
			asJson.put("baseAsset", baseAsset.getUniqueName());
		}

		JSONArray assetNames = new JSONArray();
		for (EntityAsset asset : typeMap.values()) {
			assetNames.add(asset.getUniqueName());
		}
		asJson.put("assets", assetNames);

		if (animationProgress != 0f) {
			asJson.put("animation", animationProgress);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		EntityType entityType = EnumParser.getEnumValue(asJson, "entityType", EntityType.class, null);
		if (entityType == null) {
			throw new InvalidSaveException("Unrecognised entity type: " + entityType);
		}

		switch (entityType) {
			case CREATURE:
				attributes = new CreatureEntityAttributes();
				break;
			case ITEM:
				attributes = new ItemEntityAttributes();
				break;
			case FURNITURE:
				attributes = new FurnitureEntityAttributes();
				break;
			case PLANT:
				attributes = new PlantEntityAttributes();
				break;
			case ONGOING_EFFECT:
				attributes = new OngoingEffectAttributes();
				break;
			case MECHANISM:
				attributes = new MechanismEntityAttributes();
				break;
			case VEHICLE:
				attributes = new VehicleEntityAttributes();
				break;
		}
		JSONObject attributesJson = asJson.getJSONObject("attributes");
		attributes.readFrom(attributesJson, savedGameStateHolder, relatedStores);

		this.baseAsset = relatedStores.completeAssetDictionary.getByUniqueName(asJson.getString("baseAsset"));
		if (this.baseAsset == null) {
			Logger.error("Could not find asset by name " + asJson.getString("baseAsset"));
		}

		JSONArray assetNames = asJson.getJSONArray("assets");
		for (int cursor = 0; cursor < assetNames.size(); cursor++) {
			String assetName = assetNames.getString(cursor);
			EntityAsset asset = relatedStores.completeAssetDictionary.getByUniqueName(assetName);
			if (asset == null) {
				throw new InvalidSaveException("Could not find asset by name " + assetName);
			} else {
				this.typeMap.put(asset.getType(), asset);
			}
		}

		Float animationProgress = asJson.getFloat("animation");
		if (animationProgress != null) {
			this.animationProgress = animationProgress;
		}
	}
}
