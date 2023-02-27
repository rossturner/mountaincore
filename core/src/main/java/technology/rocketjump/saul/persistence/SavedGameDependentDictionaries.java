package technology.rocketjump.saul.persistence;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.assets.FloorTypeDictionary;
import technology.rocketjump.saul.assets.WallTypeDictionary;
import technology.rocketjump.saul.assets.entities.CompleteAssetDictionary;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.cooking.CookingRecipeDictionary;
import technology.rocketjump.saul.crafting.CraftingOutputQualityDictionary;
import technology.rocketjump.saul.crafting.CraftingRecipeDictionary;
import technology.rocketjump.saul.entities.ai.goap.GoalDictionary;
import technology.rocketjump.saul.entities.ai.goap.ScheduleDictionary;
import technology.rocketjump.saul.entities.ai.goap.actions.ActionDictionary;
import technology.rocketjump.saul.entities.components.ComponentDictionary;
import technology.rocketjump.saul.entities.components.StatusEffectDictionary;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureLayoutDictionary;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.saul.entities.dictionaries.vehicle.VehicleTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.saul.entities.model.physical.creature.body.BodyStructureDictionary;
import technology.rocketjump.saul.entities.model.physical.creature.body.organs.OrganDefinitionDictionary;
import technology.rocketjump.saul.entities.model.physical.effect.OngoingEffectTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.mechanism.MechanismTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesDictionary;
import technology.rocketjump.saul.entities.tags.TagDictionary;
import technology.rocketjump.saul.environment.DailyWeatherTypeDictionary;
import technology.rocketjump.saul.environment.WeatherTypeDictionary;
import technology.rocketjump.saul.invasions.InvasionDefinitionDictionary;
import technology.rocketjump.saul.jobs.CraftingTypeDictionary;
import technology.rocketjump.saul.jobs.JobStore;
import technology.rocketjump.saul.jobs.JobTypeDictionary;
import technology.rocketjump.saul.jobs.SkillDictionary;
import technology.rocketjump.saul.mapping.tile.designation.DesignationDictionary;
import technology.rocketjump.saul.materials.DynamicMaterialFactory;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.military.SquadFormationDictionary;
import technology.rocketjump.saul.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.saul.production.StockpileGroupDictionary;
import technology.rocketjump.saul.rooms.RoomStore;
import technology.rocketjump.saul.rooms.RoomTypeDictionary;
import technology.rocketjump.saul.rooms.components.RoomComponentDictionary;
import technology.rocketjump.saul.settlement.trading.TradeCaravanDefinitionDictionary;
import technology.rocketjump.saul.sprites.BridgeTypeDictionary;

/**
 * This class is used when loading a saved game for loaded data to allow for loading of dependent data
 */
@Singleton
public class SavedGameDependentDictionaries {

	public final DynamicMaterialFactory dynamicMaterialFactory;
	public final GameMaterialDictionary gameMaterialDictionary;
	public final CraftingOutputQualityDictionary craftingOutputQualityDictionary;
	public final MessageDispatcher messageDispatcher;
	public final SkillDictionary skillDictionary;
	public final JobTypeDictionary jobTypeDictionary;
	public final ItemTypeDictionary itemTypeDictionary;
	public final FloorTypeDictionary floorTypeDictionary;
	public final CookingRecipeDictionary cookingRecipeDictionary;
	public final ComponentDictionary componentDictionary;
	public final StatusEffectDictionary statusEffectDictionary;
	public final CraftingTypeDictionary craftingTypeDictionary;
	public final CraftingRecipeDictionary craftingRecipeDictionary;
	public final CompleteAssetDictionary completeAssetDictionary;
	public final GoalDictionary goalDictionary;
	public final ScheduleDictionary scheduleDictionary;
	public final RoomStore roomStore;
	public final ActionDictionary actionDictionary;
	public final FurnitureTypeDictionary furnitureTypeDictionary;
	public final FurnitureLayoutDictionary furnitureLayoutDictionary;
	public final VehicleTypeDictionary vehicleTypeDictionary;
	public final PlantSpeciesDictionary plantSpeciesDictionary;
	public final WallTypeDictionary wallTypeDictionary;
	public final RoomTypeDictionary roomTypeDictionary;
	public final RoomComponentDictionary roomComponentDictionary;
	public final DesignationDictionary designationDictionary;
	public final StockpileGroupDictionary stockpileGroupDictionary;
	public final TagDictionary tagDictionary;
	public final SoundAssetDictionary soundAssetDictionary;
	public final BridgeTypeDictionary bridgeTypeDictionary;
	public final JobStore jobStore;
	public final ParticleEffectTypeDictionary particleEffectTypeDictionary;
	public final OngoingEffectTypeDictionary ongoingEffectTypeDictionary;
	public final WeatherTypeDictionary weatherTypeDictionary;
	public final DailyWeatherTypeDictionary dailyWeatherTypeDictionary;
	public final MechanismTypeDictionary mechanismTypeDictionary;
	public final BodyStructureDictionary bodyStructureDictionary;
	public final OrganDefinitionDictionary organDefinitionDictionary;
	public final RaceDictionary raceDictionary;
	public final SquadFormationDictionary squadFormationDictionary;
	public final InvasionDefinitionDictionary invasionDefinitionDictionary;
	public final TradeCaravanDefinitionDictionary tradeCaravanDefinitionDictionary;

