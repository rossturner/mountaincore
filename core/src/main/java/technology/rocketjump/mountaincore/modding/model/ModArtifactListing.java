package technology.rocketjump.mountaincore.modding.model;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.furniture.model.FurnitureEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.mechanism.model.MechanismEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetType;
import technology.rocketjump.mountaincore.assets.entities.model.TemplateAnimationScript;
import technology.rocketjump.mountaincore.assets.entities.plant.model.PlantEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.vehicle.model.VehicleEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.wallcap.model.WallCapAsset;
import technology.rocketjump.mountaincore.assets.model.ChannelType;
import technology.rocketjump.mountaincore.assets.model.FloorType;
import technology.rocketjump.mountaincore.assets.model.OverlapType;
import technology.rocketjump.mountaincore.assets.model.WallType;
import technology.rocketjump.mountaincore.audio.model.SoundAsset;
import technology.rocketjump.mountaincore.cooking.model.CookingRecipe;
import technology.rocketjump.mountaincore.crafting.model.CraftingOutputQuality;
import technology.rocketjump.mountaincore.crafting.model.CraftingRecipe;
import technology.rocketjump.mountaincore.entities.ai.goap.Goal;
import technology.rocketjump.mountaincore.entities.ai.goap.Schedule;
import technology.rocketjump.mountaincore.entities.factories.names.NameGenerationDescriptor;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Race;
import technology.rocketjump.mountaincore.entities.model.physical.creature.body.BodyStructure;
import technology.rocketjump.mountaincore.entities.model.physical.creature.body.organs.OrganDefinition;
import technology.rocketjump.mountaincore.entities.model.physical.effect.OngoingEffectType;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.entities.model.physical.mechanism.MechanismType;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.mountaincore.entities.model.physical.vehicle.VehicleType;
import technology.rocketjump.mountaincore.environment.model.DailyWeatherType;
import technology.rocketjump.mountaincore.environment.model.WeatherType;
import technology.rocketjump.mountaincore.invasions.model.InvasionDefinition;
import technology.rocketjump.mountaincore.jobs.model.CraftingType;
import technology.rocketjump.mountaincore.jobs.model.JobType;
import technology.rocketjump.mountaincore.jobs.model.Skill;
import technology.rocketjump.mountaincore.mapping.tile.designation.Designation;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.modding.processing.*;
import technology.rocketjump.mountaincore.modding.validation.*;
import technology.rocketjump.mountaincore.particles.model.ParticleEffectType;
import technology.rocketjump.mountaincore.production.StockpileGroup;
import technology.rocketjump.mountaincore.rooms.RoomType;
import technology.rocketjump.mountaincore.settlement.trading.model.TradeCaravanDefinition;
import technology.rocketjump.mountaincore.sprites.model.BridgeType;
import technology.rocketjump.mountaincore.ui.hints.model.Hint;
import technology.rocketjump.mountaincore.ui.i18n.LanguageType;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static technology.rocketjump.mountaincore.modding.model.ModArtifactDefinition.ArtifactCombinationType.ADDITIVE;
import static technology.rocketjump.mountaincore.modding.model.ModArtifactDefinition.ArtifactCombinationType.REPLACES_EXISTING;
import static technology.rocketjump.mountaincore.modding.model.ModArtifactDefinition.ModFileType.*;
import static technology.rocketjump.mountaincore.modding.model.ModArtifactDefinition.OutputType.*;

@Singleton
public class ModArtifactListing {

	private final Map<String, ModArtifactDefinition> byName = new HashMap<>();
	private final List<ModArtifactDefinition> allArtifacts;

