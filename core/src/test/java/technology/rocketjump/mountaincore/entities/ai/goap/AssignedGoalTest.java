package technology.rocketjump.mountaincore.entities.ai.goap;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.mountaincore.entities.EntityAssetUpdater;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureGroup;
import technology.rocketjump.mountaincore.entities.components.creature.SkillsComponent;
import technology.rocketjump.mountaincore.entities.factories.ItemEntityAttributesFactory;
import technology.rocketjump.mountaincore.entities.factories.SettlerFactory;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Race;
import technology.rocketjump.mountaincore.entities.model.physical.creature.RaceBehaviour;
import technology.rocketjump.mountaincore.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.mountaincore.environment.GameClock;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.model.TiledMap;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.mapping.tile.TileNeighbours;
import technology.rocketjump.mountaincore.mapping.tile.floor.TileFloor;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.rooms.RoomStore;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static technology.rocketjump.mountaincore.assets.model.FloorType.NULL_FLOOR;

@RunWith(MockitoJUnitRunner.class)
public class AssignedGoalTest {

	private GoalDictionary goalDictionary;
	private I18nTranslator i18nTranslator;
	private GameContext gameContext;
	private Entity entity;
	@Mock
	private EntityAssetUpdater mockAssetUpdater;
	@Mock
	private MessageDispatcher mockMessageDispatcher;
	private ScheduleDictionary scheduleDictionary;
	private RoomStore roomStore;
	@Mock
	private TiledMap mockMap;
	@Mock
	private MapTile mockTile;
	@Mock
	private ItemEntityAttributesFactory mockItemEntityAttributesFactory;
	@Mock
	private RaceDictionary mockRaceDictionary;
	private Race stubRace;
	@Mock
	private Schedule mockSchedule;
	@Mock
	private CreatureGroup mockCreatureGroup;

	@Before
	public void setUp() throws Exception {
		when(mockMap.getNearestTiles(any(Vector2.class))).thenReturn(new Array<>());
		when(mockMap.getTile(any(Vector2.class))).thenReturn(mockTile);
		when(mockMap.getTile(any(GridPoint2.class))).thenReturn(mockTile);
		when(mockMap.getNeighbours(anyInt(), anyInt())).thenReturn(new TileNeighbours());
		when(mockMap.getEmbarkPoint()).thenReturn(new GridPoint2(1, 1));

		when(mockTile.getFloor()).thenReturn(new TileFloor(NULL_FLOOR, GameMaterial.NULL_MATERIAL));
		stubRace = new Race();
		stubRace.setBodyShapes(List.of());
		RaceBehaviour stubBehaviour = new RaceBehaviour();
		stubBehaviour.setSchedule(mockSchedule);
		stubRace.setBehaviour(stubBehaviour);
		when(mockRaceDictionary.getByName(anyString())).thenReturn(stubRace);

		when(mockSchedule.getCurrentApplicableCategories(any())).thenReturn(List.of(ScheduleCategory.ANY));

		Injector injector = Guice.createInjector((binder) -> {
			binder.bind(EntityAssetUpdater.class).toInstance(mockAssetUpdater);
			binder.bind(ItemEntityAttributesFactory.class).toInstance(mockItemEntityAttributesFactory);
			binder.bind(RaceDictionary.class).toInstance(mockRaceDictionary);
		});

		gameContext = new GameContext();
		gameContext.setRandom(new RandomXS128(1L));
		gameContext.setGameClock(new GameClock());
		gameContext.setAreaMap(mockMap);


		this.goalDictionary = injector.getInstance(GoalDictionary.class);
		this.i18nTranslator = injector.getInstance(I18nTranslator.class);
		this.scheduleDictionary = injector.getInstance(ScheduleDictionary.class);
		this.roomStore = injector.getInstance(RoomStore.class);

		this.entity = injector.getInstance(SettlerFactory.class).create(new Vector2(), new SkillsComponent().withNullProfessionActive(), gameContext, true);
	}

	@Test
	public void getDescription_hasTranslation_forEachGoalAndAction() {

		for (Goal goal : goalDictionary.getAllGoals()) {
			CreatureBehaviour settlerBehaviour = resetBehaviour();

			settlerBehaviour.getGoalQueue().add(new QueuedGoal(
					goal, ScheduleCategory.ANY, GoalPriority.HIGHEST, gameContext.getGameClock()
			));
			settlerBehaviour.update(1);

			String description = i18nTranslator.getCurrentGoalDescription(entity, ((CreatureBehaviour) entity.getBehaviourComponent()).getCurrentGoal(), gameContext).toString();

			System.out.println("Goal: " + goal.name + ", description: " + description);
		}


	}

	private CreatureBehaviour resetBehaviour() {
		CreatureBehaviour behaviour = new CreatureBehaviour();
		behaviour.constructWith(goalDictionary);
		behaviour.init(entity, mockMessageDispatcher, gameContext);
		entity.replaceBehaviourComponent(behaviour);
		return behaviour;
	}
}