	@Inject
	public SavedGameDependentDictionaries(DynamicMaterialFactory dynamicMaterialFactory, GameMaterialDictionary gameMaterialDictionary,
										  CraftingOutputQualityDictionary craftingOutputQualityDictionary,
										  MessageDispatcher messageDispatcher, SkillDictionary skillDictionary,
										  JobTypeDictionary jobTypeDictionary, ItemTypeDictionary itemTypeDictionary, FloorTypeDictionary floorTypeDictionary,
										  CookingRecipeDictionary cookingRecipeDictionary, ComponentDictionary componentDictionary,
										  StatusEffectDictionary statusEffectDictionary, CraftingTypeDictionary craftingTypeDictionary,
										  CraftingRecipeDictionary craftingRecipeDictionary, CompleteAssetDictionary completeAssetDictionary,
										  GoalDictionary goalDictionary, ScheduleDictionary scheduleDictionary, RoomStore roomStore,
										  ActionDictionary actionDictionary, FurnitureTypeDictionary furnitureTypeDictionary,
										  FurnitureLayoutDictionary furnitureLayoutDictionary, VehicleTypeDictionary vehicleTypeDictionary, PlantSpeciesDictionary plantSpeciesDictionary,
										  WallTypeDictionary wallTypeDictionary, RoomTypeDictionary roomTypeDictionary,
										  RoomComponentDictionary roomComponentDictionary, DesignationDictionary designationDictionary,
										  StockpileGroupDictionary stockpileGroupDictionary, TagDictionary tagDictionary,
										  SoundAssetDictionary soundAssetDictionary, BridgeTypeDictionary bridgeTypeDictionary,
										  JobStore jobStore, ParticleEffectTypeDictionary particleEffectTypeDictionary,
										  OngoingEffectTypeDictionary ongoingEffectTypeDictionary,
										  WeatherTypeDictionary weatherTypeDictionary, DailyWeatherTypeDictionary dailyWeatherTypeDictionary,
										  MechanismTypeDictionary mechanismTypeDictionary, BodyStructureDictionary bodyStructureDictionary,
										  OrganDefinitionDictionary organDefinitionDictionary, RaceDictionary raceDictionary,
										  SquadFormationDictionary squadFormationDictionary, InvasionDefinitionDictionary invasionDefinitionDictionary,
										  TradeCaravanDefinitionDictionary tradeCaravanDefinitionDictionary) {
		this.dynamicMaterialFactory = dynamicMaterialFactory;
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.craftingOutputQualityDictionary = craftingOutputQualityDictionary;
		this.messageDispatcher = messageDispatcher;
		this.skillDictionary = skillDictionary;
		this.jobTypeDictionary = jobTypeDictionary;
		this.itemTypeDictionary = itemTypeDictionary;
		this.floorTypeDictionary = floorTypeDictionary;
		this.cookingRecipeDictionary = cookingRecipeDictionary;
		this.componentDictionary = componentDictionary;
		this.statusEffectDictionary = statusEffectDictionary;
		this.craftingTypeDictionary = craftingTypeDictionary;
		this.craftingRecipeDictionary = craftingRecipeDictionary;
		this.completeAssetDictionary = completeAssetDictionary;
		this.goalDictionary = goalDictionary;
		this.scheduleDictionary = scheduleDictionary;
		this.roomStore = roomStore;
		this.actionDictionary = actionDictionary;
		this.furnitureTypeDictionary = furnitureTypeDictionary;
		this.furnitureLayoutDictionary = furnitureLayoutDictionary;
		this.vehicleTypeDictionary = vehicleTypeDictionary;
		this.plantSpeciesDictionary = plantSpeciesDictionary;
		this.wallTypeDictionary = wallTypeDictionary;
		this.roomTypeDictionary = roomTypeDictionary;
		this.roomComponentDictionary = roomComponentDictionary;
		this.designationDictionary = designationDictionary;
		this.stockpileGroupDictionary = stockpileGroupDictionary;
		this.tagDictionary = tagDictionary;
		this.soundAssetDictionary = soundAssetDictionary;
		this.bridgeTypeDictionary = bridgeTypeDictionary;
		this.jobStore = jobStore;
		this.particleEffectTypeDictionary = particleEffectTypeDictionary;
		this.ongoingEffectTypeDictionary = ongoingEffectTypeDictionary;
		this.weatherTypeDictionary = weatherTypeDictionary;
		this.dailyWeatherTypeDictionary = dailyWeatherTypeDictionary;
		this.mechanismTypeDictionary = mechanismTypeDictionary;
		this.bodyStructureDictionary = bodyStructureDictionary;
		this.organDefinitionDictionary = organDefinitionDictionary;
		this.raceDictionary = raceDictionary;
		this.squadFormationDictionary = squadFormationDictionary;
		this.invasionDefinitionDictionary = invasionDefinitionDictionary;
		this.tradeCaravanDefinitionDictionary = tradeCaravanDefinitionDictionary;
	}
}
