package technology.rocketjump.mountaincore.ui.i18n;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Guice;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import technology.rocketjump.mountaincore.TestModule;
import technology.rocketjump.mountaincore.assets.TextureAtlasRepository;
import technology.rocketjump.mountaincore.assets.model.WallType;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.constants.ConstantsRepo;
import technology.rocketjump.mountaincore.entities.EntityAssetUpdater;
import technology.rocketjump.mountaincore.entities.EntityStore;
import technology.rocketjump.mountaincore.entities.ai.goap.GoalDictionary;
import technology.rocketjump.mountaincore.entities.components.Faction;
import technology.rocketjump.mountaincore.entities.dictionaries.furniture.FurnitureLayoutDictionary;
import technology.rocketjump.mountaincore.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.mountaincore.entities.factories.*;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.LocationComponent;
import technology.rocketjump.mountaincore.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.*;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantSpeciesDictionary;
import technology.rocketjump.mountaincore.environment.GameClock;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.jobs.CraftingTypeDictionary;
import technology.rocketjump.mountaincore.jobs.JobTypeDictionary;
import technology.rocketjump.mountaincore.jobs.SkillDictionary;
import technology.rocketjump.mountaincore.jobs.model.CraftingType;
import technology.rocketjump.mountaincore.jobs.model.Skill;
import technology.rocketjump.mountaincore.jobs.model.SkillType;
import technology.rocketjump.mountaincore.mapping.model.TiledMap;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.mapping.tile.layout.WallLayout;
import technology.rocketjump.mountaincore.mapping.tile.wall.Wall;
import technology.rocketjump.mountaincore.materials.GameMaterialDictionary;
import technology.rocketjump.mountaincore.materials.GameMaterialI18nUpdater;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.ItemMaterialSelectionMessage;
import technology.rocketjump.mountaincore.misc.twitch.TwitchDataStore;
import technology.rocketjump.mountaincore.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.mountaincore.persistence.UserPreferences;
import technology.rocketjump.mountaincore.production.StockpileGroupDictionary;
import technology.rocketjump.mountaincore.rooms.RoomStore;
import technology.rocketjump.mountaincore.rooms.constructions.ConstructionState;
import technology.rocketjump.mountaincore.rooms.constructions.FurnitureConstruction;
import technology.rocketjump.mountaincore.rooms.constructions.WallConstruction;

import java.io.IOException;
import java.util.*;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static technology.rocketjump.mountaincore.materials.model.GameMaterial.NULL_MATERIAL;
import static technology.rocketjump.mountaincore.persistence.UserPreferences.PreferenceKey.LANGUAGE;

@RunWith(MockitoJUnitRunner.class)
public class I18NTranslatorTest {

	private static final long FIXED_SEED = 12345L;
	private I18nTranslator translator;
	@Mock
	private SkillDictionary mockSkillDictionary;
	@Mock
	private EntityAssetUpdater mockEntityAssetUpdater;
	@Mock
	private MessageDispatcher mockMessageDispatcher;
	private GameMaterialDictionary gameMaterialDictionary;
	private ItemTypeDictionary itemTypeDictionary;
	@Mock
	private CraftingType mockCraftingType;
	@Mock
	private TiledMap mockMap;
	@Mock
	private MapTile mockMapTile;
	@Mock
	private Entity mockEntity;
	@Mock
	private GameContext mockGameContext;
	@Mock
	private EntityStore mockEntityStore;
	@Mock
	private TextureAtlasRepository mockTextureAtlasRepository;
	@Mock
	private TextureAtlas mockTextureAtlas;
	@Mock
	private LocationComponent mockLocationComponent;
	@Mock
	private WallType mockWallType;
	@Mock
	private ItemType mockItemType;
	@Mock
	private GoalDictionary mockGoalDictionary;
	@Mock
	private RoomStore mockRoomStore;
	@Mock
	private JobTypeDictionary mockJobTypeDictionary;
	@Mock
	private CraftingTypeDictionary mockCraftingTypeDictionary;
	@Mock
	private SoundAssetDictionary mockSoundAssetDictionary;
	@Mock
	private ConstantsRepo mockConstantsRepo;
	@Mock
	private UserPreferences mockUserPreferences;
	@Mock
	private ItemEntityAttributesFactory mockItemEntityAttributesFactory;
	@Mock
	private TwitchDataStore mockTwitchDataStore;
	@Mock
	private RaceDictionary mockRaceDictionary;
	@Mock
	private ParticleEffectTypeDictionary mockParticleEffectTypeDictionary;
	@Mock
	private GameMaterialDictionary mockMaterialDictionary;
	@Mock
	private GameClock mockClock;

