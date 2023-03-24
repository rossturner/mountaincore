package technology.rocketjump.mountaincore.materials.model;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.junit.Before;
import org.mockito.Mock;
import technology.rocketjump.mountaincore.assets.FloorTypeDictionary;
import technology.rocketjump.mountaincore.assets.WallTypeDictionary;
import technology.rocketjump.mountaincore.assets.entities.CompleteAssetDictionary;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.cooking.CookingRecipeDictionary;
import technology.rocketjump.mountaincore.crafting.CraftingOutputQualityDictionary;
import technology.rocketjump.mountaincore.crafting.CraftingRecipeDictionary;
import technology.rocketjump.mountaincore.entities.ai.goap.GoalDictionary;
import technology.rocketjump.mountaincore.entities.ai.goap.ScheduleDictionary;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.ActionDictionary;
import technology.rocketjump.mountaincore.entities.components.ComponentDictionary;
import technology.rocketjump.mountaincore.entities.components.StatusEffectDictionary;
import technology.rocketjump.mountaincore.entities.dictionaries.furniture.FurnitureLayoutDictionary;
import technology.rocketjump.mountaincore.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.mountaincore.entities.dictionaries.vehicle.VehicleTypeDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.creature.body.BodyStructureDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.creature.body.organs.OrganDefinitionDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.effect.OngoingEffectTypeDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.mechanism.MechanismTypeDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantSpeciesDictionary;
import technology.rocketjump.mountaincore.entities.tags.TagDictionary;
import technology.rocketjump.mountaincore.environment.DailyWeatherTypeDictionary;
import technology.rocketjump.mountaincore.environment.WeatherTypeDictionary;
import technology.rocketjump.mountaincore.invasions.InvasionDefinitionDictionary;
import technology.rocketjump.mountaincore.jobs.CraftingTypeDictionary;
import technology.rocketjump.mountaincore.jobs.JobStore;
import technology.rocketjump.mountaincore.jobs.JobTypeDictionary;
import technology.rocketjump.mountaincore.jobs.SkillDictionary;
import technology.rocketjump.mountaincore.mapping.tile.designation.DesignationDictionary;
import technology.rocketjump.mountaincore.materials.DynamicMaterialFactory;
import technology.rocketjump.mountaincore.materials.GameMaterialDictionary;
import technology.rocketjump.mountaincore.military.SquadFormationDictionary;
import technology.rocketjump.mountaincore.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.production.StockpileGroupDictionary;
import technology.rocketjump.mountaincore.rooms.RoomStore;
import technology.rocketjump.mountaincore.rooms.RoomTypeDictionary;
import technology.rocketjump.mountaincore.rooms.components.RoomComponentDictionary;
import technology.rocketjump.mountaincore.settlement.trading.TradeCaravanDefinitionDictionary;
import technology.rocketjump.mountaincore.sprites.BridgeTypeDictionary;

public class PersistenceTestHarness {

	protected  SavedGameStateHolder stateHolder;

