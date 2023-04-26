package technology.rocketjump.mountaincore.gamecontext;

import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.constants.ConstantsRepo;
import technology.rocketjump.mountaincore.constants.SettlementConstants;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Race;
import technology.rocketjump.mountaincore.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.mountaincore.environment.DailyWeatherTypeDictionary;
import technology.rocketjump.mountaincore.environment.GameClock;
import technology.rocketjump.mountaincore.environment.WeatherTypeDictionary;
import technology.rocketjump.mountaincore.invasions.InvasionDefinitionDictionary;
import technology.rocketjump.mountaincore.invasions.model.InvasionDefinition;
import technology.rocketjump.mountaincore.mapping.model.MapEnvironment;
import technology.rocketjump.mountaincore.mapping.model.TiledMap;
import technology.rocketjump.mountaincore.materials.GameMaterialDictionary;
import technology.rocketjump.mountaincore.persistence.UserPreferences;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.settlement.SettlementState;

import java.util.Map;
import java.util.Random;

import static technology.rocketjump.mountaincore.environment.WeatherManager.selectDailyWeather;
import static technology.rocketjump.mountaincore.screens.ScreenManager.chooseSpawnLocation;

@Singleton
public class GameContextFactory {

	private final ItemTypeDictionary itemTypeDictionary;
	private final GameMaterialDictionary gameMaterialDictionary;
	private final WeatherTypeDictionary weatherTypeDictionary;
	private final DailyWeatherTypeDictionary dailyWeatherTypeDictionary;
	private final SettlementConstants settlementConstants;
	private final UserPreferences userPreferences;
	private final InvasionDefinitionDictionary invasionDefinitionDictionary;
	private Race settlerRace;

	@Inject
	public GameContextFactory(ItemTypeDictionary itemTypeDictionary, GameMaterialDictionary gameMaterialDictionary,
							  WeatherTypeDictionary weatherTypeDictionary, DailyWeatherTypeDictionary dailyWeatherTypeDictionary,
							  ConstantsRepo constantsRepo, UserPreferences userPreferences, InvasionDefinitionDictionary invasionDefinitionDictionary, RaceDictionary raceDictionary) {
		this.itemTypeDictionary = itemTypeDictionary;
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.weatherTypeDictionary = weatherTypeDictionary;
		this.dailyWeatherTypeDictionary = dailyWeatherTypeDictionary;
		this.userPreferences = userPreferences;
		this.invasionDefinitionDictionary = invasionDefinitionDictionary;
		settlementConstants = constantsRepo.getSettlementConstants();
		this.settlerRace = raceDictionary.getByName("Dwarf"); // MODDING expose and test this
	}

	public GameContext create(String settlementName, TiledMap areaMap, long worldSeed, GameClock clock, boolean peacefulMode) {
		GameContext context = new GameContext();
		context.getSettlementState().setPeacefulMode(peacefulMode);
		context.getSettlementState().setSettlementName(settlementName);
		context.getSettlementState().setSettlerRace(settlerRace);

		if (chooseSpawnLocation(userPreferences)) {
			context.getSettlementState().setGameState(GameState.SELECT_SPAWN_LOCATION);
			clock.setPaused(true);
		} else {
			context.getSettlementState().setGameState(GameState.NORMAL);
		}

		context.getSettlementState().setFishRemainingInRiver(settlementConstants.getNumAnnualFish());
		context.setAreaMap(areaMap);
		context.setRandom(new RandomXS128(worldSeed));
		context.setGameClock(clock);
		context.setMapEnvironment(new MapEnvironment());
		initialise(context.getSettlementState(), context.getRandom());
		initialise(context.getMapEnvironment(), context);
		return context;
	}

	public GameContext create(SavedGameStateHolder stateHolder) {
		GameContext context = new GameContext();

		context.getJobs().putAll(stateHolder.jobs);
		for (int cursor = 0; cursor < stateHolder.entityIdsToLoad.size(); cursor++) {
			Long entityId = stateHolder.entityIdsToLoad.getLong(cursor);
			context.getEntities().put(entityId, stateHolder.entities.get(entityId));
		}

		context.getConstructions().putAll(stateHolder.constructions);
		context.getRooms().putAll(stateHolder.rooms);
		context.getJobRequestQueue().addAll(stateHolder.jobRequests.values());
		context.getDynamicallyCreatedMaterialsByCombinedId().putAll(stateHolder.dynamicMaterials);
		context.setSettlementState(stateHolder.getSettlementState());
		context.getSquads().putAll(stateHolder.squads);

		context.setAreaMap(stateHolder.getMap());
		context.setMapEnvironment(stateHolder.getMapEnvironment());
		context.setRandom(new RandomXS128()); // Not yet maintaining world seed
		context.setGameClock(stateHolder.getGameClock());

		return context;
	}

	private void initialise(MapEnvironment mapEnvironment, GameContext context) {
		mapEnvironment.setDailyWeather(selectDailyWeather(context, dailyWeatherTypeDictionary));
		mapEnvironment.setCurrentWeather(weatherTypeDictionary.getByName("Perfect"));
	}

	private void initialise(SettlementState settlementState, Random random) {
//		if (!settlementState.isPeacefulMode()) {
			initialise(settlementState.daysUntilNextInvasionCheck, random);
//		}
	}

	private void initialise(Map<InvasionDefinition, Integer> daysUntilNextInvasionCheck, Random random) {
		for (InvasionDefinition invasionDefinition : invasionDefinitionDictionary.getAll()) {
			int daysUntilFirstCheck = invasionDefinition.getMinDaysUntilFirstInvasion() +
					random.nextInt(invasionDefinition.getInvasionHappensWithinDays());
			daysUntilNextInvasionCheck.put(invasionDefinition, daysUntilFirstCheck);
		}

	}
}
