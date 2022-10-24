package technology.rocketjump.saul.screens.menus;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.audio.model.SoundAsset;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.environment.GameClock;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.GameSaveMessage;
import technology.rocketjump.saul.messaging.types.RequestSoundMessage;
import technology.rocketjump.saul.persistence.SavedGameInfo;
import technology.rocketjump.saul.persistence.SavedGameStore;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.i18n.I18nWord;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.MenuButtonFactory;
import technology.rocketjump.saul.ui.widgets.NotificationDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

@Singleton
public class LoadGameMenu implements Menu, GameContextAware, DisplaysText {

	private final SoundAsset startGameSound;
	private final MessageDispatcher messageDispatcher;
	private final MenuButtonFactory menuButtonFactory;
	private final SavedGameStore savedGameStore;
	private final Skin skin;
	private final I18nTranslator i18nTranslator;
	private final Stack stack = new Stack();
	private int carouselIndex = 0;

	private java.util.List<Table> slots;
	private Button leftArrow;
	private Button rightArrow;
	private GameContext gameContext;
	private SavedGameInfo selectedSavedGame;
	private Container<TextButton> deleteButton;
	private Container<TextButton> playButton;


	@Inject
	public LoadGameMenu(GuiSkinRepository skinRepository, MessageDispatcher messageDispatcher,
						SoundAssetDictionary soundAssetDictionary, MenuButtonFactory menuButtonFactory,
						SavedGameStore savedGameStore, I18nTranslator i18nTranslator) {
		this.messageDispatcher = messageDispatcher;
		this.menuButtonFactory = menuButtonFactory;
		this.savedGameStore = savedGameStore;
		this.skin = skinRepository.getMenuSkin();
		this.i18nTranslator = i18nTranslator;
		this.startGameSound = soundAssetDictionary.getByName("GameStart");

		rebuildUI();
	}

	@Override
	public void show() {
		carouselIndex = 0; //todo: not super happy with this, thinking might be better way to show what save was last loaded
		savedGamesUpdated();
	}

	@Override
	public void hide() {
	}

	@Override
	public void populate(Table containerTable) {
		containerTable.add(stack).grow();
	}

	@Override
	public void reset() {
	}

	public void savedGamesUpdated() {
		for (Table slot : slots) {
			slot.clearChildren();
			slot.setBackground(skin.getDrawable("save_greyed_out_bg"));
		}

		selectedSavedGame = null;
		disablePlayAndDeleteButtons();
		java.util.List<SavedGameInfo> savesInOrder = new ArrayList<>(savedGameStore.getAll());
		savesInOrder.sort((o1, o2) -> o2.lastModifiedTime.compareTo(o1.lastModifiedTime));

		if (carouselIndex < 0) {
			carouselIndex = 0;
		}

		if (carouselIndex > 0) {
			leftArrow.setTouchable(Touchable.enabled);
			leftArrow.setDisabled(false);
		} else {
			leftArrow.setTouchable(Touchable.disabled);
			leftArrow.setDisabled(true);
		}

		if (carouselIndex < savesInOrder.size() - 3) {
			rightArrow.setTouchable(Touchable.enabled);
			rightArrow.setDisabled(false);
		} else {
			rightArrow.setTouchable(Touchable.disabled);
			rightArrow.setDisabled(true);
		}


		for (int i = 0; i < Math.min(slots.size(), savesInOrder.size()); i++) {
			Table slotTable = slots.get(i);
			SavedGameInfo savedGame = savesInOrder.get(i + carouselIndex);
			populateSaveSlot(savedGame, slotTable);
		}
	}


	@Override
	public void onContextChange(GameContext gameContext) {
//		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}

	private void populateSaveSlot(SavedGameInfo savedGameInfo, Table saveSlot) {
		GameClock gameClock = savedGameInfo.gameClock;

		saveSlot.setTouchable(Touchable.enabled);
		saveSlot.clearListeners();
		saveSlot.addListener(new ClickListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				if (selectedSavedGame != savedGameInfo) {
					selectedSavedGame = savedGameInfo;
					enablePlayAndDeleteButtons();
				} else {
					selectedSavedGame = null;
					disablePlayAndDeleteButtons();
				}
				return super.touchDown(event, x, y, pointer, button);
			}
		});