	@Before
	public void setup() throws IOException {
		when(mockUserPreferences.getPreference(eq(LANGUAGE))).thenReturn("en-gb");

		when(mockCraftingTypeDictionary.getByName(Mockito.anyString())).thenReturn(mockCraftingType);
		when(mockWallType.getI18nKey()).thenReturn("WALL.STONE_BLOCK");
		when(mockSkillDictionary.getByName(anyString())).thenReturn(SkillDictionary.UNARMED_COMBAT_SKILL);
		when(mockMaterialDictionary.getByName(anyString())).thenReturn(NULL_MATERIAL);

		I18nRepo i18nRepo = new I18nRepo(mockUserPreferences);

		itemTypeDictionary = new ItemTypeDictionary(mockCraftingTypeDictionary, new StockpileGroupDictionary(), mockSoundAssetDictionary,
				mockConstantsRepo, mockParticleEffectTypeDictionary, mockSkillDictionary, mockMaterialDictionary);
		gameMaterialDictionary = new GameMaterialDictionary();
		new GameMaterialI18nUpdater(i18nRepo, gameMaterialDictionary).preLanguageUpdated();

		translator = new I18nTranslator(i18nRepo, mockEntityStore);



		when(mockGameContext.getRandom()).thenReturn(new RandomXS128(FIXED_SEED));
		when(mockGameContext.getAreaMap()).thenReturn(mockMap);
		when(mockGameContext.getGameClock()).thenReturn(mockClock);
	}

	@Test
	public void describeHumanoid() throws IOException {
		Skill profession = new Skill();
		profession.setI18nKey("PROFESSION.BLACKSMITH");
		profession.setType(SkillType.PROFESSION);
		Entity entity = Guice.createInjector(new TestModule()).getInstance(SettlerFactory.class)
				.create(new Vector2(), profession, null, mockGameContext, false);

		I18nText description = translator.getDescription(entity);

		assertThat(description.toString()).isEqualTo("Holmes Threesmith, journeyman blacksmith");
	}

	@Test
	public void describeItem() throws IOException {
		ItemEntityAttributes attributes = new ItemEntityAttributes(0);
		attributes.setQuantity(1);
		attributes.setItemType(itemTypeDictionary.getByName("Product-Ration"));
		attributes.setMaterial(gameMaterialDictionary.getByName("Rockbread"));

		Entity entity = new ItemEntityFactory(mockMessageDispatcher, gameMaterialDictionary, mockEntityAssetUpdater).create(attributes, new GridPoint2(), true, mockGameContext, Faction.SETTLEMENT);

		I18nText description = translator.getDescription(entity);

		assertThat(description.toString()).isEqualTo("Rockbread ration");
	}

	@Test
	public void describeSackOfVegetables() {
		ItemEntityAttributes attributes = new ItemEntityAttributes(0);
		attributes.setQuantity(2);
		attributes.setItemType(itemTypeDictionary.getByName("Ingredient-Vegetable-Sack"));
		attributes.setMaterial(gameMaterialDictionary.getByName("Potato"));

		Entity entity = new ItemEntityFactory(mockMessageDispatcher, gameMaterialDictionary, mockEntityAssetUpdater).create(attributes, new GridPoint2(), true, mockGameContext, Faction.SETTLEMENT);

		I18nText description = translator.getDescription(entity);

		assertThat(description.toString()).isEqualTo("2 potatoes");
	}

	@Test
	public void describeResources() throws IOException {
		Entity entity = createPileOfLogs();

		I18nText description = translator.getDescription(entity);

		assertThat(description.toString()).isEqualTo("5 oaken logs");
	}

	@Test
	public void describeTree() throws IOException {
		Entity entity = createTree();

		I18nText description = translator.getDescription(entity);

		assertThat(description.toString()).isEqualTo("Oak tree");
	}

