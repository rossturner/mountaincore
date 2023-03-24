package technology.rocketjump.mountaincore.mapping.model;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.mountaincore.environment.model.DailyWeatherType;
import technology.rocketjump.mountaincore.environment.model.WeatherType;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.ChildPersistable;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rendering.utils.HexColors;

/**
 * This class is to store data such as the ambient temperature, weather, etc.
 */
public class MapEnvironment implements ChildPersistable {

	private DailyWeatherType dailyWeather;
	private WeatherType currentWeather;
	private double weatherTimeRemaining;

	private Color sunlightColor = Color.WHITE.cpy();
	private Color weatherColor = Color.WHITE.cpy();
	private float sunlightAmount = 1;
	private double fallenSnow = 0;

	public WeatherType getCurrentWeather() {
		return currentWeather;
	}

	public void setCurrentWeather(WeatherType currentWeather) {
		this.currentWeather = currentWeather;
	}

	public Color getSunlightColor() {
		return sunlightColor;
	}

	public void setSunlightColor(Color sunlightColor) {
		this.sunlightColor = sunlightColor;
		this.sunlightAmount = (sunlightColor.r + sunlightColor.g + sunlightColor.b) / 3f;
	}

	public float getSunlightAmount() {
		return sunlightAmount;
	}

	public Color getWeatherColor() {
		return weatherColor;
	}

	public void setWeatherColor(Color weatherColor) {
		this.weatherColor = weatherColor;
	}

	public double getFallenSnow() {
		return fallenSnow;
	}

	public void setFallenSnow(double fallenSnow) {
		this.fallenSnow = fallenSnow;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("dailyWeather", dailyWeather.getName());
		asJson.put("weather", currentWeather.getName());
		asJson.put("weatherRemaining", weatherTimeRemaining);
		asJson.put("sunlight", HexColors.toHexString(sunlightColor));
		asJson.put("weatherColor", HexColors.toHexString(weatherColor));
		asJson.put("snow", fallenSnow);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.dailyWeather = relatedStores.dailyWeatherTypeDictionary.getByName(asJson.getString("dailyWeather"));
		if (this.dailyWeather == null) {
			throw new InvalidSaveException("Could not find daily weather type with name " + asJson.getString("dailyWeather"));
		}

		this.currentWeather = relatedStores.weatherTypeDictionary.getByName(asJson.getString("weather"));
		if (this.currentWeather == null) {
			throw new InvalidSaveException("Could not find weather type with name " + asJson.getString("weather"));
		}
		this.weatherTimeRemaining = asJson.getDoubleValue("weatherRemaining");
		setSunlightColor(HexColors.get(asJson.getString("sunlight")));
		setWeatherColor(HexColors.get(asJson.getString("weatherColor")));
		this.fallenSnow = asJson.getDoubleValue("snow");
	}

	public double getWeatherTimeRemaining() {
		return weatherTimeRemaining;
	}

	public void setWeatherTimeRemaining(double weatherTimeRemaining) {
		this.weatherTimeRemaining = weatherTimeRemaining;
	}

	public DailyWeatherType getDailyWeather() {
		return dailyWeather;
	}

	public void setDailyWeather(DailyWeatherType dailyWeather) {
		this.dailyWeather = dailyWeather;
	}

}