	protected SavedGameDependentDictionaries dictionaries;
	@Mock
	protected DynamicMaterialFactory mockDynamicMaterialFactory;
	@Mock
	protected MessageDispatcher mockMessageDispatcher;
	@Mock
	protected GameMaterialDictionary mockMaterialDictionary;
	@Mock
	protected SkillDictionary mockSkillDictionary;
	@Mock
	protected ItemTypeDictionary mockItemTypeDictionary;
	@Mock
	protected CookingRecipeDictionary mockCookingRecipeDictionary;
	@Mock
	protected FloorTypeDictionary mockFloorTypeDictionary;
	@Mock
	protected ComponentDictionary mockComponentDictionary;
	@Mock
	protected CraftingTypeDictionary mockCraftingTypeDictionary;
	@Mock
	protected CraftingRecipeDictionary mockCraftingRecipeDictionary;
	@Mock
	protected CompleteAssetDictionary mockCompleteAssetDictionary;
	@Mock
	protected GoalDictionary mockGoalDictionary;
	@Mock
	protected ScheduleDictionary mockScheduleDictionary;
	@Mock
	protected RoomStore mockRoomStore;
	@Mock
	protected ActionDictionary mockActionDictionary;
	@Mock
	protected FurnitureTypeDictionary mockFurnitureTypeDictionary;
	@Mock
	protected FurnitureLayoutDictionary mockFurnitureLayoutDictionary;
	@Mock
	protected PlantSpeciesDictionary mockPlantSpeciesDictionary;
	@Mock
	protected WallTypeDictionary mockWallTypeDictionary;
	@Mock
	protected RoomTypeDictionary mockRoomTypeDictionary;
	@Mock
	protected RoomComponentDictionary mockRoomComponentDictionary;
	@Mock
	protected DesignationDictionary mockDesignationDictionary;
	@Mock
	protected StockpileGroupDictionary mockStockpileGroupDictionary;
	@Mock
	protected TagDictionary mockTagDictionary;
	@Mock
	protected JobTypeDictionary mockJobTypeDictionary;
	@Mock
	protected StatusEffectDictionary mockStatusEffectDictionary;
	@Mock
	protected SoundAssetDictionary mockSoundAssetDictionary;
	@Mock
	protected BridgeTypeDictionary mockBridgeTypeDictionary;
	@Mock
	protected JobStore mockJobStore;
	@Mock
	private ParticleEffectTypeDictionary mockParticleEffectTypeDictionary;
	@Mock
	private OngoingEffectTypeDictionary mockOngoingEffectTypeDictionary;
	@Mock
	private WeatherTypeDictionary mockWeatherTypeDictionary;
	@Mock
	private DailyWeatherTypeDictionary mockDailyWeatherTypeDictionary;
	@Mock
	private MechanismTypeDictionary mockMechanismTypeDictionary;
	@Mock
	private BodyStructureDictionary mockBodyStructureDictionary;
	@Mock
	private RaceDictionary mockRaceDictionary;
	@Mock
	private OrganDefinitionDictionary mockOrganDefinitionDictionary;
	@Mock
	private CraftingOutputQualityDictionary mockCraftingOutputQualityDictionary;
	@Mock
	private SquadFormationDictionary mockSquadFormationDictionary;
	@Mock
	private InvasionDefinitionDictionary mockInvasionDefinitionDictionary;
	@Mock
	private VehicleTypeDictionary mockVehicleTypeDictionary;
	@Mock
	private TradeCaravanDefinitionDictionary mockTradeCaravanDefinitionDictionary;

	@Before
	public void setup() {
		stateHolder = new SavedGameStateHolder();

		dictionaries = new SavedGameDependentDictionaries(
				mockDynamicMaterialFactory,
				mockMaterialDictionary,
				mockCraftingOutputQualityDictionary,
				mockMessageDispatcher,
				mockSkillDictionary,
				mockJobTypeDictionary,
				mockItemTypeDictionary,
				mockFloorTypeDictionary,
				mockCookingRecipeDictionary,
				mockComponentDictionary,
				mockStatusEffectDictionary,
				mockCraftingTypeDictionary,
				mockCraftingRecipeDictionary,
				mockCompleteAssetDictionary,
				mockGoalDictionary,
				mockScheduleDictionary,
				mockRoomStore,
				mockActionDictionary,
				mockFurnitureTypeDictionary,
				mockFurnitureLayoutDictionary,
				mockVehicleTypeDictionary,
				mockPlantSpeciesDictionary,
				mockWallTypeDictionary,
				mockRoomTypeDictionary,
				mockRoomComponentDictionary,
				mockDesignationDictionary,
				mockStockpileGroupDictionary,
				mockTagDictionary,
				mockSoundAssetDictionary,
				mockBridgeTypeDictionary,
				mockJobStore,
				mockParticleEffectTypeDictionary,
				mockOngoingEffectTypeDictionary,
				mockWeatherTypeDictionary,
				mockDailyWeatherTypeDictionary,
				mockMechanismTypeDictionary,
				mockBodyStructureDictionary,
				mockOrganDefinitionDictionary,
				mockRaceDictionary,
				mockSquadFormationDictionary,
				mockInvasionDefinitionDictionary,
				mockTradeCaravanDefinitionDictionary);

	}

}
