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
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.ClickableSoundsListener;
import technology.rocketjump.saul.ui.eventlistener.TooltipFactory;
import technology.rocketjump.saul.ui.eventlistener.TooltipLocationHint;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.i18n.I18nWord;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.LabelFactory;
import technology.rocketjump.saul.ui.widgets.MenuButtonFactory;
import technology.rocketjump.saul.ui.widgets.NoTitleDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

@Singleton
public class LoadGameMenu extends PaperMenu implements GameContextAware, DisplaysText {

	private static final float SAVE_SLOT_WIDTH = 824.0f;
	private static final float SAVE_SLOT_HEIGHT = 1144.0f;
	public static final String SAVE_BG = "save_bg_patch";
	public static final String SELECTED_SAVE_BG = "selected_save_bg_patch";
	private final SoundAsset startGameSound;
	private final MessageDispatcher messageDispatcher;
	private final SoundAssetDictionary soundAssetDictionary;
	private final MenuButtonFactory menuButtonFactory;
	private final SavedGameStore savedGameStore;
	private final Skin mainGameSkin;
	private final I18nTranslator i18nTranslator;
	private final TooltipFactory tooltipFactory;
	private final LabelFactory labelFactory;
	private int carouselIndex = 0;

	private java.util.List<Table> slots;
	private java.util.List<Table> slotOverlays;
	private Button leftArrow;
	private Button rightArrow;
	private GameContext gameContext;
	private SavedGameInfo selectedSavedGame;
	private Container<TextButton> deleteButton;
	private Container<TextButton> playButton;


	@Inject
	public LoadGameMenu(GuiSkinRepository skinRepository, MessageDispatcher messageDispatcher,
	                    SoundAssetDictionary soundAssetDictionary, MenuButtonFactory menuButtonFactory,
	                    SavedGameStore savedGameStore, I18nTranslator i18nTranslator,
	                    TooltipFactory tooltipFactory, LabelFactory labelFactory) {
		super(skinRepository);
		this.messageDispatcher = messageDispatcher;
		this.soundAssetDictionary = soundAssetDictionary;
		this.menuButtonFactory = menuButtonFactory;
		this.savedGameStore = savedGameStore;
		this.mainGameSkin = skinRepository.getMainGameSkin();
		this.i18nTranslator = i18nTranslator;
		this.startGameSound = soundAssetDictionary.getByName("GameStart");
		this.tooltipFactory = tooltipFactory;
		this.labelFactory = labelFactory;

		rebuildUI();
	}