	@Inject
	public ModArtifactListing() {
		allArtifacts = Arrays.asList(
				////////// AI //////////
				def("ai", "schedules.json", JSON_ARRAY, Schedule.class,
						"ai", "schedules", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),
				def("ai/goals", "**/*.json", JSON_OBJECT, Goal.class,
						"ai", "goals", JSON_ARRAY, SINGLE_FILE, ADDITIVE, /* TODO Check Goal JSON */ UntypedJsonProcessor.class),

				/////////// Animations //////////
				def("entities/animation", "*.json", JSON_ARRAY, TemplateAnimationScript.class,
						"definitions/animation", "animations", JSON_ARRAY, SINGLE_FILE, ADDITIVE, UntypedJsonProcessor.class),


				/////////// Code //////////
				def("code/build/libs", "*.jar", JAR, null,
						"code", null, JAR, COPY_ORIGINAL_FILES, ADDITIVE, CopyFilesProcessor.class),

				////////// definitions //////////

				def("definitions/bodyStructures", "*.json", JSON_OBJECT, BodyStructure.class,
						"definitions", "bodyStructures", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),
				def("definitions/bodyStructures/organs", "*.json", JSON_ARRAY, OrganDefinition.class,
						"definitions", "organs", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),

				def("definitions/constants", "**.json", JSON_KEY_VALUES, null,
						"definitions", "constants", JSON_OBJECT, SINGLE_FILE, ADDITIVE, UntypedJsonProcessor.class),

				def("definitions/crafting", "cookingRecipes", JSON_ARRAY, CookingRecipe.class,
						"definitions/crafting", "cookingRecipes", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),
				def("definitions/crafting/recipes", "*.json", JSON_ARRAY, CraftingRecipe.class,
						"definitions/crafting", "craftingRecipes", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),
				def("definitions/crafting", "craftingTypes", JSON_ARRAY, CraftingType.class,
						"definitions/crafting", "craftingTypes", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),
				def("definitions/crafting", "craftingOutputQuality", JSON_ARRAY, CraftingOutputQuality.class,
						"definitions/crafting", "craftingOutputQuality", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),

				def("definitions/materials", "*-materials.json", JSON_ARRAY, GameMaterial.class,
						"definitions", "materials", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),

				def("definitions", "designations.json", JSON_ARRAY, Designation.class,
						"definitions", "designations", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),
				def("definitions", "invasions.json", JSON_ARRAY, InvasionDefinition.class,
						"definitions", "invasions", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),
				def("definitions", "jobTypes.json", JSON_ARRAY, JobType.class,
						"definitions/types", "jobTypes", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),
				def("definitions", "skills.json", JSON_ARRAY, Skill.class,
						"definitions/types", "skills", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),
				def("definitions", "tradeCaravan.json", JSON_OBJECT, TradeCaravanDefinition.class,
						"definitions", "tradeCaravans", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),
				def("definitions", "weatherTypes.json", JSON_ARRAY, WeatherType.class,
						"definitions", "weatherTypes", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),

				////////// entities //////////
				def("entities", "entityAssetTypes", JSON_MAP, EntityAssetType.class,
						"definitions/entityAssets", "entityAssetTypes", JSON_MAP, SINGLE_FILE, REPLACES_EXISTING, UntypedJsonProcessor.class),
				def("entities", "renderLayers", JSON_MAP, null,
						"definitions/entityAssets", "renderLayers", JSON_MAP, SINGLE_FILE, REPLACES_EXISTING, UntypedJsonProcessor.class),
				def("entities/furniture", "**/descriptors", JSON_ARRAY, FurnitureEntityAsset.class,
						"definitions/entityAssets", "furnitureEntityAssets", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class,
						ReferencedImagesExist.class, UniqueNames.class),
				def("entities/vehicle", "**/descriptors", JSON_ARRAY, VehicleEntityAsset.class,
						"definitions/entityAssets", "vehicleEntityAssets", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class,
						ReferencedImagesExist.class, UniqueNames.class),
				def("entities/creature", "**/descriptors", JSON_ARRAY, CreatureEntityAsset.class,
						"definitions/entityAssets", "creatureEntityAssets", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class,
						ReferencedImagesExist.class, UniqueNames.class),
				def("entities/item", "**/descriptors", JSON_ARRAY, ItemEntityAsset.class,
						"definitions/entityAssets", "itemEntityAssets", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class,
						ReferencedImagesExist.class, UniqueNames.class),
				def("entities/plant", "**/descriptors", JSON_ARRAY, PlantEntityAsset.class,
						"definitions/entityAssets", "plantEntityAssets", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class,
						ReferencedImagesExist.class, UniqueNames.class),
				def("entities/wallCap", "**/descriptors", JSON_ARRAY, WallCapAsset.class,
						"definitions/entityAssets", "wallCapAssets", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class,
						ReferencedImagesExist.class, UniqueNames.class),
				def("entities/mechanism", "**/descriptors", JSON_ARRAY, MechanismEntityAsset.class,
						"definitions/entityAssets", "mechanismEntityAssets", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class,
						ReferencedImagesExist.class, UniqueNames.class),
				def("entities/plant", "**/*-swatch.png", PNG, null,
						"definitions/plantColorSwatches", null, PNG, COPY_ORIGINAL_FILES, ADDITIVE, CopyFilesProcessor.class, UniqueFilenames.class),
				def("entities/creature", "**/*-swatch.png", PNG, null,
						"definitions/creatureColorSwatches", null, PNG, COPY_ORIGINAL_FILES, ADDITIVE, CopyFilesProcessor.class, UniqueFilenames.class),
				def("entities/creature", "**/race.json", JSON_OBJECT, Race.class,
						"definitions/types", "races", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),
				def("entities/furniture", "**/furnitureLayouts.json", JSON_ARRAY, FurnitureLayout.class,
						"definitions/types", "furnitureLayouts", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),
				def("entities/furniture", "**/furnitureType.json", JSON_OBJECT, FurnitureType.class,
						"definitions/types", "furnitureTypes", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),
				def("entities/vehicle", "**/vehicleType.json", JSON_OBJECT, VehicleType.class,
						"definitions/types", "vehicleTypes", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),
				def("entities/item", "**/itemType.json", JSON_OBJECT, ItemType.class,
						"definitions/types", "itemTypes", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),
				def("entities/plant", "**/plantSpecies.json", JSON_OBJECT, PlantSpecies.class,
						"definitions/types", "plantSpecies", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),
				def("entities/ongoing_effect", "**/effectTypes.json", JSON_ARRAY, OngoingEffectType.class,
						"definitions/types", "ongoingEffectTypes", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),
				def("entities/mechanism", "**/mechanismType.json", JSON_OBJECT, MechanismType.class,
						"definitions/types", "mechanismTypes", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),
				def("entities", "**/*![-swatch].png", PNG_PLUS_NORMALS, null,
						"tilesets", "entities", PACKR_ATLAS_PLUS_NORMALS, SPECIAL, ADDITIVE, TexturePackerProcessor.class,
						UniqueFilenames.class, ReferencedByAssetDescriptor.class, WarnOnMissingNormals.class),

				////////// icons //////////
				def("icons", "**/*.png", PNG, null,
						"tilesets", "gui", PACKR_ATLAS, SPECIAL, ADDITIVE, TexturePackerProcessor.class,
						UniqueFilenames.class),

				////////// masks //////////
				def("masks", "**/*.png", PNG, null,
						"tilesets", "masks", PACKR_ATLAS, SPECIAL, ADDITIVE, TexturePackerProcessor.class,
						UniqueFilenames.class),
				def("masks", "overlapTypes.json", JSON_ARRAY, OverlapType.class,
						"definitions/types", "overlapTypes", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),

				////////// music //////////
				def("music/peaceful", "*.ogg", OGG, null,
						"music/peaceful", null, OGG, COPY_ORIGINAL_FILES, ADDITIVE, CopyFilesProcessor.class),
				def("music/skirmish", "*.ogg", OGG, null,
						"music/skirmish", null, OGG, COPY_ORIGINAL_FILES, ADDITIVE, CopyFilesProcessor.class),
				def("music/invasion", "*.ogg", OGG, null,
						"music/invasion", null, OGG, COPY_ORIGINAL_FILES, ADDITIVE, CopyFilesProcessor.class),
				def("music/invasion_stinger", "*.ogg", OGG, null,
						"music/invasion_stinger", null, OGG, COPY_ORIGINAL_FILES, ADDITIVE, CopyFilesProcessor.class),

				////////// particles //////////
				def("particles/libgdx", "**/*.p", P_FILE, null,
						"definitions/particles", null, P_FILE, COPY_ORIGINAL_FILES, ADDITIVE, CopyFilesProcessor.class),
				def("particles/types", "*.json", JSON_ARRAY, ParticleEffectType.class,
						"definitions/types", "particleTypes", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),
				def("particles/shaders", "**/*.glsl", GLSL, null,
						"definitions/shaders", null, GLSL, COPY_ORIGINAL_FILES, ADDITIVE, CopyFilesProcessor.class),

				////////// rooms //////////
				def("rooms", "roomTypes.json", JSON_ARRAY, RoomType.class,
						"definitions/types", "roomTypes", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),
				def("rooms", "stockpileGroups.json", JSON_ARRAY, StockpileGroup.class,
						"definitions", "stockpileGroups", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),

				////////// settings //////////
				def("settings", "sunlight.json", JSON_OBJECT, null,
						"settings", "sunlight", JSON_ARRAY, SINGLE_FILE, REPLACES_EXISTING, SunlightProcessor.class),
				def("settings", "timeAndDaySettings.json", JSON_OBJECT, null,
						"settings", "timeAndDaySettings", JSON_OBJECT, SINGLE_FILE, REPLACES_EXISTING, UntypedJsonProcessor.class),
				def("settings", "immigrationSettings.json", JSON_OBJECT, null,
						"settings", "immigrationSettings", JSON_OBJECT, SINGLE_FILE, REPLACES_EXISTING, UntypedJsonProcessor.class),
				def("settings", "dailyWeather.json", JSON_ARRAY, DailyWeatherType.class,
						"settings", "dailyWeather", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),

				////////// sounds //////////
					// Do not use anything but .wav for sound effects or else game freezes while parsing ogg/mp3
				def("sounds", "**/*.wav", WAV, null,
						"sounds/data", null, WAV, COPY_ORIGINAL_FILES, ADDITIVE, CopyFilesProcessor.class),
				def("sounds", "**/sound_descriptors.json", JSON_ARRAY, SoundAsset.class,
						"sounds", "soundAssets", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class,
						ReferencedSoundsExist.class, UniqueNames.class),

				////////// terrain //////////
				def("terrain/bridges", "**/bridge-tileset-definition.json", JSON_OBJECT, BridgeType.class,
						"definitions/types", "bridgeTypes", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),
				def("terrain/channels", "**/definition.json", JSON_OBJECT, ChannelType.class,
						"definitions/types", "channelTypes", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),
				def("terrain/floors", "**/*-tileset-definition.json", JSON_OBJECT, FloorType.class,
						"definitions/types", "floorTypes", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class, OverlapTypeExists.class),
				def("terrain/walls", "**/definition.json", JSON_OBJECT, WallType.class,
						"definitions/types", "wallTypes", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),

				def("terrain", "**/*.png", PNG_PLUS_NORMALS, null,
						"tilesets", "terrain", PACKR_ATLAS_PLUS_NORMALS, SPECIAL, ADDITIVE, TexturePackerProcessor.class,
						UniqueFilenames.class, ReferencedByTilesetDefinition.class, WarnOnMissingNormals.class),
				def("terrain", "doorwayEdges.json", JSON_OBJECT, null,
						"terrain", "doorwayEdges", JSON_OBJECT, SINGLE_FILE, REPLACES_EXISTING, UntypedJsonProcessor.class),
				def("terrain", "doorwayClosedEdges.json", JSON_OBJECT, null,
						"terrain", "doorwayClosedEdges", JSON_OBJECT, SINGLE_FILE, REPLACES_EXISTING, UntypedJsonProcessor.class),
				def("terrain", "wallEdges.json", JSON_OBJECT, null,
						"terrain", "wallEdges", JSON_OBJECT, SINGLE_FILE, REPLACES_EXISTING, UntypedJsonProcessor.class),
				def("terrain", "wallLayoutQuadrants.json", JSON_OBJECT, null,
						"terrain", "wallLayoutQuadrants", JSON_OBJECT, SINGLE_FILE, REPLACES_EXISTING, UntypedJsonProcessor.class),

				////////// text //////////
				def("text/credits", "*.csv", CSV, null,
						"text/credits", null, CSV, COPY_ORIGINAL_FILES, ADDITIVE, CopyFilesProcessor.class),
				def("text/names/csv", "*.csv", CSV, null,
						"text/csv", null, CSV, COPY_ORIGINAL_FILES, ADDITIVE, CopyFilesProcessor.class),
				def("text/names", "**/descriptor.json", JSON_OBJECT, NameGenerationDescriptor.class,
						"text", "name-descriptors", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),

				////////// translations //////////
				def("translations", "languages.json", JSON_ARRAY, LanguageType.class,
						"translations", "languages", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),
				def("translations", "*.csv", CSV, null,
						"translations", "collated", CSV, SPECIAL, ADDITIVE, LanguagesCsvProcessor.class),


				////////// ui //////////
				// TODO MODDING this also wants some work
				def("ui/cursors/4k", "*.png", PNG, null,
						"ui/cursors/4k", null, PNG, COPY_ORIGINAL_FILES, ADDITIVE, CopyFilesProcessor.class),
				def("ui/cursors/1080p", "*.png", PNG, null,
						"ui/cursors/1080p", null, PNG, COPY_ORIGINAL_FILES, ADDITIVE, CopyFilesProcessor.class),
				def("ui/fonts", "*.[ot]tf", TTF, null,
						"ui/fonts", null, TTF, COPY_ORIGINAL_FILES, ADDITIVE, CopyFilesProcessor.class),

				def("ui/skin", "menu-skin[0-9]?.*", SKIN_ATLAS, null,
						"ui/skin", "menu-skin", SKIN_ATLAS, SPECIAL, REPLACES_EXISTING, SkinFilesProcessor.class),
				def("ui/skin", "management-skin[0-9]?.*", SKIN_ATLAS, null,
						"ui/skin", "management-skin", SKIN_ATLAS, SPECIAL, REPLACES_EXISTING, SkinFilesProcessor.class),
				def("ui/skin", "main-game-skin.*", SKIN_ATLAS, null,
						"ui/skin", "main-game-skin", SKIN_ATLAS, SPECIAL, REPLACES_EXISTING, SkinFilesProcessor.class),


				def("ui/notifications", "*.png", PNG, null,
						"ui/notifications", null, PNG, COPY_ORIGINAL_FILES, ADDITIVE, CopyFilesProcessor.class),
				def("ui/hints", "*.json", JSON_ARRAY, Hint.class,
						"ui", "hints", JSON_ARRAY, SINGLE_FILE, ADDITIVE, GenericClassTypeProcessor.class),
				def("ui", "minimapSelection", PNG, null,
						"ui", "minimapSelection", PNG, SINGLE_FILE, REPLACES_EXISTING, CopyFilesProcessor.class),
				def("ui", "RJT_LOGO_BLACK", PNG, null,
						"ui", "RJT_LOGO_BLACK", PNG, SINGLE_FILE, REPLACES_EXISTING, CopyFilesProcessor.class),
				def("ui", "uiSettings", JSON_OBJECT, null,
						"ui", "uiSettings", JSON_OBJECT, SINGLE_FILE, REPLACES_EXISTING, UntypedJsonProcessor.class),

				////////// water //////////
				// TODO This could be more generic as well
				def("water", "water.png", PNG, null,
						"water/sprite", "water", PNG, SINGLE_FILE, REPLACES_EXISTING, CopyFilesProcessor.class),
				def("water", "*.glsl", GLSL, null,
						"water/shaders", null, GLSL, COPY_ORIGINAL_FILES, ADDITIVE, CopyFilesProcessor.class),
				def("water", "water_NORMALS.png", PNG, null,
						"water/normal_sprite", "water_NORMALS", PNG, SINGLE_FILE, REPLACES_EXISTING, CopyFilesProcessor.class),
				def("water", "wave_mask.png", PNG, null,
						"water/mask", "wave_mask", PNG, SINGLE_FILE, REPLACES_EXISTING, CopyFilesProcessor.class)

		);

		for (ModArtifactDefinition artifactDef : allArtifacts) {
			byName.put(artifactDef.getName(), artifactDef);
		}

		byName.get("tilesets/entities.atlas").setPackJsonPath("entities/pack.json");
		byName.get("tilesets/terrain.atlas").setPackJsonPath("terrain/pack.json");
		byName.get("tilesets/gui.atlas").setPackJsonPath("icons/pack.json");
		byName.get("tilesets/masks.atlas").setPackJsonPath("terrain/pack.json");
	}

	private ModArtifactDefinition def(String modDir, String inputFileNameMatcher, ModArtifactDefinition.ModFileType inputFileType, Class<?> classType, String assetDir, String outputFileName, ModArtifactDefinition.ModFileType outputFileType, ModArtifactDefinition.OutputType outputType,
									  ModArtifactDefinition.ArtifactCombinationType combinationType,
									  Class<? extends ModArtifactProcessor> processor, Class<? extends ModArtifactValidator>... validators) {
		return new ModArtifactDefinition(assetDir, outputFileName, outputType, outputFileType, classType,
				modDir, inputFileNameMatcher, inputFileType, combinationType, processor, validators);
	}

	public List<ModArtifactDefinition> getAll() {
		return allArtifacts;
	}

	public ModArtifactDefinition getByName(String name) {
		return byName.get(name);
	}
}