	@Test
	public void describeShrub() throws IOException {
		Entity entity = createShrub();

		I18nText description = translator.getDescription(entity);

		assertThat(description.toString()).isEqualTo("Bush");
	}

	@Test
	public void describeFurniture() throws IOException {
		FurnitureEntityAttributes attributes = new FurnitureEntityAttributesFactory(new FurnitureTypeDictionary(new FurnitureLayoutDictionary(),
				itemTypeDictionary)).byName("Stonemason_Bench", GameMaterialType.STONE, gameMaterialDictionary.getByName("Granite"));
		Entity entity = new FurnitureEntityFactory(mockMessageDispatcher, mockEntityAssetUpdater).create(attributes, new GridPoint2(), null, mockGameContext);
		I18nText description = translator.getDescription(entity);

		assertThat(description.toString()).isEqualTo("Granite stonemason workbench");
	}

	@Test
	public void describeDoor() throws IOException {
		FurnitureEntityAttributes attributes = new FurnitureEntityAttributesFactory(new FurnitureTypeDictionary(new FurnitureLayoutDictionary(),
				itemTypeDictionary)).byName("SINGLE_DOOR", GameMaterialType.WOOD, gameMaterialDictionary.getByName("Oak"));
		Entity entity = new FurnitureEntityFactory(mockMessageDispatcher, mockEntityAssetUpdater).create(attributes, new GridPoint2(), null, mockGameContext);
		I18nText description = translator.getDescription(entity);

		assertThat(description.toString()).isEqualTo("Oaken door");
	}

