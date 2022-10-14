package technology.rocketjump.saul.ui.widgets.maingame;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.environment.WeatherTypeDictionary;
import technology.rocketjump.saul.environment.model.GameSpeed;
import technology.rocketjump.saul.environment.model.Season;
import technology.rocketjump.saul.environment.model.WeatherType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.ScaledToFitLabel;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class TimeDateWidget extends Container<Table> implements Telegraph, GameContextAware, DisplaysText {

	private final MessageDispatcher messageDispatcher;
	private final I18nTranslator i18nTranslator;

	private final Table layoutTable = new Table();

	private Label settlementNameLabel;

	private final Table seasonWeatherTable = new Table();
	private final Skin mainGameSkin;
	private Label seasonLabel;
	private Image seasonIcon;
	private Label weatherLabel;
	private Image weatherIcon;

	private final Table gameSpeedControlsTable = new Table();
	private final ArrayList<Button> speedButtons = new ArrayList<>();

	private Label dateTimeText;
	private final Map<Season, Drawable> seasonDrawables = new EnumMap<>(Season.class);
	private final Map<WeatherType, Drawable> weatherDrawables = new HashMap<>();
	private GameContext gameContext;

	@Inject
	public TimeDateWidget(GuiSkinRepository skinRepository, I18nTranslator i18nTranslator, WeatherTypeDictionary weatherTypeDictionary,
						  MessageDispatcher messageDispatcher) {
		this.i18nTranslator = i18nTranslator;
		this.messageDispatcher = messageDispatcher;
		this.mainGameSkin = skinRepository.getMainGameSkin();

		Drawable background = mainGameSkin.getDrawable("info_box_bg");
		this.setBackground(background);
		this.size(background.getMinWidth() / 2f, background.getMinHeight() / 2f);

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

		rebuildUI();

		this.setActor(layoutTable);

		messageDispatcher.addListener(this, MessageType.GAME_SPEED_CHANGED);
	}

	@Override
	public void rebuildUI() {
		settlementNameLabel = new ScaledToFitLabel("...", mainGameSkin.get("settlement-name-label", Label.LabelStyle.class), 247);

		buildGameSpeedTable();
		buildSeasonWeatherTable();

		dateTimeText = new Label("date/time", mainGameSkin);

		layoutTable.clearChildren();
		layoutTable.padLeft(35);
		layoutTable.padTop(49);
		layoutTable.center();
		layoutTable.defaults().padBottom(8);
		layoutTable.add(centeredContainer(settlementNameLabel)).size(247, 35).center().row();
		layoutTable.add(centeredContainer(gameSpeedControlsTable)).center().width(279).row();
		layoutTable.add(centeredContainer(seasonWeatherTable)).center().width(279).row();
		layoutTable.add(centeredContainer(dateTimeText)).center().width(279).row();

		onContextChange(gameContext);
	}

	private void buildGameSpeedTable() {
		speedButtons.clear();
		gameSpeedControlsTable.clearChildren();
		gameSpeedControlsTable.defaults().padLeft(22);

		for (GameSpeed gameSpeed : GameSpeed.VISIBLE_TO_UI) {
			Button speedButton = new Button(mainGameSkin, "game-speed-"+gameSpeed.name().toLowerCase());
			speedButton.addListener(new ClickListener() {
				@Override
				public void clicked(InputEvent event, float x, float y) {
					messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, gameSpeed);
				}
			});
			Cell<Button> cell = gameSpeedControlsTable.add(speedButton);
			if (gameSpeed.equals(GameSpeed.PAUSED)) {
				cell.padLeft(0);
			}
			speedButtons.add(speedButton);
		}

	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.GAME_SPEED_CHANGED -> {
				GameSpeed selectedSpeed = (GameSpeed) msg.extraInfo;
				speedChangedTo(selectedSpeed);
				return true;
			}
			default -> throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + getClass().getSimpleName() + ", " + msg.toString());
		}
	}

	private void speedChangedTo(GameSpeed selectedSpeed) {
		int index = 0;
		for (GameSpeed gameSpeedForIndex : GameSpeed.VISIBLE_TO_UI) {
			Button speedButton = speedButtons.get(index);
			speedButton.setChecked(selectedSpeed.equals(gameSpeedForIndex));
			index++;
		}
	}

	private void buildSeasonWeatherTable() {
		seasonWeatherTable.clearChildren();
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

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
		if (gameContext == null) {
			settlementNameLabel.setText("...");
		} else {
			settlementNameLabel.setText(gameContext.getSettlementState().getSettlementName());
			GameSpeed currentSpeed = gameContext.getGameClock().isPaused() ? GameSpeed.PAUSED : gameContext.getGameClock().getCurrentGameSpeed();
			speedChangedTo(currentSpeed);
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

	@Override
	public void clearContextRelatedState() {

	}
}
