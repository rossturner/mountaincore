package technology.rocketjump.mountaincore.rooms.constructions;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.SequentialIdGenerator;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.mountaincore.entities.model.physical.item.QuantifiedItemType;
import technology.rocketjump.mountaincore.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.mountaincore.entities.tags.ConstructionOverrideTag;
import technology.rocketjump.mountaincore.entities.tags.Tag;
import technology.rocketjump.mountaincore.misc.VectorUtils;
import technology.rocketjump.mountaincore.persistence.JSONUtils;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FurnitureConstruction extends Construction {

	private Entity furnitureEntityToBePlaced;
	private Set<GridPoint2> tileLocations = new HashSet<>();
	private GridPoint2 primaryLocation;

	public FurnitureConstruction() {

	}

	public FurnitureConstruction(Entity furnitureEntityToBePlaced) {
		this.constructionId = SequentialIdGenerator.nextId();
		this.furnitureEntityToBePlaced = furnitureEntityToBePlaced;

		GridPoint2 location = VectorUtils.toGridPoint(furnitureEntityToBePlaced.getLocationComponent().getWorldPosition());
		FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) furnitureEntityToBePlaced.getPhysicalEntityComponent().getAttributes();
		for (GridPoint2 extraTileOffset : attributes.getCurrentLayout().getExtraTiles()) {
			tileLocations.add(location.cpy().add(extraTileOffset));
		}
		tileLocations.add(location);
		primaryLocation = location;
		primaryMaterialType = attributes.getPrimaryMaterialType();

		List<Tag> processedTags = attributes.getFurnitureType().getProcessedTags();
		for (Tag tag : processedTags) {
			if (tag instanceof ConstructionOverrideTag) {
				ConstructionOverrideTag constructionOverrideTag = (ConstructionOverrideTag)tag;
				for (String arg : constructionOverrideTag.getArgs()) {
					ConstructionOverrideTag.ConstructionOverrideSetting setting = ConstructionOverrideTag.ConstructionOverrideSetting.valueOf(arg);
					this.constructionOverrideSettings.add(setting);
				}
			}
		}

		List<QuantifiedItemType> requirements = attributes.getFurnitureType().getRequirements().get(primaryMaterialType);
		if (requirements == null) {
			Logger.error("Constructing " + attributes.getFurnitureType().getName() + " from  " + attributes.getPrimaryMaterialType() + " which has no requirements");
			this.requirements = new ArrayList<>();
		} else {
			this.requirements = QuantifiedItemTypeWithMaterial.convert(requirements);
		}

	}

	@Override
	public ConstructionType getConstructionType() {
		return ConstructionType.FURNITURE_CONSTRUCTION;
	}

	@Override
	public Set<GridPoint2> getTileLocations() {
		return tileLocations;
	}

	@Override
	public Entity getEntity() {
		return furnitureEntityToBePlaced;
	}

	@Override
	public void allocationCancelled(HaulingAllocation allocation) {
		incomingHaulingAllocations.remove(allocation);
	}

	public Entity getFurnitureEntityToBePlaced() {
		return furnitureEntityToBePlaced;
	}

	@Override
	public GridPoint2 getPrimaryLocation() {
		return primaryLocation;
	}

	@Override
	public boolean isAutoCompleted() {
		if (furnitureEntityToBePlaced.getPhysicalEntityComponent().getAttributes() instanceof FurnitureEntityAttributes) {
			FurnitureType furnitureType = ((FurnitureEntityAttributes)furnitureEntityToBePlaced.getPhysicalEntityComponent().getAttributes()).getFurnitureType();
			return furnitureType.isAutoConstructed();
		} else {
			return false;
		}
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		if (savedGameStateHolder.constructions.containsKey(getId())) {
			return;
		}
		super.writeTo(savedGameStateHolder);
		JSONObject asJson = savedGameStateHolder.constructionsJson.getJSONObject(savedGameStateHolder.constructionsJson.size() - 1);

		if (furnitureEntityToBePlaced != null) {
			furnitureEntityToBePlaced.writeTo(savedGameStateHolder); // Needs writing as it is not part of the current set of entities, yet
			asJson.put("entity", furnitureEntityToBePlaced.getId());
		}

		if (!tileLocations.isEmpty()) {
			JSONArray locationsJson = new JSONArray(tileLocations.size());
			for (GridPoint2 tileLocation : tileLocations) {
				locationsJson.add(JSONUtils.toJSON(tileLocation));
			}
			asJson.put("locations", locationsJson);
		}

		if (primaryLocation != null) {
			asJson.put("primaryLocation", JSONUtils.toJSON(primaryLocation));
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		Long entityId = asJson.getLong("entity");
		if (entityId != null) {
			furnitureEntityToBePlaced = savedGameStateHolder.entities.get(entityId);
			if (furnitureEntityToBePlaced == null) {
				throw new InvalidSaveException("Could not find entity by ID " + entityId);
			}
		}

		JSONArray locationsJson = asJson.getJSONArray("locations");
		for (int cursor = 0; cursor < locationsJson.size(); cursor++) {
			tileLocations.add(JSONUtils.gridPoint2(locationsJson.getJSONObject(cursor)));
		}

		this.primaryLocation = JSONUtils.gridPoint2(asJson.getJSONObject("primaryLocation"));
	}

	@Override
	public String getFurnitureTypeI18nKey() {
		FurnitureEntityAttributes furnitureEntityAttributes = (FurnitureEntityAttributes) getEntity().getPhysicalEntityComponent().getAttributes();
		return furnitureEntityAttributes.getFurnitureType().getI18nKey();
	}
}