	@Override
	public void savedGamesUpdated() {
		for (Table slot : slots) {
			slot.clearChildren();
			slot.setBackground(skin.getDrawable("save_greyed_out_bg"));
		}
		for (Table slotOverlay : slotOverlays) {
			slotOverlay.clearChildren();
		}

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
			Table slotOverlay = slotOverlays.get(i);
			SavedGameInfo savedGame = savesInOrder.get(i + carouselIndex);
			populateSaveSlot(savedGame, slotTable, slotOverlay);
		}
	}


	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}

	private void populateSaveSlot(SavedGameInfo savedGameInfo, Table saveSlot, Table slotOverlay) {

		if (savedGameInfo.peacefulMode) {
			Image peacefulModeImage = new Image(skin.getDrawable("icon_peaceful_mode"));
//			peacefulModeImage.setTouchable(Touchable.disabled);
			tooltipFactory.simpleTooltip(peacefulModeImage, "GUI.EMBARK.PEACEFUL_MODE", TooltipLocationHint.BELOW);
			slotOverlay.add(peacefulModeImage).top().left().padLeft(60f).padTop(50f).expand();
		}

		GameClock gameClock = savedGameInfo.gameClock;

		saveSlot.setTouchable(Touchable.enabled);
		saveSlot.clearListeners();

		saveSlot.addListener(new ChangeCursorOnHover(saveSlot, GameCursor.SELECT, messageDispatcher));
		saveSlot.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
		saveSlot.addListener(new ClickListener() {

			@Override
			public void clicked(InputEvent event, float x, float y) {
				int tapCount = getTapCount();

				if (tapCount == 1) {
					for (Table slot : slots) {
						if (!slot.getChildren().isEmpty()) {
							slot.setBackground(skin.getDrawable(SAVE_BG));
						}
					}

					if (selectedSavedGame != savedGameInfo) {
						saveSlot.setBackground(skin.getDrawable(SELECTED_SAVE_BG));

						selectedSavedGame = savedGameInfo;
						enablePlayAndDeleteButtons();
					} else {
						selectedSavedGame = null;
						disablePlayAndDeleteButtons();
					}
				} else if (tapCount == 2) {
					selectedSavedGame = savedGameInfo;
					playSavedGame();
				}

			}


		});

		if (selectedSavedGame == savedGameInfo) {
			saveSlot.setBackground(skin.getDrawable(SELECTED_SAVE_BG));
		} else {
			saveSlot.setBackground(skin.getDrawable(SAVE_BG));
		}

		saveSlot.add();
		saveSlot.row();

		Label settlementName = new Label(savedGameInfo.settlementName, skin, "save_title_ribbon");
		settlementName.setAlignment(Align.center);

		saveSlot.add(settlementName).padLeft(10).padRight(10);

		saveSlot.row();

		saveSlot.add(new Label(gameClock.getFormattedGameTime(), skin, "white_text_default-font-23")).spaceTop(26.0f).spaceBottom(26.0f);
		saveSlot.row();

		//TODO: refactor a season widget (label and icon)
		Label seasonLabel = new Label(i18nTranslator.translate(gameClock.getCurrentSeason().getI18nKey()), skin, "white_text_default-font-23");
		Image seasonIcon = new Image(mainGameSkin.getDrawable("asset_season_" + gameClock.getCurrentSeason().name().toLowerCase() + "_icon"));

		HorizontalGroup season = new HorizontalGroup();
		season.space(12f);
		season.addActor(seasonLabel);
		season.addActor(seasonIcon);
		saveSlot.add(season).spaceTop(26.0f).spaceBottom(26.0f);

		saveSlot.row();
		saveSlot.add(new Label(i18nTranslator.getDayString(gameClock).toString(), skin, "white_text_default-font-23")).spaceTop(26.0f).spaceBottom(26.0f);

		saveSlot.row();
		saveSlot.add(new Label(i18nTranslator.getYearString(gameClock).toString(), skin, "white_text_default-font-23")).spaceTop(26.0f).spaceBottom(26.0f);

		saveSlot.row();
		saveSlot.add(new Label(savedGameInfo.version, skin, "white_text_default-font-23")).spaceTop(26.0f).spaceBottom(26.0f);

		saveSlot.row();
		saveSlot.add(new Label(savedGameInfo.formattedFileModifiedTime, skin, "white_text_default-font-23")).spaceTop(26.0f).spaceBottom(26.0f);
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

	@Override
	protected Actor buildComponentLayer() {
		Table table = new Table();
		table.setName("loadGameComponents");
		table.padLeft(36.0f);
		table.padRight(36.0f);
		table.padTop(0.0f);
		table.padBottom(0.0f);

		Table titleTable = new Table();
		titleTable.setName("title");

		Label titleRibbon = labelFactory.titleRibbon("MENU.LOAD_GAME");
		titleTable.add(titleRibbon).width(1146);


		table.add(titleTable).padTop(84.0f).padBottom(84.0f).colspan(5);
		table.row();

		this.leftArrow = new Button(skin, "left_arrow");
		leftArrow.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
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

		Table slot1Overlay = new Table();
		Table slot2Overlay = new Table();
		Table slot3Overlay = new Table();

		Stack stack1 = new Stack();
		stack1.add(slot1);
		stack1.add(slot1Overlay);
		Stack stack2 = new Stack();
		stack2.add(slot2);
		stack2.add(slot2Overlay);
		Stack stack3 = new Stack();
		stack3.add(slot3);
		stack3.add(slot3Overlay);


		this.slots = Arrays.asList(slot1, slot2, slot3);
		this.slotOverlays = Arrays.asList(slot1Overlay, slot2Overlay, slot3Overlay);


		this.rightArrow = new Button(skin, "right_arrow");
		rightArrow.addListener(new ClickListener() {
			@Override
			public void clicked(InputEvent event, float x, float y) {
				carouselIndex++;
				savedGamesUpdated();
			}
		});

		table.add(leftArrow);
		table.add(stack1).width(SAVE_SLOT_WIDTH).height(SAVE_SLOT_HEIGHT);
		table.add(stack2).width(SAVE_SLOT_WIDTH).height(SAVE_SLOT_HEIGHT);
		table.add(stack3).width(SAVE_SLOT_WIDTH).height(SAVE_SLOT_HEIGHT);
		table.add(rightArrow);

		table.row();

		Table loadControls = new Table();
		loadControls.setName("loadControls");
		this.deleteButton = menuButtonFactory.createButton("GUI.LOAD_GAME.TABLE.DELETE", skin, MenuButtonFactory.ButtonStyle.BTN_SCALABLE_50PT)
				.withAction(() -> {
					if (selectedSavedGame == null) {
						return;
					}

					NoTitleDialog dialog = new NoTitleDialog(skin, messageDispatcher, soundAssetDictionary);

					I18nText dialogText = i18nTranslator.getTranslatedWordWithReplacements("GUI.DIALOG.CONFIRM_DELETE_SAVE",
									Map.of("name", new I18nWord(selectedSavedGame.settlementName)))
							.breakAfterLength(i18nTranslator.getCurrentLanguageType().getBreakAfterLineLength());
					dialog.getContentTable().add(new Label(dialogText.toString(), skin, "white_text_default-font-23")).growY();

					dialog.withButton(i18nTranslator.getTranslatedString("GUI.DIALOG.CONFIRM"), (Runnable) () -> {
						savedGameStore.delete(selectedSavedGame);
						carouselIndex = 0;
						selectedSavedGame = null;
						disablePlayAndDeleteButtons();
						savedGamesUpdated();
					}, skin.get("btn_dialog_1", TextButton.TextButtonStyle.class));
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
					playSavedGame();
				})
				.build();

		deleteButton.width(336f).height(144.0f);
		backButton.width(336f).height(144.0f);
		playButton.width(336f).height(144.0f);

		loadControls.add(deleteButton).fill().spaceLeft(36.0f);
		loadControls.add(backButton).fill().spaceLeft(36.0f).spaceRight(36.0f);
		loadControls.add(playButton).fill().spaceRight(36.0f);

		disablePlayAndDeleteButtons();

		table.add(loadControls).spaceTop(180.0f).spaceBottom(180.0f).expandX().align(Align.right).colspan(5);

		return table;
	}

	private void playSavedGame() {
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
	}

	@Override
	public void show() {
		carouselIndex = 0; //todo: not super happy with this, thinking might be better way to show what save was last loaded
		savedGamesUpdated();
	}

	@Override
	public void rebuildUI() {
		rebuild();
	}
}
