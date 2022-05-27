package technology.rocketjump.saul.screens.menus;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.audio.model.SoundAsset;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.GameSaveMessage;
import technology.rocketjump.saul.messaging.types.RequestSoundMessage;
import technology.rocketjump.saul.modding.ModCompatibilityChecker;
import technology.rocketjump.saul.persistence.SavedGameInfo;
import technology.rocketjump.saul.persistence.SavedGameStore;
import technology.rocketjump.saul.persistence.UserPreferences;
import technology.rocketjump.saul.ui.Scene2DUtils;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.i18n.I18nWord;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Singleton
public class LoadGameMenu implements Menu, GameContextAware {

	private final Table outerTable;

	private final Table savedGamesTable;
	private final ClickableTableFactory clickableTableFactory;
	private final SoundAsset startGameSound;
	private ScrollPane scrollPane;

	private final IconButton backButton;
	private final Skin uiSkin;
	private final MessageDispatcher messageDispatcher;
	private final I18nWidgetFactory i18NWidgetFactory;
	private final SavedGameStore savedGameStore;
	private final ModCompatibilityChecker modCompatibilityChecker;
	private final IconButtonFactory iconButtonFactory;
	private final I18nTranslator i18nTranslator;

	private boolean displayed;
	private GameContext gameContext;

	@Inject
	public LoadGameMenu(UserPreferences userPreferences, GuiSkinRepository guiSkinRepository, MessageDispatcher messageDispatcher,
						IconButtonFactory iconButtonFactory, I18nWidgetFactory i18NWidgetFactory, SoundAssetDictionary soundAssetDictionary,
						SavedGameStore savedGameStore, ModCompatibilityChecker modCompatibilityChecker, ClickableTableFactory clickableTableFactory,
						I18nTranslator i18nTranslator) {
		this.uiSkin = guiSkinRepository.getDefault();
		this.messageDispatcher = messageDispatcher;
		this.i18NWidgetFactory = i18NWidgetFactory;
		this.iconButtonFactory = iconButtonFactory;
		this.savedGameStore = savedGameStore;
		this.modCompatibilityChecker = modCompatibilityChecker;
		this.clickableTableFactory = clickableTableFactory;
		this.i18nTranslator = i18nTranslator;

		startGameSound = soundAssetDictionary.getByName("GameStart");

		outerTable = new Table(uiSkin);
		outerTable.setFillParent(false);
		outerTable.center();
		outerTable.background("default-rect");
//		outerTable.setDebug(true);

		savedGamesTable = new Table(uiSkin);

		scrollPane = Scene2DUtils.wrapWithScrollPane(savedGamesTable, uiSkin);

		backButton = iconButtonFactory.create("GUI.BACK_LABEL", null, Color.LIGHT_GRAY, ButtonStyle.SMALL);
		backButton.setAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.TOP_LEVEL_MENU);
		});

	}

	@Override
	public void show() {
		displayed = true;
		reset();
	}

	@Override
	public void hide() {
		displayed = false;
	}

	@Override
	public void populate(Table containerTable) {
		containerTable.add(outerTable).center();
	}

	@Override
	public void reset() {
		outerTable.clearChildren();

		savedGamesTable.clearChildren();

		savedGamesTable.add(i18NWidgetFactory.createLabel("GUI.LOAD_GAME.TABLE.SETTLEMENT_NAME")).pad(10);
		savedGamesTable.add(i18NWidgetFactory.createLabel("GUI.LOAD_GAME.TABLE.VERSION")).pad(10);
		savedGamesTable.add(i18NWidgetFactory.createLabel("GUI.LOAD_GAME.TABLE.GAME_TIME")).pad(10);
		savedGamesTable.add(i18NWidgetFactory.createLabel("GUI.LOAD_GAME.TABLE.DATE_TIME")).pad(10);
		savedGamesTable.add(new Container<>()).pad(10);

		savedGamesTable.row();


		List<SavedGameInfo> savesInOrder = new ArrayList<>(savedGameStore.getAll());
		savesInOrder.sort((o1, o2) -> o2.lastModifiedTime.compareTo(o1.lastModifiedTime));

		for (SavedGameInfo savedGameInfo : savesInOrder) {
			if (savedGameInfo.settlementName == null) {
				continue;
			}

			ClickableTable saveRow = clickableTableFactory.create();
			saveRow.setBackground("default-rect");

			saveRow.add(new Label(savedGameInfo.settlementName, uiSkin)).pad(5).expandX();
			saveRow.add(new Label(savedGameInfo.version, uiSkin)).pad(5).expandX();
			saveRow.add(new Label(savedGameInfo.formattedGameTime, uiSkin)).pad(5).expandX();
			saveRow.add(new Label(savedGameInfo.formattedFileModifiedTime, uiSkin)).pad(5).expandX();
			saveRow.setAction(() -> {
				if (gameContext != null) {
					if (gameContext.getSettlementState().getSettlementName().equals(savedGameInfo.settlementName)) {
						// Same game, just go back to it
						messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.TOP_LEVEL_MENU);
						messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_GAME");
					} else {
						// different game, save this first
						messageDispatcher.dispatchMessage(MessageType.PERFORM_SAVE, new GameSaveMessage(false));
						messageDispatcher.dispatchMessage(MessageType.PERFORM_LOAD, savedGameInfo);
					}
				} else {
					messageDispatcher.dispatchMessage(MessageType.PERFORM_LOAD, savedGameInfo);
				}
			});
			saveRow.setOnClickSoundAction(() -> {
				messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(startGameSound));
			});

			savedGamesTable.add(saveRow).colspan(4).width(600);

			TextButton deleteButton = new TextButton(i18nTranslator.getTranslatedString("GUI.LOAD_GAME.TABLE.DELETE").toString(), uiSkin);
			deleteButton.addListener(new ClickListener() {
				@Override
				public void clicked (InputEvent event, float x, float y) {
					NotificationDialog dialog = new NotificationDialog(
							i18nTranslator.getTranslatedString("GUI.DIALOG.INFO_TITLE"),
							uiSkin,
							messageDispatcher
					);
					dialog.withText(i18nTranslator.getTranslatedWordWithReplacements("GUI.DIALOG.CONFIRM_DELETE_SAVE",
							Map.of("name", new I18nWord(savedGameInfo.settlementName)))
							.breakAfterLength(i18nTranslator.getCurrentLanguageType().getBreakAfterLineLength()));

					dialog.withButton(i18nTranslator.getTranslatedString("GUI.DIALOG.OK_BUTTON"), (Runnable) () -> {
						savedGameStore.delete(savedGameInfo);
						reset();
					});
					dialog.withButton(i18nTranslator.getTranslatedString("GUI.DIALOG.CANCEL_BUTTON"));
					messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, dialog);

				}
			});
			savedGamesTable.add(deleteButton).pad(5).center().row();
		}

		outerTable.add(scrollPane).colspan(2).pad(10).left().row();

		outerTable.add(backButton).colspan(2).pad(10).left().row();

	}

	public void savedGamesUpdated() {
		if (displayed) {
			reset();
		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
