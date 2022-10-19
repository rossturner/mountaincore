package technology.rocketjump.saul.screens.menus;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.environment.GameClock;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.modding.ModCompatibilityChecker;
import technology.rocketjump.saul.persistence.SavedGameInfo;
import technology.rocketjump.saul.persistence.SavedGameStore;
import technology.rocketjump.saul.persistence.UserPreferences;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.i18n.I18nWord;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.MenuButtonFactory;
import technology.rocketjump.saul.ui.widgets.NotificationDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

@Singleton
public class LoadGameMenu implements Menu, GameContextAware {

//	private final Table outerTable;
//
//	private final Table savedGamesTable;
//	private final ClickableTableFactory clickableTableFactory;
//	private final SoundAsset startGameSound;
//	private ScrollPane scrollPane;
//
//	private final IconButton backButton;
//	private final Skin uiSkin;
//	private final MessageDispatcher messageDispatcher;
//	private final I18nWidgetFactory i18NWidgetFactory;
//	private final ModCompatibilityChecker modCompatibilityChecker;
//	private final IconButtonFactory iconButtonFactory;
//	private final I18nTranslator i18nTranslator;

//	private boolean displayed;
//	private GameContext gameContext;

	private final SavedGameStore savedGameStore;
	private final Skin skin;
	private final I18nTranslator i18nTranslator;
	private final Stack stack = new Stack();
	private final java.util.List<Table> slots;
	private final Button leftArrow;
	private final Button rightArrow;
	private int carouselIndex = 0;

	private SavedGameInfo selectedSavedGame;
	private Container<TextButton> deleteButton;
	private Container<TextButton> playButton;


	@Inject
	public LoadGameMenu(UserPreferences userPreferences, GuiSkinRepository skinRepository, MessageDispatcher messageDispatcher,
						SoundAssetDictionary soundAssetDictionary, MenuButtonFactory menuButtonFactory,
						SavedGameStore savedGameStore, ModCompatibilityChecker modCompatibilityChecker, I18nTranslator i18nTranslator) {
		this.savedGameStore = savedGameStore;
		this.skin = skinRepository.getMenuSkin();
		this.i18nTranslator = i18nTranslator;

		Table table1 = new Table();
		table1.setName("backgroundBase");
		stack.addActor(table1);

		table1 = new Table();
		table1.setName("background");
		table1.setBackground(skin.getDrawable("paper_texture_bg"));

		table1.add().padLeft(121.0f); //.preferredWidth(38.0f);

		table1.add().expandX();

		table1.add().padRight(121.0f);
		stack.addActor(table1);

		table1 = new Table();
		table1.setName("loadGameComponents");
		table1.padLeft(197.0f);
		table1.padRight(197.0f);
		table1.padTop(0.0f);
		table1.padBottom(0.0f);

		Image image = new Image(skin, "title_ribbon_bg_left");

		Table titleTable = new Table();
		titleTable.setName("title");
		titleTable.add(image);
		image = new Image(skin, "title_ribbon_bg_middle");

		titleTable.add(image);

		image = new Image(skin, "title_ribbon_bg_right");
		titleTable.add(image);

		table1.add(titleTable).padTop(84.0f).padBottom(84.0f).colspan(5);
		table1.row();

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

		table1.add(leftArrow).maxWidth(58.0f).maxHeight(127.0f);
		table1.add(slot1).width(412.0f).height(572.0f);
		table1.add(slot2).width(412.0f).height(572.0f);
		table1.add(slot3).width(412.0f).height(572.0f);
		table1.add(rightArrow).width(58.0f).height(127.0f);

		table1.row();

		Table loadControls = new Table();
		loadControls.setName("loadControls");
		this.deleteButton = menuButtonFactory.createButton("GUI.LOAD_GAME.TABLE.DELETE", skin, MenuButtonFactory.ButtonStyle.BTN_SCALABLE)
				.withHeaderFont(50).withEssentialWidth(168.0f) //todo: this is needed else the font isn't sizing properly
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

		Container<TextButton> backButton = menuButtonFactory.createButton("GUI.BACK_LABEL", skin, MenuButtonFactory.ButtonStyle.BTN_SCALABLE)
				.withHeaderFont(50).withEssentialWidth(168.0f) //todo: this is needed else the font isn't sizing properly
				.withAction(() -> {
					messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.TOP_LEVEL_MENU);
				})
				.build();
		this.playButton = menuButtonFactory.createButton("GUI.LOAD_GAME.TABLE.PLAY", skin, MenuButtonFactory.ButtonStyle.BTN_SCALABLE)
				.withHeaderFont(50).withEssentialWidth(168.0f) //todo: this is needed else the font isn't sizing properly
				.withAction(() -> {
//					messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.TOP_LEVEL_MENU);
				})
				.build();

		deleteButton.width(168f).height(72.0f);
		backButton.width(168f).height(72.0f);
		playButton.width(168f).height(72.0f);

		loadControls.add(playButton).fill().spaceRight(18.0f);
		loadControls.add(backButton).fill().spaceLeft(18.0f).spaceRight(18.0f);
		loadControls.add(deleteButton).fill().spaceLeft(18.0f);


