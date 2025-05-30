package technology.rocketjump.mountaincore.screens.menus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.GameSaveMessage;
import technology.rocketjump.mountaincore.persistence.PersistenceCallback;
import technology.rocketjump.mountaincore.persistence.SavedGameStore;
import technology.rocketjump.mountaincore.ui.i18n.DisplaysText;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;
import technology.rocketjump.mountaincore.ui.i18n.LanguageType;
import technology.rocketjump.mountaincore.ui.skins.GuiSkinRepository;
import technology.rocketjump.mountaincore.ui.widgets.CustomSelect;
import technology.rocketjump.mountaincore.ui.widgets.MenuButtonFactory;
import technology.rocketjump.mountaincore.ui.widgets.WidgetFactory;

@Singleton
public class TopLevelMenu extends BannerMenu implements DisplaysText {
    private final WidgetFactory widgetFactory;
    private final SavedGameStore savedGameStore;
    private final ModsMenu modsMenu;
    private Container<TextButton> continueGameButton;
    private Container<TextButton> loadGameButton;
    protected boolean gameStarted = false;
    private Texture logo;

    @Inject
    public TopLevelMenu(GuiSkinRepository skinRepository, MenuButtonFactory menuButtonFactory,
						WidgetFactory widgetFactory, MessageDispatcher messageDispatcher,
						SavedGameStore savedGameStore, I18nTranslator i18nTranslator, ModsMenu modsMenu) {
        super(skinRepository, menuButtonFactory, messageDispatcher, i18nTranslator);
        this.savedGameStore = savedGameStore;
        this.widgetFactory = widgetFactory;
        this.modsMenu = modsMenu;
        logo = new Texture("assets/main_menu/Mountaincore_Logo.png");
        logo.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

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
                                gameStarted();
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
                    messageDispatcher.dispatchMessage(MessageType.SHOW_DIALOG, modsMenu);
                })
                .build();

        Container<TextButton> creditsButton = menuButtonFactory.createButton("MENU.CREDITS", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_3_47PT)
                .withScaledToFitLabel(lesserImportanceWidth)
                .withAction(() -> {
                    messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.CREDITS_MENU);
                })
                .build();

        Container<TextButton> quitButton = menuButtonFactory.createButton(gameStarted ? "MENU.SAVE_AND_QUIT" : "MENU.QUIT", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_4_47PT)
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
        return new Image(logo);
    }

    public boolean hasGameStarted() {
        return this.gameStarted;
    }

    public void gameStarted() {
        this.gameStarted = true;
        rebuildUI();
    }

    @Override
    public void rebuildUI() {
        super.rebuild();
    }
}