	@Test
	public void describeFurnitureConstruction() throws IOException {
		GameMaterial material = gameMaterialDictionary.getByName("Granite");
		FurnitureEntityAttributes attributes = new FurnitureEntityAttributesFactory(new FurnitureTypeDictionary(new FurnitureLayoutDictionary(),
				itemTypeDictionary)).byName("Stonemason_Bench", GameMaterialType.STONE , material);
		Entity furnitureEntity = new FurnitureEntityFactory(mockMessageDispatcher, mockEntityAssetUpdater).create(attributes, new GridPoint2(), null, mockGameContext);

		FurnitureConstruction construction = new FurnitureConstruction(furnitureEntity);
		for (QuantifiedItemTypeWithMaterial requirement : construction.getRequirements()) {
			requirement.setMaterial(material);
		}

		assertThat(construction.getHeadlineDescription(translator).toString()).isEqualTo("Construction of granite stonemason workbench");
	}

//	@Test
//	public void describeJobs() throws IOException {
//		GameContext gameContext = new GameContext();
//		gameContext.setRandom(new RandomXS128());
//		gameContext.setAreaMap(mockMap);
//
//		NorseNameGenerator nameGenerator = new NorseNameGenerator();
//		HumanoidEntityAttributes attributes = new HumanoidEntityAttributesFactory(
//				new HairColorFactory(), new SkinColorFactory(), new AccessoryColorFactory(), nameGenerator
//		).create();
//		Profession profession = new Profession();
//		profession.setI18nKey("PROFESSION.BLACKSMITH");
//		Entity entity = new HumanoidEntityFactory(
//				mockMessageDispatcher, new ProfessionDictionary(), mockEntityAssetUpdater,
//				mockGoalDictionary, mockScheduleDictionary).create(attributes, null, new Vector2(), profession, gameContext);
//
//		HumanoidBehaviour behaviour = (HumanoidBehaviour) entity.getBehaviourComponent();
//
//		// Idle
//		assertThat(translator.getCurrentGoalDescription(entity, gameContext)).isEqualTo("Idle");
//		behaviour.setCurrentGoal(new IdleGoal(entity, new GameClock(), 1f));
//		assertThat(translator.getCurrentGoalDescription(entity, gameContext)).isEqualTo("Idle");
//		// Note that just going to a location as the primary goal is also idling
//		behaviour.setCurrentGoal(new GoToLocationGoal(entity, new Vector2(), mockMessageDispatcher, null));
//		assertThat(translator.getCurrentGoalDescription(entity, gameContext)).isEqualTo("Idle");
//
//		// Working on job
//		WorkOnJobGoal workOnJobGoal = new WorkOnJobGoal(entity, mockMessageDispatcher, gameContext);
//		behaviour.setCurrentGoal(workOnJobGoal);
//		assertThat(translator.getCurrentGoalDescription(entity, gameContext)).isEqualTo("Looking for work to do");
//
//		// Hauling job
//		HaulingAllocation haulingAllocation = new HaulingAllocation();
//		haulingAllocation.setQuantity(2);
//		haulingAllocation.setItemType(itemTypeDictionary.getByName("Resource-Gem"));
//		haulingAllocation.setGameMaterial(gameMaterialDictionary.getByName("Sapphire"));
//		workOnJobGoal.setAssignedJob(createHaulingJob(haulingAllocation, mockEntity));
//		assertThat(translator.getCurrentGoalDescription(entity, gameContext)).isEqualTo("Hauling 2 sapphire gems");
//
//		haulingAllocation.setQuantity(1);
//		haulingAllocation.setItemType(itemTypeDictionary.getByName("Resource-Stone-Unrefined"));
//		haulingAllocation.setGameMaterial(gameMaterialDictionary.getByName("Marble"));
//		assertThat(translator.getCurrentGoalDescription(entity, gameContext)).isEqualTo("Hauling marble rough stone boulder");
//
//		// Other jobs
//
//		ProfessionDictionary professionDictionary = new ProfessionDictionary();
//
//		// Crafting jobs
//
//		Job craftingJob = new Job(JobType.CRAFT_ITEM);
//		workOnJobGoal.setAssignedJob(craftingJob);
//
//		craftingJob.setRequiredProfession(professionDictionary.getByName("CARPENTER"));
//		assertThat(translator.getCurrentGoalDescription(entity, gameContext)).isEqualTo("Crafting");
//		craftingJob.setRequiredProfession(professionDictionary.getByName("STONEMASON"));
//		assertThat(translator.getCurrentGoalDescription(entity, gameContext)).isEqualTo("Sculpting");
//
//		// Mining job
//
//		Job miningJob = new Job(JobType.MINING);
//		miningJob.setJobLocation(new GridPoint2());
//		miningJob.setRequiredProfession(professionDictionary.getByName("MINER"));
//		workOnJobGoal.setAssignedJob(miningJob);
//
//		when(mockMap.getTile(any(GridPoint2.class))).thenReturn(mockMapTile);
//		Wall wall = new Wall(new WallLayout(1), new WallType("Test gem wall", "WALL.GEMS", 1L, GameMaterialType.GEM, false, null, null),
//				gameMaterialDictionary.getByName("Sapphire"));
//		when(mockMapTile.hasWall()).thenReturn(true);
//		when(mockMapTile.getWall()).thenReturn(wall);
//
//		assertThat(translator.getCurrentGoalDescription(entity, gameContext)).isEqualTo("Mining sapphire gems");
//
//		wall = new Wall(new WallLayout(1), new WallType("Test rock wall", "WALL.ROUGH_STONE", 1L, GameMaterialType.STONE, false, null, null),
//				gameMaterialDictionary.getByName("Granite"));
//		when(mockMapTile.getWall()).thenReturn(wall);
//
//		assertThat(translator.getCurrentGoalDescription(entity, gameContext)).isEqualTo("Mining granite rock wall");
//
//		// Logging job
//
//		Job loggingJob = new Job(JobType.LOGGING);
//		loggingJob.setRequiredProfession(professionDictionary.getByName("LUMBERJACK"));
//		loggingJob.setJobLocation(new GridPoint2());
//		workOnJobGoal.setAssignedJob(loggingJob);
//
//		List<Entity> tileEntities = new ArrayList<>();
//		tileEntities.add(createPlantForMap());
//		when(mockMapTile.getEntities()).thenReturn(tileEntities);
//		when(mockMapTile.hasTree()).thenReturn(true);
//
//		assertThat(translator.getCurrentGoalDescription(entity, gameContext)).isEqualTo("Cutting oak tree");
//
//		// Constructing furniture job
//
//		Job constructionJob = new Job(JobType.CONSTRUCT_STONE_FURNITURE);
//		constructionJob.setRequiredProfession(professionDictionary.getByName("STONEMASON"));
//		constructionJob.setJobLocation(new GridPoint2());
//		workOnJobGoal.setAssignedJob(constructionJob);
//
//		assertThat(translator.getCurrentGoalDescription(entity, gameContext)).isEqualTo("Sculpting furniture");
//
//		Job clearGroundJob = new Job(JobType.CLEAR_GROUND);
//		clearGroundJob.setRequiredProfession(null);
//		clearGroundJob.setJobLocation(new GridPoint2());
//		workOnJobGoal.setAssignedJob(clearGroundJob);
//
//		tileEntities.clearContextRelatedState();
//		tileEntities.add(createShrub());
//
//		assertThat(translator.getCurrentGoalDescription(entity, gameContext)).isEqualTo("Clearing bush");
//
//		Job collectItemJob = new Job(JobType.COLLECT_ITEM);
//		collectItemJob.setRequiredProfession(null);
//		collectItemJob.setJobLocation(new GridPoint2());
//		collectItemJob.setTargetId(7L);
//		workOnJobGoal.setAssignedJob(collectItemJob);
//
//		Entity itemEntity = createPileOfLogs();
//		when(mockEntityStore.getById(7L)).thenReturn(itemEntity);
//
//		assertThat(translator.getCurrentGoalDescription(entity, mockGameContext)).isEqualTo("Hauling 5 oaken logs");
//	}