		saveSlot.setBackground(skin.getDrawable("save_bg_2_scalable"));

		saveSlot.add();
		saveSlot.row();

		Label settlementName = new Label(savedGameInfo.settlementName, skin, "save_title_ribbon");
		settlementName.setAlignment(Align.center);
//		settlementName.setHeight(54);
//
//		Container<Label> settlementNameContainer = new Container<>(settlementName);
//		settlementNameContainer.height(54);
//		settlementNameContainer.setBackground(skin.getDrawable("save_title_ribbon_bg"));

		saveSlot.add(settlementName).padLeft(10).padRight(10);

		saveSlot.row();
		saveSlot.add(new Label(gameClock.getFormattedGameTime(), skin, "white_text")).spaceTop(26.0f).spaceBottom(26.0f);

		saveSlot.row();

//			seasonIcon.setDrawable(seasonDrawables.get(gameContext.getGameClock().getCurrentSeason()));
		//TODO: refactor a season widget (label and icon)
		saveSlot.add(new Label(i18nTranslator.getTranslatedString(gameClock.getCurrentSeason().getI18nKey()).toString(), skin, "white_text")).spaceTop(26.0f).spaceBottom(26.0f);

		saveSlot.row();
		saveSlot.add(new Label(i18nTranslator.getDayString(gameClock).toString(), skin, "white_text")).spaceTop(26.0f).spaceBottom(26.0f);

		saveSlot.row();
		saveSlot.add(new Label(i18nTranslator.getYearString(gameClock).toString(), skin, "white_text")).spaceTop(26.0f).spaceBottom(26.0f);