		table1.add(loadControls).spaceTop(90.0f).spaceBottom(90.0f).expandX().align(Align.right).colspan(5);
		stack.addActor(table1);



//		this.uiSkin = guiSkinRepository.getDefault();
//		this.messageDispatcher = messageDispatcher;
//		this.i18NWidgetFactory = i18NWidgetFactory;
//		this.iconButtonFactory = iconButtonFactory;
//		this.savedGameStore = savedGameStore;
//		this.modCompatibilityChecker = modCompatibilityChecker;
//		this.clickableTableFactory = clickableTableFactory;
//		this.i18nTranslator = i18nTranslator;
//
//		startGameSound = soundAssetDictionary.getByName("GameStart");

//		savedGamesTable = new Table(uiSkin);
	}

	@Override
	public void show() {
		carouselIndex = 0; //todo: not super happy with this, thinking might be better way to show what save was last loaded
		savedGamesUpdated();
//		displayed = true;
//		reset();
	}

	@Override
	public void hide() {
//		displayed = false;
	}

	@Override
	public void populate(Table containerTable) {
//		containerTable.add(outerTable).center();
		containerTable.add(stack).grow();
	}

	@Override
	public void reset() {
//		outerTable.clearChildren();
//
//		savedGamesTable.clearChildren();
//
//		savedGamesTable.add(i18NWidgetFactory.createLabel("GUI.LOAD_GAME.TABLE.SETTLEMENT_NAME")).pad(10);
//		savedGamesTable.add(i18NWidgetFactory.createLabel("GUI.LOAD_GAME.TABLE.VERSION")).pad(10);
//		savedGamesTable.add(i18NWidgetFactory.createLabel("GUI.LOAD_GAME.TABLE.GAME_TIME")).pad(10);
//		savedGamesTable.add(i18NWidgetFactory.createLabel("GUI.LOAD_GAME.TABLE.DATE_TIME")).pad(10);
//		savedGamesTable.add(new Container<>()).pad(10);
//
//		savedGamesTable.row();
//
//
//		List<SavedGameInfo> savesInOrder = new ArrayList<>(savedGameStore.getAll());
//		savesInOrder.sort((o1, o2) -> o2.lastModifiedTime.compareTo(o1.lastModifiedTime));
//
//		for (SavedGameInfo savedGameInfo : savesInOrder) {
//			if (savedGameInfo.settlementName == null) {
//				continue;
//			}
//
//			ClickableTable saveRow = clickableTableFactory.create();
//			saveRow.setBackground("default-rect");
//
//			saveRow.add(new Label(savedGameInfo.settlementName, uiSkin)).pad(5).expandX();
//			saveRow.add(new Label(savedGameInfo.version, uiSkin)).pad(5).expandX();
//			saveRow.add(new Label(savedGameInfo.formattedGameTime, uiSkin)).pad(5).expandX();
//			saveRow.add(new Label(savedGameInfo.formattedFileModifiedTime, uiSkin)).pad(5).expandX();
//			saveRow.setAction(() -> {
//				if (gameContext != null) {
//					if (gameContext.getSettlementState().getSettlementName().equals(savedGameInfo.settlementName)) {
//						// Same game, just go back to it
//						messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.TOP_LEVEL_MENU);
//						messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_GAME");
//					} else {
//						// different game, save this first
//						messageDispatcher.dispatchMessage(MessageType.PERFORM_SAVE, new GameSaveMessage(false));
//						messageDispatcher.dispatchMessage(MessageType.PERFORM_LOAD, savedGameInfo);
//					}
//				} else {
//					messageDispatcher.dispatchMessage(MessageType.PERFORM_LOAD, savedGameInfo);
//				}
//			});
//			saveRow.setOnClickSoundAction(() -> {
//				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(startGameSound));
//			});
//
//			savedGamesTable.add(saveRow).colspan(4).width(600);
//
//			TextButton deleteButton = new TextButton(i18nTranslator.getTranslatedString("GUI.LOAD_GAME.TABLE.DELETE").toString(), uiSkin);
//			deleteButton.addListener(new ClickListener() {
//				@Override
//				public void clicked (InputEvent event, float x, float y) {
//					NotificationDialog dialog = new NotificationDialog(
//							i18nTranslator.getTranslatedString("GUI.DIALOG.INFO_TITLE"),
//							uiSkin,
//							messageDispatcher
//					);
//					dialog.withText(i18nTranslator.getTranslatedWordWithReplacements("GUI.DIALOG.CONFIRM_DELETE_SAVE",
//							Map.of("name", new I18nWord(savedGameInfo.settlementName)))
//							.breakAfterLength(i18nTranslator.getCurrentLanguageType().getBreakAfterLineLength()));
//
//					dialog.withButton(i18nTranslator.getTranslatedString("GUI.DIALOG.OK_BUTTON"), (Runnable) () -> {
//						savedGameStore.delete(savedGameInfo);
//						reset();
//					});
//					dialog.withButton(i18nTranslator.getTranslatedString("GUI.DIALOG.CANCEL_BUTTON"));
//					messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, dialog);
//
//				}
//			});
//			savedGamesTable.add(deleteButton).pad(5).center().row();
//		}
//
//		outerTable.add(scrollPane).colspan(2).pad(10).left().row();
//
//		outerTable.add(backButton).colspan(2).pad(10).left().row();

	}

	public void savedGamesUpdated() {
		//TODO: update save slots
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
		} else if (carouselIndex == savesInOrder.size()) {
			//todo: do something
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
		settlementName.setHeight(54);

		Container<Label> settlementNameContainer = new Container<>(settlementName);
		settlementNameContainer.height(54);
		settlementNameContainer.setBackground(skin.getDrawable("save_title_ribbon_bg"));

		saveSlot.add(settlementNameContainer).padLeft(10).padRight(10);

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
	}

	private void disablePlayAndDeleteButtons() {
		this.playButton.getActor().setDisabled(true);
		this.playButton.getActor().setTouchable(Touchable.disabled);
		this.deleteButton.getActor().setDisabled(true);
		this.deleteButton.getActor().setTouchable(Touchable.disabled);
	}
}
