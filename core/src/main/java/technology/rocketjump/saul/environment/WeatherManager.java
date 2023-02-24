package technology.rocketjump.saul.environment;


import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.saul.assets.FloorTypeDictionary;
import technology.rocketjump.saul.assets.model.FloorType;
import technology.rocketjump.saul.audio.model.SoundAsset;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.environment.model.DailyWeatherType;
import technology.rocketjump.saul.environment.model.ForecastItem;
import technology.rocketjump.saul.environment.model.WeatherType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.Updatable;
import technology.rocketjump.saul.jobs.model.JobTarget;
import technology.rocketjump.saul.mapping.model.TiledMap;
import technology.rocketjump.saul.mapping.tile.*;
import technology.rocketjump.saul.mapping.tile.roof.TileRoofState;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.ParticleRequestMessage;
import technology.rocketjump.saul.messaging.types.ReplaceFloorMessage;
import technology.rocketjump.saul.messaging.types.RequestSoundMessage;
import technology.rocketjump.saul.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.saul.particles.model.ParticleEffectType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Singleton
public class WeatherManager implements Updatable, Telegraph {

	private final GameMaterial snowMaterialType;
	private final FloorType snowFloorType;
	private GameContext gameContext;

	private final DailyWeatherTypeDictionary dailyWeatherTypeDictionary;
	private final WeatherEffectUpdater weatherEffectUpdater;
	private final MessageDispatcher messageDispatcher;

	private static final float TOTAL_COLOR_CHANGE_TIME = 10f;
	private double lastUpdateGameTime;
	private Double timeToNextLightningStrike;
	private ParticleEffectType lightningEffectType;
	private SoundAsset thunderCrackSoundAsset;

