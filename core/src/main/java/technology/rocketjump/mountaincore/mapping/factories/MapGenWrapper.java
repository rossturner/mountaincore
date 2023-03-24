package technology.rocketjump.mountaincore.mapping.factories;

import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.Inject;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantSpeciesDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantSpeciesType;
import technology.rocketjump.mountaincore.mapgen.generators.GameMapGenerator;
import technology.rocketjump.mountaincore.mapgen.model.input.*;
import technology.rocketjump.mountaincore.mapgen.model.output.GameMap;
import technology.rocketjump.mountaincore.materials.GameMaterialDictionary;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * This class is intended as a wrapper around the (once-separate) mapgen package code
 */
public class MapGenWrapper {

	private final GameMaterialDictionary materialDictionary;
	private final PlantSpeciesDictionary plantSpeciesDictionary;

	@Inject
	public MapGenWrapper(GameMaterialDictionary materialDictionary, PlantSpeciesDictionary plantSpeciesDictionary) {
		this.materialDictionary = materialDictionary;
		this.plantSpeciesDictionary = plantSpeciesDictionary;
	}

	public GameMap createUsingLibrary(long worldSeed, int worldWidth, int worldHeight) {
		Logger.info("Generating map with seed: " + worldSeed + ", width: " + worldWidth + ", height: " + worldHeight);
		long startTime = System.currentTimeMillis();
		GameMapGenerationParams generationParams = createMapGenerationParams(worldWidth, worldHeight);
		Random random = new RandomXS128(worldSeed);
		GameMapGenerator generator = new GameMapGenerator(generationParams, random);

		GameMap mapGenGameMap = generator.completeGeneration();
		long endTime = System.currentTimeMillis();
		Logger.info("Map generation took " + (endTime - startTime) + "ms");

		return mapGenGameMap;
	}

	private GameMapGenerationParams createMapGenerationParams(int width, int height) {
		// MODDING expose this, also give some controls into it

		Map<String, OreType> oreNameMap = new HashMap<>();

		for (GameMaterial oreMaterial : materialDictionary.getByType(GameMaterialType.ORE)) {
			oreNameMap.put(oreMaterial.getMaterialName(),
					new OreType(oreMaterial.getMaterialName(), oreMaterial.getColor(), oreMaterial.getPrevalence()));
		}

		GameMapGenerationParams generationParams = new GameMapGenerationParams(width, height);
//		generationParams.setRatioOfMountains(0.25f);

		for (GameMaterial rockMaterial : materialDictionary.getByType(GameMaterialType.STONE)) {
			RockType rockType = new RockType(
					rockMaterial.getRockGroup(), rockMaterial.getMaterialName(), rockMaterial.getColor(), rockMaterial.getPrevalence()
			);
			for (String oreName : rockMaterial.getOreNames()) {
				OreType oreType = oreNameMap.get(oreName);
				if (oreType == null) {
					Logger.error("Could not find ore with name " + oreName + " for " + rockMaterial.getMaterialName());
					continue;
				}
				rockType.addOreType(oreType);
			}
			generationParams.addRockTypes(rockType);
		}
		for (GameMaterial gemMaterial : materialDictionary.getByType(GameMaterialType.GEM)) {
			GemType gemType = new GemType(gemMaterial.getRockGroup(), gemMaterial.getMaterialName(), gemMaterial.getColor(), gemMaterial.getPrevalence());
			generationParams.addGemTypes(gemType);
		}
		for (PlantSpecies treeSpecies : plantSpeciesDictionary.getBySpeciesType(PlantSpeciesType.TREE)) {
			TreeType treeType = new TreeType(treeSpecies.getSpeciesName());
//			if (treeSpecies.isEvergreen()) {
//				treeType.setMinYPosition(0.7f);
//			} else {
//				treeType.setMaxYPosition(0.8f);
//			}
			generationParams.getTreeTypes().add(treeType);
		}
		for (PlantSpecies shrubSpecies : plantSpeciesDictionary.getBySpeciesType(PlantSpeciesType.SHRUB)) {
			generationParams.getShrubTypes().add(new ShrubType(shrubSpecies.getSpeciesName(), shrubSpecies.anyStageHasFruit()));
		}

		for (PlantSpecies mushroomSpecies : plantSpeciesDictionary.getBySpeciesType(PlantSpeciesType.MUSHROOM)) {
			generationParams.getMushroomTypes().add(new MushroomType(mushroomSpecies.getSpeciesName(), mushroomSpecies.getOccurenceWeight()));
		}
		for (PlantSpecies mushroomTreeSpecies : plantSpeciesDictionary.getBySpeciesType(PlantSpeciesType.MUSHROOM_TREE)) {
			generationParams.getMushroomTypes().add(new MushroomType(mushroomTreeSpecies.getSpeciesName(), mushroomTreeSpecies.getOccurenceWeight()));
		}


		return generationParams;
	}

}
