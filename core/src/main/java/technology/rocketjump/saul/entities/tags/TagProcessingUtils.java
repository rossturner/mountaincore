package technology.rocketjump.saul.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.inject.Inject;
import technology.rocketjump.saul.assets.FloorTypeDictionary;
import technology.rocketjump.saul.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.cooking.CookingRecipeDictionary;
import technology.rocketjump.saul.crafting.CraftingOutputQualityDictionary;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesDictionary;
import technology.rocketjump.saul.jobs.CraftingTypeDictionary;
import technology.rocketjump.saul.jobs.JobStore;
import technology.rocketjump.saul.jobs.JobTypeDictionary;
import technology.rocketjump.saul.jobs.ProfessionDictionary;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.saul.rooms.StockpileGroupDictionary;

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
	public final ProfessionDictionary professionDictionary;
	public final JobTypeDictionary jobTypeDictionary;
	public final CraftingTypeDictionary craftingTypeDictionary;
	public final FurnitureTypeDictionary furnitureTypeDictionary;
	public final SoundAssetDictionary soundAssetDictionary;
	public final JobStore jobStore;
	public final ParticleEffectTypeDictionary particleEffectTypeDictionary;

	@Inject
	public TagProcessingUtils(MessageDispatcher messageDispatcher, EntityAssetTypeDictionary entityAssetTypeDictionary, FloorTypeDictionary floorTypeDictionary,
							  ItemTypeDictionary itemTypeDictionary, GameMaterialDictionary materialDictionary, CraftingOutputQualityDictionary craftingOutputQualityDictionary, PlantSpeciesDictionary plantSpeciesDictionary,
							  StockpileGroupDictionary stockpileGroupDictionary, CookingRecipeDictionary cookingRecipeDictionary,
							  ProfessionDictionary professionDictionary, JobTypeDictionary jobTypeDictionary,
							  CraftingTypeDictionary craftingTypeDictionary, FurnitureTypeDictionary furnitureTypeDictionary,
							  SoundAssetDictionary soundAssetDictionary, JobStore jobStore, ParticleEffectTypeDictionary particleEffectTypeDictionary) {
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
		this.professionDictionary = professionDictionary;
		this.craftingTypeDictionary = craftingTypeDictionary;
		this.furnitureTypeDictionary = furnitureTypeDictionary;
		this.soundAssetDictionary = soundAssetDictionary;
		this.jobStore = jobStore;
		this.particleEffectTypeDictionary = particleEffectTypeDictionary;
	}
}