	@Inject
	public WeatherManager(DailyWeatherTypeDictionary dailyWeatherTypeDictionary, WeatherEffectUpdater weatherEffectUpdater,
						  FloorTypeDictionary floorTypeDictionary,
						  GameMaterialDictionary gameMaterialDictionary, MessageDispatcher messageDispatcher,
						  ParticleEffectTypeDictionary particleEffectTypeDictionary, SoundAssetDictionary soundAssetDictionary) {
		this.dailyWeatherTypeDictionary = dailyWeatherTypeDictionary;
		this.weatherEffectUpdater = weatherEffectUpdater;

		snowFloorType = floorTypeDictionary.getByFloorTypeName("fallen_snow");
		snowMaterialType = gameMaterialDictionary.getByName("Snowfall");
		lightningEffectType = particleEffectTypeDictionary.getByName("Lightning strike");
		thunderCrackSoundAsset = soundAssetDictionary.getByName("Thundercrack");
		this.messageDispatcher = messageDispatcher;

		messageDispatcher.addListener(this, MessageType.DAY_ELAPSED);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.DAY_ELAPSED: {
				gameContext.getMapEnvironment().setDailyWeather(selectDailyWeather(gameContext, dailyWeatherTypeDictionary));
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	public static DailyWeatherType selectDailyWeather(GameContext gameContext, DailyWeatherTypeDictionary dailyWeatherTypeDictionary) {
		List<DailyWeatherType> weatherTypesForSeason = dailyWeatherTypeDictionary.getAll().stream()
				.filter(dailyWeatherType -> dailyWeatherType.getApplicableSeason().equals(gameContext.getGameClock().getCurrentSeason()))
				.collect(Collectors.toList());

		float combinedChance = 0f;
		for (DailyWeatherType dailyWeatherType : weatherTypesForSeason) {
			combinedChance += dailyWeatherType.getChance();
		}
		float roll = gameContext.getRandom().nextFloat() * combinedChance;
		for (DailyWeatherType dailyWeatherType : weatherTypesForSeason) {
			roll -= dailyWeatherType.getChance();
			if (roll <= 0f) {
				return dailyWeatherType;
			}
		}

		// should not get here
		return weatherTypesForSeason.get(0);
	}

	@Override
	public void update(float deltaTime) {
		if (gameContext != null) {
			double currentGameTime = gameContext.getGameClock().getCurrentGameTime();
			double elapsedGameTime = currentGameTime - lastUpdateGameTime;
			this.lastUpdateGameTime = currentGameTime;

			gameContext.getMapEnvironment().setWeatherTimeRemaining(gameContext.getMapEnvironment().getWeatherTimeRemaining() - elapsedGameTime);

			if (gameContext.getMapEnvironment().getCurrentWeather().getAccumulatesSnowPerHour() != null) {
				updateSnowfall(elapsedGameTime);
			}
			if (gameContext.getMapEnvironment().getCurrentWeather().getLightningStrikesPerHour() != null) {
				if (timeToNextLightningStrike == null) {
					timeToNextLightningStrike = getTimeToNextLightningStrike();
				}
				timeToNextLightningStrike -= elapsedGameTime;
				if (timeToNextLightningStrike < 0) {
					triggerLightningStrike();
					timeToNextLightningStrike = null;
				}
			}

			if (gameContext.getMapEnvironment().getWeatherTimeRemaining() < 0) {
				triggerNextWeather();
			}

			updateWeatherColor(deltaTime);
		}
	}

	private double getTimeToNextLightningStrike() {
		return gameContext.getRandom().nextDouble() * ((1.0 / gameContext.getMapEnvironment().getCurrentWeather().getLightningStrikesPerHour()) * 2.0);
	}

	private void updateSnowfall(double elapsedGameTime) {
		double extraSnow = gameContext.getMapEnvironment().getCurrentWeather().getAccumulatesSnowPerHour() * elapsedGameTime;
		double currentSnow = gameContext.getMapEnvironment().getFallenSnow();
		double newSnow = currentSnow + extraSnow;
		newSnow = Math.max(0.0, Math.min(newSnow, 1.0));

		int currentSnowPercentile = toSnowPercentile(currentSnow);
		int newSnowPercentile = toSnowPercentile(newSnow);

		if (newSnowPercentile != currentSnowPercentile) {
			if (newSnowPercentile > currentSnowPercentile) {
				// Increasing snowfall
				for (int percentileToAdd = currentSnowPercentile; percentileToAdd < newSnowPercentile || percentileToAdd == 100; percentileToAdd++) {
					addSnowToGround(percentileToAdd);
				}
			} else if (newSnowPercentile < currentSnowPercentile) {
				// Decreasing snowfall
				for (int percentileToRemove = currentSnowPercentile; percentileToRemove > newSnowPercentile || percentileToRemove == 0; percentileToRemove--) {
					removeSnowFromGround(percentileToRemove);
				}
			}

			TiledMap areaMap = gameContext.getAreaMap();
			for (int y = 0; y <= areaMap.getHeight(); y++) {
				for (int x = 0; x <= areaMap.getWidth(); x++) {
					MapVertex vertex = areaMap.getVertex(x, y);
					TileNeighbours neighbours = areaMap.getTileNeighboursOfVertex(vertex);
					vertex.setTransitoryFloorAlpha(averageAlphas(neighbours.values(), currentSnow));
				}
			}
		}
		gameContext.getMapEnvironment().setFallenSnow(newSnow);
	}

	private float averageAlphas(Collection<MapTile> mapTiles, double currentSnow) {
		float acc = 0.0f;
		int count = 0;
		for (MapTile mapTile : mapTiles) {
			if (mapTile != null) {
				float transitoryFloorAlpha = mapTile.getTransitoryFloorAlpha(currentSnow);
				if (transitoryFloorAlpha > 0) {
					acc += transitoryFloorAlpha;
					count++;
				}
			}
		}

		if (count > 0) {
			return acc / count;
		} else {
			return 0.0f;
		}
	}

	private void addSnowToGround(int percentileToAdd) {
		for (MapTile mapTile : gameContext.getAreaMap().getTilesForPercentile(percentileToAdd)) {
			if (mapTile.getRoof().getState().equals(TileRoofState.OPEN) && !mapTile.getFloor().getFloorType().equals(snowFloorType) &&
				!mapTile.hasWall() && !mapTile.getFloor().isRiverTile() & !mapTile.getFloor().hasBridge()) {
				messageDispatcher.dispatchMessage(MessageType.SET_TRANSITORY_FLOOR, new ReplaceFloorMessage(mapTile.getTilePosition(), snowFloorType, snowMaterialType));
			}
		}
	}

	private void removeSnowFromGround(int percentileToRemove) {
		for (MapTile mapTile : gameContext.getAreaMap().getTilesForPercentile(percentileToRemove)) {
			if (mapTile.getFloor().getFloorType().equals(snowFloorType)) {
				messageDispatcher.dispatchMessage(MessageType.REMOVE_TRANSITORY_FLOOR, mapTile.getTilePosition());
			}
		}
	}

	public void triggerNextWeather() {
		WeatherType currentWeather = gameContext.getMapEnvironment().getCurrentWeather();
		List<ForecastItem> forecast = gameContext.getMapEnvironment().getDailyWeather().getForecast();

		ForecastItem selectedForecast = null;
		if (forecast.size() == 1) {
			selectedForecast = forecast.get(0);
		} else {
			float combinedChance = 0f;
			for (ForecastItem forecastItem : forecast) {
				if (!forecastItem.getWeatherType().equals(currentWeather)) {
					combinedChance += forecastItem.getChance();
				}
			}

			float roll = gameContext.getRandom().nextFloat() * combinedChance;
			for (ForecastItem forecastItem : forecast) {
				if (!forecastItem.getWeatherType().equals(currentWeather)) {
					roll -= forecastItem.getChance();
					if (roll <= 0f) {
						selectedForecast = forecastItem;
						break;
					}
				}
			}

			if (selectedForecast == null) {
				// should not happen
				selectedForecast = forecast.get(0);
			}
		}

		double nextWeatherDuration = selectedForecast.getMinDurationHours();
		nextWeatherDuration += gameContext.getRandom().nextDouble() * Math.abs(selectedForecast.getMaxDurationHours() - selectedForecast.getMinDurationHours());

		gameContext.getMapEnvironment().setCurrentWeather(selectedForecast.getWeatherType());
		gameContext.getMapEnvironment().setWeatherTimeRemaining(nextWeatherDuration);
		timeToNextLightningStrike = null;
		weatherEffectUpdater.weatherChanged();
	}

	private void updateWeatherColor(float deltaTime) {
		WeatherType currentWeather = gameContext.getMapEnvironment().getCurrentWeather();
		Color targetWeatherColor = currentWeather.getMaxSunlightColor();
		Color currentWeatherColor = gameContext.getMapEnvironment().getWeatherColor();

		if (!currentWeatherColor.equals(targetWeatherColor)) {

			float changeAmount = deltaTime / TOTAL_COLOR_CHANGE_TIME;

			float channelDiff = targetWeatherColor.r - currentWeatherColor.r;
			if (Math.abs(channelDiff) <= changeAmount) {
				currentWeatherColor.r = targetWeatherColor.r;
			} else if (channelDiff < 0) {
				currentWeatherColor.r -= changeAmount;
			} else {
				currentWeatherColor.r += changeAmount;
			}

			channelDiff = targetWeatherColor.g - currentWeatherColor.g;
			if (Math.abs(channelDiff) <= changeAmount) {
				currentWeatherColor.g = targetWeatherColor.g;
			} else if (channelDiff < 0) {
				currentWeatherColor.g -= changeAmount;
			} else {
				currentWeatherColor.g += changeAmount;
			}

			channelDiff = targetWeatherColor.b - currentWeatherColor.b;
			if (Math.abs(channelDiff) <= changeAmount) {
				currentWeatherColor.b = targetWeatherColor.b;
			} else if (channelDiff < 0) {
				currentWeatherColor.b -= changeAmount;
			} else {
				currentWeatherColor.b += changeAmount;
			}

		}
	}

	private int toSnowPercentile(double snowAmount) {
		return (int) (snowAmount * 100);
	}

	private void triggerLightningStrike() {
		List<MapTile> potentialStrikeLocations = new ArrayList<>();
		TiledMap map = gameContext.getAreaMap();
		for (int i = 0; i < 40; i++) {
			potentialStrikeLocations.add(map.getTile(
					gameContext.getRandom().nextInt(map.getWidth()),
					gameContext.getRandom().nextInt(map.getHeight()
			)));
		}

		Optional<MapTile> strikeLocation = potentialStrikeLocations.stream()
				.filter(tile -> tile.getRoof().getState().equals(TileRoofState.OPEN) && tile.getExploration().equals(TileExploration.EXPLORED))
				.sorted((a, b) -> strikeChance(b) - strikeChance(a))
				.findFirst();

		if (strikeLocation.isPresent()) {
			triggerStrikeAt(strikeLocation.get());
		}

	}

	public void triggerStrikeAt(MapTile targetTile) {
		messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(
				lightningEffectType, Optional.empty(), Optional.of(new JobTarget(targetTile)), (p) -> {}
		));
		messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(
				thunderCrackSoundAsset,
				targetTile.getEntities().isEmpty() ? null : targetTile.getEntities().iterator().next().getId(),
				targetTile.getWorldPositionOfCenter(), null
		));
		if (gameContext.getRandom().nextBoolean()) {
			messageDispatcher.dispatchMessage(MessageType.START_FIRE_IN_TILE, targetTile);
		}
	}

	private int strikeChance(MapTile a) {
		if (a.hasTree()) {
			return 100;
		} else if (a.hasPlant()) {
			return -1; // quick fix to avoid crops
		} else {
			return a.getEntities().size();
		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
		this.lastUpdateGameTime = gameContext.getGameClock().getCurrentGameTime();
	}

	@Override
	public void clearContextRelatedState() {
	}

	@Override
	public boolean runWhilePaused() {
		return false;
	}
}
