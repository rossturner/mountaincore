package technology.rocketjump.saul.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.inject.Inject;
import technology.rocketjump.saul.assets.FloorTypeDictionary;
import technology.rocketjump.saul.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.cooking.CookingRecipeDictionary;
import technology.rocketjump.saul.crafting.CraftingOutputQualityDictionary;
import technology.rocketjump.saul.crafting.CraftingRecipeDictionary;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesDictionary;
import technology.rocketjump.saul.jobs.CraftingTypeDictionary;
import technology.rocketjump.saul.jobs.JobStore;
import technology.rocketjump.saul.jobs.JobTypeDictionary;
import technology.rocketjump.saul.jobs.SkillDictionary;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.saul.production.StockpileComponentUpdater;
import technology.rocketjump.saul.production.StockpileGroupDictionary;

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