		saveSlot.row();
		saveSlot.add(new Label(savedGameInfo.version, skin, "white_text")).spaceTop(26.0f).spaceBottom(26.0f);
	}

	private void enablePlayAndDeleteButtons() {
		this.playButton.getActor().setDisabled(false);
		this.playButton.getActor().setTouchable(Touchable.enabled);
		this.deleteButton.getActor().setDisabled(false);
		this.deleteButton.getActor().setTouchable(Touchable.enabled);
		this.playButton.addAction(Actions.alpha(1f));
		this.deleteButton.addAction(Actions.alpha(1f));

	}

	private void disablePlayAndDeleteButtons() {
		this.playButton.getActor().setDisabled(true);
		this.playButton.getActor().setTouchable(Touchable.disabled);
		this.deleteButton.getActor().setDisabled(true);
		this.deleteButton.getActor().setTouchable(Touchable.disabled);

		this.playButton.addAction(Actions.alpha(0.5f));
		this.deleteButton.addAction(Actions.alpha(0.5f));
	}

	private Actor buildComponentLayer() {
		Table table = new Table();
		table.setName("loadGameComponents");
		table.padLeft(197.0f);
		table.padRight(197.0f);
		table.padTop(0.0f);
		table.padBottom(0.0f);

		Table titleTable = new Table();
		titleTable.setName("title");

		Label titleRibbon = new Label(i18nTranslator.getTranslatedString("MENU.LOAD_GAME").toString(), skin, "title_ribbon");

		titleRibbon.setAlignment(Align.center);
		titleTable.add(titleRibbon).width(1146); //.maxHeight(111.5f).maxWidth(573);


		table.add(titleTable).padTop(84.0f).padBottom(84.0f).colspan(5);
		table.row();

		this.leftArrow = new Button(skin, "left_arrow");
		leftArrow.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				carouselIndex--;
				savedGamesUpdated();
			}
		});

		Table slot1 = new Table();
		slot1.setName("slot1");

		Table slot2 = new Table();
		slot2.setName("slot2");

		Table slot3 = new Table();
		slot3.setName("slot3");

		this.slots = Arrays.asList(slot1, slot2, slot3);

		this.rightArrow = new Button(skin, "right_arrow");
		rightArrow.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				super.clicked(event, x, y);
				carouselIndex++;
				savedGamesUpdated();
			}
		});

		table.add(leftArrow);
		table.add(slot1).width(824.0f).height(1144.0f);
		table.add(slot2).width(824.0f).height(1144.0f);
		table.add(slot3).width(825.0f).height(1144.0f);
		table.add(rightArrow);

		table.row();

		Table loadControls = new Table();
		loadControls.setName("loadControls");
		this.deleteButton = menuButtonFactory.createButton("GUI.LOAD_GAME.TABLE.DELETE", skin, MenuButtonFactory.ButtonStyle.BTN_SCALABLE_50PT)
				.withAction(() -> {
					if (selectedSavedGame == null) {
						return;
					}

					NotificationDialog dialog = new NotificationDialog(
							i18nTranslator.getTranslatedString("GUI.DIALOG.INFO_TITLE"),
							skin,
							messageDispatcher
					);
					dialog.withText(i18nTranslator.getTranslatedWordWithReplacements("GUI.DIALOG.CONFIRM_DELETE_SAVE",
									Map.of("name", new I18nWord(selectedSavedGame.settlementName)))
							.breakAfterLength(i18nTranslator.getCurrentLanguageType().getBreakAfterLineLength()));

					dialog.withButton(i18nTranslator.getTranslatedString("GUI.DIALOG.OK_BUTTON"), (Runnable) () -> {
						savedGameStore.delete(selectedSavedGame);
						carouselIndex = 0;
						savedGamesUpdated();
					});
					dialog.withButton(i18nTranslator.getTranslatedString("GUI.DIALOG.CANCEL_BUTTON"));
					messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, dialog);
				})
				.build();

		Container<TextButton> backButton = menuButtonFactory.createButton("GUI.BACK_LABEL", skin, MenuButtonFactory.ButtonStyle.BTN_SCALABLE_50PT)
				.withAction(() -> {
					messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.TOP_LEVEL_MENU);
				})
				.build();

		this.playButton = menuButtonFactory.createButton("GUI.LOAD_GAME.TABLE.PLAY", skin, MenuButtonFactory.ButtonStyle.BTN_SCALABLE_50PT)
				.withAction(() -> {
					if (gameContext != null) {
						if (gameContext.getSettlementState().getSettlementName().equals(selectedSavedGame.settlementName)) {
							// Same game, just go back to it
							messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.TOP_LEVEL_MENU);
							messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_GAME");
						} else {
							// different game, save this first
							messageDispatcher.dispatchMessage(MessageType.PERFORM_SAVE, new GameSaveMessage(false));
							messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(startGameSound));
							messageDispatcher.dispatchMessage(MessageType.PERFORM_LOAD, selectedSavedGame);
						}
					} else {
						messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(startGameSound));
						messageDispatcher.dispatchMessage(MessageType.PERFORM_LOAD, selectedSavedGame);
					}


				})
				.build();

		deleteButton.width(336f).height(144.0f);
		backButton.width(336f).height(144.0f);
		playButton.width(336f).height(144.0f);

		loadControls.add(playButton).fill().spaceRight(36.0f);
		loadControls.add(backButton).fill().spaceLeft(36.0f).spaceRight(36.0f);
		loadControls.add(deleteButton).fill().spaceLeft(36.0f);


		table.add(loadControls).spaceTop(180.0f).spaceBottom(180.0f).expandX().align(Align.right).colspan(5);
		stack.addActor(table);

		return table;
	}

	private Actor buildBackgroundLayer() {
		Table table = new Table();
		table.setName("background");
		table.setBackground(skin.getDrawable("paper_texture_bg"));
		table.add(new Image(skin.getDrawable("paper_texture_bg_pattern_large"))).padLeft(242.0f);
		table.add().expandX();
		table.add(new Image(skin.getDrawable("paper_texture_bg_pattern_large"))).padRight(242.0f);
		return table;
	}

	private Actor buildBackgroundBaseLayer() {
		Table table = new Table();
		table.setName("backgroundBase");
		table.add(new Image(skin.getDrawable("menu_bg_left"))).left();
		table.add().expandX();
		table.add(new Image(skin.getDrawable("menu_bg_right"))).right();
		return table;
	}

	@Override
	public void rebuildUI() {
		stack.addActor(buildBackgroundBaseLayer());
		stack.addActor(buildBackgroundLayer());
		stack.addActor(buildComponentLayer());
		savedGamesUpdated();
	}
}
