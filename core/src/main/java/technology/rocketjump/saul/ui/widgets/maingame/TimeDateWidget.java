package technology.rocketjump.saul.ui.widgets.maingame;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.environment.WeatherTypeDictionary;
import technology.rocketjump.saul.environment.model.Season;
import technology.rocketjump.saul.environment.model.WeatherType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class TimeDateWidget extends Container<Table> {

	private final I18nTranslator i18nTranslator;

	private final Table layoutTable;

	private final Label settlementNameLabel;

	private final Table seasonWeatherTable = new Table();
	private Label seasonLabel;
	private Image seasonIcon;
	private Label weatherLabel;
	private Image weatherIcon;

	private final Table gameSpeedControlsTable = new Table();

	private final Label dateTimeText;

	private final Map<Season, Drawable> seasonDrawables = new EnumMap<>(Season.class);
	private final Map<WeatherType, Drawable> weatherDrawables = new HashMap<>();

	public TimeDateWidget(Skin mainGameSkin, I18nTranslator i18nTranslator, WeatherTypeDictionary weatherTypeDictionary) {
		this.i18nTranslator = i18nTranslator;

		Drawable background = mainGameSkin.getDrawable("info_box_bg");
		this.setBackground(background);
		this.size(background.getMinWidth() / 2f, background.getMinHeight() / 2f);

		layoutTable = new Table();

		settlementNameLabel = new Label("...", mainGameSkin.get("settlement-name-label", Label.LabelStyle.class));

		buildGameSpeedTable(mainGameSkin);
		buildSeasonWeatherTable(mainGameSkin);

		dateTimeText = new Label("date/time", mainGameSkin);


		Container<Table> seasonWeatherContainer = new Container<>();
		seasonWeatherContainer.setActor(seasonWeatherTable);
		seasonWeatherContainer.center();

		for (Season season : Season.values()) {
			String drawableName = "asset_season_" + season.name().toLowerCase() + "_icon";
			Drawable seasonDrawable = mainGameSkin.getDrawable(drawableName);
			if (seasonDrawable == null) {
				Logger.error("Could not find drawable with name " + drawableName + " in main game skin");
			} else {
				seasonDrawables.put(season, seasonDrawable);
			}
		}
		for (WeatherType weatherType : weatherTypeDictionary.getAll()) {
			Drawable weatherDrawable = mainGameSkin.getDrawable(weatherType.getDrawableIconName());
			if (weatherDrawable == null) {
				Logger.error(String.format("Could not find drawable with name %s for weather type %s", weatherType.getDrawableIconName(), weatherType.getName()));
			} else {
				weatherDrawables.put(weatherType, weatherDrawable);
			}
		}


		layoutTable.setDebug(true);
		layoutTable.padLeft(35);
		layoutTable.padTop(80);
		layoutTable.center();
		layoutTable.defaults().padBottom(10);
		layoutTable.add(centeredContainer(settlementNameLabel)).size(247, 35).center().row();
		layoutTable.add(centeredContainer(gameSpeedControlsTable)).center().width(279).row();
		layoutTable.add(centeredContainer(seasonWeatherTable)).center().width(279).row();
		layoutTable.add(centeredContainer(dateTimeText)).center().width(279).row();
		this.setActor(layoutTable);
	}

	private void buildGameSpeedTable(Skin mainGameSkin) {
		gameSpeedControlsTable.setDebug(true);





	}

	private void buildSeasonWeatherTable(Skin mainGameSkin) {
		seasonWeatherTable.defaults().padLeft(4);

		seasonLabel = new Label("SEASON", mainGameSkin);
		seasonWeatherTable.add(seasonLabel);

		seasonIcon = new Image();
		seasonWeatherTable.add(centeredContainer(seasonIcon)).size(23, 23);

		weatherLabel = new Label("WEATHER", mainGameSkin);
		seasonWeatherTable.add(weatherLabel);

		weatherIcon = new Image();
		seasonWeatherTable.add(centeredContainer(weatherIcon)).size(23, 23);
	}

	public void reset(GameContext gameContext) {
		if (gameContext == null) {
			settlementNameLabel.setText("...");
		} else {
			settlementNameLabel.setText(gameContext.getSettlementState().getSettlementName());
		}
	}

	public void update(GameContext gameContext) {
		dateTimeText.setText(i18nTranslator.getDateTimeString(gameContext.getGameClock()).toString());

		seasonLabel.setText(i18nTranslator.getTranslatedString(gameContext.getGameClock().getCurrentSeason().getI18nKey()).toString());
		seasonIcon.setDrawable(seasonDrawables.get(gameContext.getGameClock().getCurrentSeason()));

		weatherLabel.setText(i18nTranslator.getTranslatedString(gameContext.getMapEnvironment().getCurrentWeather().getI18nKey()).toString());
		weatherIcon.setDrawable(weatherDrawables.get(gameContext.getMapEnvironment().getCurrentWeather()));
	}


	private Container<Actor> centeredContainer(Actor actor) {
		Container<Actor> container = new Container<>();
		container.setActor(actor);
		container.center();
		return container;
	}
}
