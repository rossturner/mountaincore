package technology.rocketjump.mountaincore.entities.model.physical.plant;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.graphics.Color;
import org.apache.commons.lang3.EnumUtils;
import technology.rocketjump.mountaincore.assets.entities.model.ColoringLayer;
import technology.rocketjump.mountaincore.entities.model.physical.EntityAttributes;
import technology.rocketjump.mountaincore.environment.model.Season;
import technology.rocketjump.mountaincore.jobs.model.Job;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.persistence.EnumParser;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rendering.utils.HexColors;

import java.util.EnumMap;
import java.util.Map;

import static technology.rocketjump.mountaincore.entities.model.physical.plant.PlantSpeciesGrowthStage.PlantSpeciesHarvestType.FARMING;

public class PlantEntityAttributes implements EntityAttributes {

	private long seed;
	private PlantSpecies species;
	private GameMaterial burnedMaterial;

	private Map<ColoringLayer, Color> actualColors = new EnumMap<>(ColoringLayer.class);

	private int growthStageCursor = 0;
	private float growthStageProgress = 0.75f;
	private float growthRate = 1;
	private MoistureState moistureState = MoistureState.WATERED;

	private double seasonProgress = 0;

	private boolean afflictedByPests;
	private Job removePestsJob;

	public PlantEntityAttributes() {

	}

	public PlantEntityAttributes(long seed, PlantSpecies species) {
		this.seed = seed;
		this.species = species;

		updateColors(null);
	}

	/**
	 * @return true if a colour changed from CLEAR to something else or back, necessitating an asset update
	 */
	public boolean updateColors(Season currentSeason) {
		boolean assetUpdateRequired = false;
		PlantSpeciesGrowthStage currentGrowthStage = species.getGrowthStages().get(this.growthStageCursor);
		float progress = growthStageProgress;
		for (Map.Entry<ColoringLayer, SpeciesColor> colorEntry : species.getDefaultColors().entrySet()) {
			SpeciesColor colorToUse = colorEntry.getValue();

			if (currentGrowthStage.getColors().containsKey(colorEntry.getKey())) {
				colorToUse = currentGrowthStage.getColors().get(colorEntry.getKey());
			}

			PlantSeasonSettings seasonSettings = null;

			if (currentSeason != null && species.getSeasons().containsKey(currentSeason)) {
				seasonSettings = species.getSeasons().get(currentSeason);
			}

			if (seasonSettings != null) {
				if (seasonSettings.getColors().containsKey(colorEntry.getKey())) {
					colorToUse = species.getSeasons().get(currentSeason).getColors().get(colorEntry.getKey());
					progress = (float) seasonProgress;
				}
			}

			Color oldColor = actualColors.get(colorEntry.getKey());
			Color newColor = colorToUse.getColor(progress, seed);

			actualColors.put(colorEntry.getKey(), newColor);

			if (Color.CLEAR.equals(oldColor) && !Color.CLEAR.equals(newColor) ||
					!Color.CLEAR.equals(oldColor) && Color.CLEAR.equals(newColor)) {
				assetUpdateRequired = true;
			}
		}
		return assetUpdateRequired;
	}

	@Override
	public long getSeed() {
		return seed;
	}

	@Override
	public Color getColor(ColoringLayer coloringLayer) {
		return actualColors.get(coloringLayer);
	}

	@Override
	public EntityAttributes clone() {
		PlantEntityAttributes cloned = new PlantEntityAttributes(seed, species);

		cloned.actualColors.putAll(this.actualColors);

		cloned.growthStageCursor = this.growthStageCursor;
		cloned.growthRate = this.growthRate;
		cloned.growthStageProgress = this.growthStageProgress;

		return cloned;
	}

	@Override
	public Map<GameMaterialType, GameMaterial> getMaterials() {
		if (burnedMaterial != null) {
			return Map.of(species.getMaterial().getMaterialType(), burnedMaterial);
		} else {
			return Map.of(species.getMaterial().getMaterialType(), species.getMaterial());
		}
	}

	public int getGrowthStageCursor() {
		return growthStageCursor;
	}

	public void setGrowthStageCursor(int growthStageCursor) {
		this.growthStageCursor = growthStageCursor;
		setGrowthStageProgress(0);
	}

	public float getGrowthStageProgress() {
		return growthStageProgress;
	}

	public void setGrowthStageProgress(float growthStageProgress) {
		if (growthStageProgress > 1f) {
			growthStageProgress = 1f;
		}
		this.growthStageProgress = growthStageProgress;
	}

	public float getGrowthRate() {
		return growthRate;
	}

	public void setGrowthRate(float growthRate) {
		this.growthRate = growthRate;
	}

	public MoistureState getMoistureState() {
		return moistureState;
	}

	public void setMoistureState(MoistureState moistureState) {
		this.moistureState = moistureState;
	}

	public PlantSpecies getSpecies() {
		return species;
	}

	public double getSeasonProgress() {
		return seasonProgress;
	}

	public boolean isAfflictedByPests() {
		return afflictedByPests;
	}

	public void setAfflictedByPests(Job removePestsJob) {
		this.afflictedByPests = true;
		this.removePestsJob = removePestsJob;
	}

	public void clearAfflitctedByPests() {
		this.afflictedByPests = false;
		this.removePestsJob = null;
	}