	@Test
	public void getValueForKey() throws Exception {
		assertThat(translator.getTranslatedString("GUI.ORDERS_LABEL").toString()).isEqualTo("Orders");
	}

	@Test
	public void getWallDescription_forAllMaterials() {
		when(mockMapTile.hasWall()).thenReturn(true);

		for (GameMaterial gameMaterial : gameMaterialDictionary.getAll()) {
			Wall testWall = new Wall(new WallLayout(0), mockWallType, gameMaterial);
			when(mockMapTile.getWall()).thenReturn(testWall);
			I18nText description = translator.getWallDescription(mockMapTile);
			assertThat(description).isNotNull();
			assertThat(description.toString().length()).isGreaterThan(0);
		}

	}

	@Test
	public void getWallConstructionDescription_doesNotShowMaterialType_whenNoMaterialSelected() {
		WallConstruction wallConstruction = createWallConstruction(NULL_MATERIAL);

		I18nText description = wallConstruction.getHeadlineDescription(translator);

		assertThat(description.toString()).isEqualTo("Construction of smooth stone wall");
	}

	@Test
	public void getWallConstructionDescription_doesShowMaterialType_whenMaterialIsSelected() {
		WallConstruction wallConstruction = createWallConstruction(gameMaterialDictionary.getByName("Dolostone"));

		I18nText description = wallConstruction.getHeadlineDescription(translator);

		assertThat(description.toString()).isEqualTo("Construction of smooth stone wall");
	}

