package technology.rocketjump.saul.screens.menus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.GameSaveMessage;
import technology.rocketjump.saul.persistence.PersistenceCallback;
import technology.rocketjump.saul.persistence.SavedGameStore;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.i18n.LanguageType;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.CustomSelect;
import technology.rocketjump.saul.ui.widgets.MenuButtonFactory;
import technology.rocketjump.saul.ui.widgets.WidgetFactory;

@Singleton
public class TopLevelMenu extends BannerMenu implements DisplaysText {
    private final WidgetFactory widgetFactory;
    private final SavedGameStore savedGameStore;
    private Container<TextButton> continueGameButton;
    private Container<TextButton> loadGameButton;
    protected boolean gameStarted = false;

    @Inject
    public TopLevelMenu(GuiSkinRepository skinRepository, MenuButtonFactory menuButtonFactory, WidgetFactory widgetFactory, MessageDispatcher messageDispatcher, SavedGameStore savedGameStore, I18nTranslator i18nTranslator) {
        super(skinRepository, menuButtonFactory, messageDispatcher, i18nTranslator);
        this.savedGameStore = savedGameStore;
        this.widgetFactory = widgetFactory;

        rebuild();
    }


    @Override
    protected void addMainBannerComponents(Table buttonsTable) {

        Container<TextButton> continueButton = menuButtonFactory.createButton("MENU.CONTINUE_GAME", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_1_47PT)
                .withScaledToFitLabel(614)
                .withAction(() -> {
                    if (gameStarted) {
                        messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_GAME");
                    } else {
                        messageDispatcher.dispatchMessage(MessageType.TRIGGER_QUICKLOAD, (PersistenceCallback) wasSuccessful -> {
                            if (wasSuccessful) {
                                gameStarted = true;
                            }
                        });
                    }
                })
                .build();

        Container<TextButton> loadGameButton = menuButtonFactory.createButton("MENU.LOAD_GAME", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_1_47PT)
                .withScaledToFitLabel(614)
                .withAction(() -> {
                    messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.LOAD_GAME_MENU);
                })
                .build();

        Container<TextButton> newGameButton = menuButtonFactory.createButton("MENU.NEW_GAME", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_1_47PT)
                .withScaledToFitLabel(614)
                .withAction(() -> {
                    messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.EMBARK_MENU);
                })
                .build();

        int lesserImportanceWidth = 554;
        Container<TextButton> optionsButton = menuButtonFactory.createButton("MENU.OPTIONS", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_1_47PT)
                .withScaledToFitLabel(lesserImportanceWidth)
                .withAction(() -> {
                    messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.OPTIONS_MENU);
                })
                .build();

        Container<TextButton> modsButton = menuButtonFactory.createButton("MENU.MODS", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_2_47PT)
                .withScaledToFitLabel(lesserImportanceWidth)
                .withAction(() -> {
                    messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.MODS_MENU);
                })
                .build();

        Container<TextButton> creditsButton = menuButtonFactory.createButton("MENU.CREDITS", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_3_47PT)
                .withScaledToFitLabel(lesserImportanceWidth)
                .build();

        Container<TextButton> quitButton = menuButtonFactory.createButton("MENU.QUIT", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_4_47PT)
                .withScaledToFitLabel(lesserImportanceWidth)
                .withAction(() -> {
                    messageDispatcher.dispatchMessage(MessageType.PERFORM_SAVE, new GameSaveMessage(false));
                    Gdx.app.exit();
                })
                .build();

        float quitButtonScale = (lesserImportanceWidth - quitButton.getPrefWidth()) / quitButton.getPrefWidth();
        optionsButton.scaleBy(quitButtonScale);
        modsButton.scaleBy(quitButtonScale);
        creditsButton.scaleBy(quitButtonScale);
        quitButton.scaleBy(quitButtonScale);

        CustomSelect<LanguageType> languageSelect = widgetFactory.createLanguageSelectBox(menuSkin);

        this.continueGameButton = continueButton;
        this.loadGameButton = loadGameButton;

        disableButton(continueButton);
        disableButton(loadGameButton);
        disableButton(modsButton);


        buttonsTable.add(continueButton).padBottom(30f).row();
        buttonsTable.add(loadGameButton).padBottom(30f).row();
        buttonsTable.add(newGameButton).padBottom(30f).row();
        buttonsTable.add(optionsButton).padBottom(30f).height(116).row();
        buttonsTable.add(modsButton).padBottom(26f).height(116).row();
        buttonsTable.add(creditsButton).padBottom(26f).height(116).row();
        buttonsTable.add(quitButton).padBottom(26f).height(116).row();
        buttonsTable.add(languageSelect).padBottom(416f).height(80).width(lesserImportanceWidth).row();
        buttonsTable.bottom();

    }


    public void savedGamesUpdated() {
        if (savedGameStore.hasSave()) {
            enableButton(loadGameButton);
        }
        if (savedGameStore.hasSave() || gameStarted) {
            enableButton(continueGameButton);
        }
    }

    @Override
    protected void addSecondaryBannerComponents(Table secondaryBanner) {

    }

    @Override
    protected Actor getMainBannerLogo() {
        return new Table();
    }


    public void gameStarted() {
        this.gameStarted = true;
    }

    @Override
    public void rebuildUI() {
        super.rebuild();
    }
}