	public Job getRemovePestsJob() {
		return removePestsJob;
	}

	public void setSeasonProgress(double seasonProgress) {
		if (seasonProgress > 1.0) {
			seasonProgress = 1.0;
		}
		this.seasonProgress = seasonProgress;
	}

	public float estimatedProgressToHarvesting() {
		if (species.getPlantType().equals(PlantSpeciesType.CROP)) {
			float totalSeasonsUntilHarvest = 0;
			float seasonProgressCompleted = 0;

			Integer growthStageIterator = 0;
			boolean currentStageReached = false;
			while (growthStageIterator != null) {
				PlantSpeciesGrowthStage iteratorGrowthStage = species.getGrowthStages().get(growthStageIterator);
				if (FARMING.equals(iteratorGrowthStage.getHarvestType())) {
					break;
				}

				totalSeasonsUntilHarvest += iteratorGrowthStage.getSeasonsUntilComplete();
				if (this.growthStageCursor == growthStageIterator) {
					 seasonProgressCompleted += (iteratorGrowthStage.getSeasonsUntilComplete() * this.growthStageProgress);
					currentStageReached = true;
				} else if (!currentStageReached) {
					seasonProgressCompleted += iteratorGrowthStage.getSeasonsUntilComplete();
				}

				growthStageIterator = iteratorGrowthStage.getNextGrowthStage();
			}

			return seasonProgressCompleted / totalSeasonsUntilHarvest;
		} else {
			return 0;
		}
	}

	public boolean isBurned() {
		return burnedMaterial != null;
	}

	public void setBurned(GameMaterial burnedMaterial, Color burnedColor) {
		this.burnedMaterial = burnedMaterial;
		for (Map.Entry<ColoringLayer, SpeciesColor> colorEntry : species.getDefaultColors().entrySet()) {
			if (colorEntry.getKey().equals(ColoringLayer.BRANCHES_COLOR)) {
				actualColors.put(colorEntry.getKey(), burnedColor);
			} else {
				actualColors.put(colorEntry.getKey(), Color.CLEAR);
			}
		}
	}

	public boolean isTree() {
		return getSpecies().getPlantType().equals(PlantSpeciesType.TREE) || getSpecies().getPlantType().equals(PlantSpeciesType.MUSHROOM_TREE);
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("seed", seed);
		asJson.put("species", species.getSpeciesName());

		if (!actualColors.isEmpty()) {
			JSONObject colorsJson = new JSONObject(true);
			for (Map.Entry<ColoringLayer, Color> entry : actualColors.entrySet()) {
				colorsJson.put(entry.getKey().name(), HexColors.toHexString(entry.getValue()));
			}
			asJson.put("colors", colorsJson);
		}
		asJson.put("growthStage", growthStageCursor);
		asJson.put("stageProgress", growthStageProgress);
		asJson.put("growthRate", growthRate);
		if (!moistureState.equals(MoistureState.WATERED)) {
			asJson.put("moistureState", moistureState.name());
		}
		asJson.put("seasonProgress", seasonProgress);
		if (afflictedByPests) {
			asJson.put("afflicted", true);
		}
		if (removePestsJob != null) {
			removePestsJob.writeTo(savedGameStateHolder);
			asJson.put("removePestsJob", removePestsJob.getJobId());
		}
		if (burnedMaterial != null) {
			asJson.put("burnedMaterial", burnedMaterial.getMaterialName());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		seed = asJson.getLongValue("seed");
		species = relatedStores.plantSpeciesDictionary.getByName(asJson.getString("species"));
		if (species == null) {
			throw new InvalidSaveException("Could not find plant species by name " + asJson.getString("species"));
		}
		JSONObject colorsJson = asJson.getJSONObject("colors");
		if (colorsJson != null) {
			for (String coloringLayerName : colorsJson.keySet()) {
				ColoringLayer coloringLayer = EnumUtils.getEnum(ColoringLayer.class, coloringLayerName);
				if (coloringLayer == null) {
					throw new InvalidSaveException("Could not find coloring layer by name " + coloringLayerName);
				}
				Color color = HexColors.get(colorsJson.getString(coloringLayerName));
				actualColors.put(coloringLayer, color);
			}
		}
		growthStageCursor = asJson.getIntValue("growthStage");
		growthStageProgress = asJson.getFloatValue("stageProgress");
		growthRate = asJson.getFloatValue("growthRate");
		moistureState = EnumParser.getEnumValue(asJson, "moistureState", MoistureState.class, MoistureState.WATERED);
		seasonProgress = asJson.getFloatValue("seasonProgress");
		afflictedByPests = asJson.getBooleanValue("afflicted");
		Long removePestsJobId = asJson.getLong("removePestsJob");
		if (removePestsJobId != null) {
			removePestsJob = savedGameStateHolder.jobs.get(removePestsJobId);
			if (removePestsJob == null) {
				throw new InvalidSaveException("Could not find job by ID " + removePestsJobId);
			}
		}

		String burnedMaterialName = asJson.getString("burnedMaterial");
		if (burnedMaterialName != null) {
			this.burnedMaterial = relatedStores.gameMaterialDictionary.getByName(burnedMaterialName);
			if (this.burnedMaterial == null) {
				throw new InvalidSaveException("Could not find material with name " + burnedMaterialName);
			}
		}
	}

}