	@Test
	public void getConstructionStatusDescription() {
		WallConstruction wallConstruction = createWallConstruction(NULL_MATERIAL);

		wallConstruction.setState(ConstructionState.CLEARING_WORK_SITE);

		assertThat(wallConstruction.getConstructionStatusDescriptions(translator, null).toString()).contains("Removing other items");

		wallConstruction.setState(ConstructionState.SELECTING_MATERIALS);
		doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable {
				ItemMaterialSelectionMessage messageInfo = invocation.getArgument(1, ItemMaterialSelectionMessage.class);
				messageInfo.callback.accept(null);
				return null;
			}
		}).when(mockMessageDispatcher).dispatchMessage(eq(MessageType.SELECT_AVAILABLE_MATERIAL_FOR_ITEM_TYPE), any(ItemMaterialSelectionMessage.class));

		assertThat(wallConstruction.getConstructionStatusDescriptions(translator, mockMessageDispatcher).toString()).contains("Waiting for more stone blocks to be available");

		wallConstruction.setState(ConstructionState.WAITING_FOR_RESOURCES);

		assertThat(wallConstruction.getConstructionStatusDescriptions(translator, null).toString()).contains("Waiting for resources to arrive");

		wallConstruction.setState(ConstructionState.WAITING_FOR_COMPLETION);

		assertThat(wallConstruction.getConstructionStatusDescriptions(translator, null).toString()).contains("Under construction");
	}

	@Test
	public void getDateTimeString() {
		GameClock gameClock = new GameClock();

		I18nText result = translator.getDateTimeString(gameClock);

		assertThat(result.toString()).isEqualTo("08:00, Day 1, Year 1");
	}

	@Test
	public void getItemType_GivenSingularItem_ReturnsFirstLetterUppercase() {
		GameMaterial material = new GameMaterial();
		material.setI18nValue(new I18nWord("SKIN.DWARF_HIDE", "Dwarf"));
		ItemType itemType = Mockito.mock(ItemType.class);
		when(itemType.getI18nKey()).thenReturn("PRODUCT.LEATHER");

		I18nText processed = translator.getItemDescription(1, material, itemType, ItemQuality.STANDARD);

		assertThat(processed.toString()).isEqualTo("Dwarf leather");
	}

	@Test
	public void getItemType_GivenMultipleItem_DoesNotCapitalizeFirstAlpha() {
		GameMaterial material = new GameMaterial();
		material.setI18nValue(new I18nWord("SKIN.DWARF_HIDE", "Deer"));
		ItemType itemType = Mockito.mock(ItemType.class);
		when(itemType.getI18nKey()).thenReturn("PRODUCT.LEATHER");

		I18nText processed = translator.getItemDescription(3, material, itemType, ItemQuality.STANDARD);

		assertThat(processed.toString()).isEqualTo("3 deer leather");
	}

	private Entity createTree() throws IOException {
		PlantEntityAttributesFactory factory = new PlantEntityAttributesFactory(new PlantSpeciesDictionary(gameMaterialDictionary, itemTypeDictionary));
		Random random = new RandomXS128(1L);
		PlantSpeciesDictionary speciesDictionary = new PlantSpeciesDictionary(gameMaterialDictionary, itemTypeDictionary);
		PlantEntityAttributes attributes = factory.createBySpecies(speciesDictionary.getByName("Oak"), random);

		return new PlantEntityFactory(mockMessageDispatcher, mockEntityAssetUpdater, mockJobTypeDictionary).create(attributes, new GridPoint2(), mockGameContext);
	}

	public Entity createShrub() throws IOException {
		PlantEntityAttributesFactory factory = new PlantEntityAttributesFactory(new PlantSpeciesDictionary(gameMaterialDictionary, itemTypeDictionary));
		Random random = new RandomXS128(1L);
		PlantSpeciesDictionary speciesDictionary = new PlantSpeciesDictionary(gameMaterialDictionary, itemTypeDictionary);
		PlantEntityAttributes attributes = factory.createBySpecies(speciesDictionary.getByName("Shrub"), random);

		return new PlantEntityFactory(mockMessageDispatcher, mockEntityAssetUpdater, mockJobTypeDictionary).create(attributes, new GridPoint2(), mockGameContext);
	}

	public Entity createPileOfLogs() {
		ItemEntityAttributes attributes = new ItemEntityAttributes(1);
		attributes.setQuantity(5);
		attributes.setItemType(itemTypeDictionary.getByName("Resource-Logs"));
		attributes.setMaterial(gameMaterialDictionary.getByName("Oak"));

		return new ItemEntityFactory(mockMessageDispatcher, gameMaterialDictionary, mockEntityAssetUpdater).create(attributes, new GridPoint2(), true, mockGameContext, Faction.SETTLEMENT);
	}

	private WallConstruction createWallConstruction(GameMaterial material) {
		when(mockItemType.getI18nKey()).thenReturn("RESOURCE.STONE.BLOCK");
		when(mockItemType.getPrimaryMaterialType()).thenReturn(GameMaterialType.STONE);

		Map<GameMaterialType, List<QuantifiedItemType>> requirements = new HashMap<>();
		QuantifiedItemType item = new QuantifiedItemType();
		item.setQuantity(3);
		item.setItemType(mockItemType);
		List<QuantifiedItemType> items = Arrays.asList(item);
		requirements.put(GameMaterialType.STONE, items);
		when(mockWallType.getRequirements()).thenReturn(requirements);
		when(mockWallType.getMaterialType()).thenReturn(GameMaterialType.STONE);
		return new WallConstruction(new GridPoint2(), mockWallType, material);
	}

}