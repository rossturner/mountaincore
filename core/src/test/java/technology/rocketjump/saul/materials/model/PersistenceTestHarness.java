package technology.rocketjump.saul.materials.model;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.junit.Before;
import org.mockito.Mock;
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
import technology.rocketjump.saul.jobs.CraftingTypeDictionary;
import technology.rocketjump.saul.jobs.JobStore;
import technology.rocketjump.saul.jobs.JobTypeDictionary;
import technology.rocketjump.saul.jobs.SkillDictionary;
import technology.rocketjump.saul.mapping.tile.designation.DesignationDictionary;
import technology.rocketjump.saul.materials.DynamicMaterialFactory;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.military.SquadFormationDictionary;
import technology.rocketjump.saul.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.rooms.RoomStore;
import technology.rocketjump.saul.rooms.RoomTypeDictionary;
import technology.rocketjump.saul.rooms.StockpileGroupDictionary;
import technology.rocketjump.saul.rooms.components.RoomComponentDictionary;
import technology.rocketjump.saul.sprites.BridgeTypeDictionary;

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
				mockSquadFormationDictionary);

	}

}
