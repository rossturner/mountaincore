package technology.rocketjump.saul.entities.ai.goap;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
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
import technology.rocketjump.saul.entities.EntityAssetUpdater;
import technology.rocketjump.saul.entities.behaviour.creature.SettlerBehaviour;
import technology.rocketjump.saul.entities.factories.ItemEntityAttributesFactory;
import technology.rocketjump.saul.entities.factories.SettlerFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.saul.environment.GameClock;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.model.TiledMap;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.mapping.tile.floor.TileFloor;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.rooms.RoomStore;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static technology.rocketjump.saul.assets.model.FloorType.NULL_FLOOR;
import static technology.rocketjump.saul.jobs.ProfessionDictionary.NULL_PROFESSION;

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

	@Before
	public void setUp() throws Exception {
		Injector injector = Guice.createInjector((binder) -> {
			binder.bind(EntityAssetUpdater.class).toInstance(mockAssetUpdater);
			binder.bind(ItemEntityAttributesFactory.class).toInstance(mockItemEntityAttributesFactory);
			binder.bind(RaceDictionary.class).toInstance(mockRaceDictionary);
		});

		gameContext = new GameContext();
		gameContext.setRandom(new RandomXS128(1L));
		gameContext.setGameClock(new GameClock());
		gameContext.setAreaMap(mockMap);

		when(mockMap.getNearestTiles(any(Vector2.class))).thenReturn(new Array<>());
		when(mockMap.getTile(any(Vector2.class))).thenReturn(mockTile);

		when(mockTile.getFloor()).thenReturn(new TileFloor(NULL_FLOOR, GameMaterial.NULL_MATERIAL));
		stubRace = new Race();
		stubRace.setBodyShapes(List.of());
		when(mockRaceDictionary.getByName(anyString())).thenReturn(stubRace);

		this.goalDictionary = injector.getInstance(GoalDictionary.class);
		this.i18nTranslator = injector.getInstance(I18nTranslator.class);
		this.scheduleDictionary = injector.getInstance(ScheduleDictionary.class);
		this.roomStore = injector.getInstance(RoomStore.class);

		this.entity = injector.getInstance(SettlerFactory.class).create(new Vector2(), new Vector2(), NULL_PROFESSION, NULL_PROFESSION, gameContext);

	}

	@Test
	public void getDescription_hasTranslation_forEachGoalAndAction() {

		for (Goal goal : goalDictionary.getAllGoals()) {
			SettlerBehaviour settlerBehaviour = resetSettlerBehaviour();

			settlerBehaviour.getGoalQueue().add(new QueuedGoal(
					goal, ScheduleCategory.ANY, GoalPriority.HIGHEST, gameContext.getGameClock()
			));
			settlerBehaviour.update(1, gameContext);

			String description = i18nTranslator.getCurrentGoalDescription(entity, ((SettlerBehaviour) entity.getBehaviourComponent()).getCurrentGoal(), gameContext).toString();

			System.out.println("Goal: " + goal.name + ", description: " + description);
		}


	}

	private SettlerBehaviour resetSettlerBehaviour() {
		SettlerBehaviour settlerBehaviour = new SettlerBehaviour();
		settlerBehaviour.constructWith(goalDictionary, roomStore);
		settlerBehaviour.init(entity, mockMessageDispatcher, gameContext);
		entity.replaceBehaviourComponent(settlerBehaviour);
		return settlerBehaviour;
	}
}