package technology.rocketjump.mountaincore.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.inject.Inject;
import technology.rocketjump.mountaincore.assets.FloorTypeDictionary;
import technology.rocketjump.mountaincore.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.cooking.CookingRecipeDictionary;
import technology.rocketjump.mountaincore.crafting.CraftingOutputQualityDictionary;
import technology.rocketjump.mountaincore.crafting.CraftingRecipeDictionary;
import technology.rocketjump.mountaincore.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantSpeciesDictionary;
import technology.rocketjump.mountaincore.jobs.CraftingTypeDictionary;
import technology.rocketjump.mountaincore.jobs.JobStore;
import technology.rocketjump.mountaincore.jobs.JobTypeDictionary;
import technology.rocketjump.mountaincore.jobs.SkillDictionary;
import technology.rocketjump.mountaincore.materials.GameMaterialDictionary;
import technology.rocketjump.mountaincore.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.mountaincore.production.StockpileComponentUpdater;
import technology.rocketjump.mountaincore.production.StockpileGroupDictionary;

public class TagProcessingUtils {

	public final MessageDispatcher messageDispatcher;
	public final EntityAssetTypeDictionary entityAssetTypeDictionary;
	public final FloorTypeDictionary floorTypeDictionary;
	public final ItemTypeDictionary itemTypeDictionary;
	public final GameMaterialDictionary materialDictionary;
	public final CraftingOutputQualityDictionary craftingOutputQualityDictionary;
	public final PlantSpeciesDictionary plantSpeciesDictionary;
	public final StockpileGroupDictionary stockpileGroupDictionary;
	public final CookingRecipeDictionary cookingRecipeDictionary;
	public final SkillDictionary skillDictionary;
	public final JobTypeDictionary jobTypeDictionary;
	public final CraftingTypeDictionary craftingTypeDictionary;
	public final CraftingRecipeDictionary craftingRecipeDictionary;
	public final FurnitureTypeDictionary furnitureTypeDictionary;
	public final SoundAssetDictionary soundAssetDictionary;
	public final JobStore jobStore;
	public final ParticleEffectTypeDictionary particleEffectTypeDictionary;
	public final RaceDictionary raceDictionary;
	public final StockpileComponentUpdater stockpileComponentUpdater;

	@Inject
	public TagProcessingUtils(MessageDispatcher messageDispatcher, EntityAssetTypeDictionary entityAssetTypeDictionary, FloorTypeDictionary floorTypeDictionary,
							  ItemTypeDictionary itemTypeDictionary, GameMaterialDictionary materialDictionary, CraftingOutputQualityDictionary craftingOutputQualityDictionary, PlantSpeciesDictionary plantSpeciesDictionary,
							  StockpileGroupDictionary stockpileGroupDictionary, CookingRecipeDictionary cookingRecipeDictionary,
							  SkillDictionary skillDictionary, JobTypeDictionary jobTypeDictionary,
							  CraftingTypeDictionary craftingTypeDictionary, CraftingRecipeDictionary craftingRecipeDictionary, FurnitureTypeDictionary furnitureTypeDictionary,
							  SoundAssetDictionary soundAssetDictionary, JobStore jobStore,
							  ParticleEffectTypeDictionary particleEffectTypeDictionary, RaceDictionary raceDictionary, StockpileComponentUpdater stockpileComponentUpdater) {
		this.messageDispatcher = messageDispatcher;
		this.entityAssetTypeDictionary = entityAssetTypeDictionary;
		this.floorTypeDictionary = floorTypeDictionary;
		this.itemTypeDictionary = itemTypeDictionary;
		this.materialDictionary = materialDictionary;
		this.craftingOutputQualityDictionary = craftingOutputQualityDictionary;
		this.plantSpeciesDictionary = plantSpeciesDictionary;
		this.stockpileGroupDictionary = stockpileGroupDictionary;
		this.cookingRecipeDictionary = cookingRecipeDictionary;
		this.jobTypeDictionary = jobTypeDictionary;
		this.skillDictionary = skillDictionary;
		this.craftingTypeDictionary = craftingTypeDictionary;
		this.craftingRecipeDictionary = craftingRecipeDictionary;
		this.furnitureTypeDictionary = furnitureTypeDictionary;
		this.soundAssetDictionary = soundAssetDictionary;
		this.jobStore = jobStore;
		this.particleEffectTypeDictionary = particleEffectTypeDictionary;
		this.raceDictionary = raceDictionary;
		this.stockpileComponentUpdater = stockpileComponentUpdater;
	}
}
