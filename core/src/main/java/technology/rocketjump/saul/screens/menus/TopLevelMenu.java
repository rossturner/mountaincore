package technology.rocketjump.saul.screens.menus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.GameSaveMessage;
import technology.rocketjump.saul.persistence.PersistenceCallback;
import technology.rocketjump.saul.persistence.SavedGameStore;
import technology.rocketjump.saul.ui.i18n.LanguageType;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.CustomSelect;
import technology.rocketjump.saul.ui.widgets.MenuButtonFactory;
import technology.rocketjump.saul.ui.widgets.WidgetFactory;

@Singleton
public class TopLevelMenu implements Menu {
    private final Skin menuSkin;
    private final MenuButtonFactory menuButtonFactory;
    private final WidgetFactory widgetFactory;
    private final MessageDispatcher messageDispatcher;
    private final Stack sceneStack = new Stack();
    private final Image discordIconImage;
    private final Image twitchIconImage;
    private final Image bannerPoleImage;
    private final SavedGameStore savedGameStore;
    private Container<TextButton> continueGameButton;
    private Container<TextButton> loadGameButton;
    private boolean gameStarted = false;

    @Inject
    public TopLevelMenu(GuiSkinRepository skinRepository, MenuButtonFactory menuButtonFactory, WidgetFactory widgetFactory, MessageDispatcher messageDispatcher, SavedGameStore savedGameStore) {
        this.menuSkin = skinRepository.getMenuSkin();
        this.menuButtonFactory = menuButtonFactory;
        this.widgetFactory = widgetFactory;
        this.messageDispatcher = messageDispatcher;
        this.savedGameStore = savedGameStore;

        this.discordIconImage = new Image(menuSkin.getDrawable("icon_discord"), Scaling.fit);
        this.twitchIconImage = new Image(menuSkin.getDrawable("icon_twitch"), Scaling.fit);
        this.bannerPoleImage = new Image(menuSkin.getDrawable("asset_bg_banner_pole"), Scaling.fit); //TODO: Decide on what to do about the pole

        bannerPoleImage.setAlign(Align.top);

        sceneStack.add(bannerPoleImage);
        sceneStack.add(buildSocialMediaLayer());
        sceneStack.add(buildMainMenuLayer());
    }


    @Override
    public void show() {

    }

    @Override
    public void hide() {

    }

    @Override
    public void populate(Table containerTable) {
        containerTable.add(sceneStack).expand().fill();
        savedGamesUpdated();
    }

    @Override
    public void reset() {
    }

    private Table buildSocialMediaLayer() {
        Container<TextButton> discordButton = menuButtonFactory.createButton("MENU.JOIN_DISCORD", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_3)
                .withHeaderFont(36)
                .build();

        Container<TextButton> twitchButton = menuButtonFactory.createButton("MENU.LINK_TWITCH_ACCOUNT", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_4)
                .withHeaderFont(36)
                .build();

        discordButton.getActor().add(discordIconImage).size(50, 43).padLeft(10f).padRight(44f);
        discordButton.getActor().getLabel().setAlignment(Align.right);

        twitchButton.getActor().add(twitchIconImage).size(45, 50).padLeft(10f).padRight(50f);
        twitchButton.getActor().getLabel().setAlignment(Align.right);

        Table table = new Table();
        table.defaults().padBottom(44f);
        table.add(twitchButton).padLeft(41f);
        table.add(discordButton).padLeft(36f);
        table.bottom().left();

        return table;
    }


    private Actor buildMainMenuLayer() {

        Container<TextButton> continueButton = menuButtonFactory.createButton("MENU.CONTINUE_GAME", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_1)
                .withHeaderFont(47)
                .withEssentialWidth(307)
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

        Container<TextButton> loadGameButton = menuButtonFactory.createButton("MENU.LOAD_GAME", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_1)
                .withHeaderFont(47)
                .withEssentialWidth(307)
                .withAction(() -> {
                    messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.LOAD_GAME_MENU);
                })
                .build();

        Container<TextButton> newGameButton = menuButtonFactory.createButton("MENU.NEW_GAME", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_1)
                .withHeaderFont(47)
                .withEssentialWidth(307)
                .withAction(() -> {
                    messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.EMBARK_MENU);
                })
                .build();

        Container<TextButton> optionsButton = menuButtonFactory.createButton("MENU.OPTIONS", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_1)
                .withHeaderFont(47)
                .withScaleBy(-0.1f)
                .withEssentialWidth(277)
                .withAction(() -> {
                    messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.OPTIONS_MENU);
                })
                .build();

        Container<TextButton> modsButton = menuButtonFactory.createButton("MENU.MODS", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_2)
                .withHeaderFont(47)
                .withScaleBy(-0.1f)
                .withEssentialWidth(277)
                .withAction(() -> {
                    messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.MODS_MENU);
                })
                .build();

        Container<TextButton> creditsButton = menuButtonFactory.createButton("MENU.CREDITS", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_3)
                .withHeaderFont(47)
                .withScaleBy(-0.1f)
                .withEssentialWidth(277)
                .build();

        Container<TextButton> quitButton = menuButtonFactory.createButton("MENU.QUIT", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_4)
                .withHeaderFont(47)
                .withScaleBy(-0.1f)
                .withEssentialWidth(277)
                .withAction(() -> {
                    messageDispatcher.dispatchMessage(MessageType.PERFORM_SAVE, new GameSaveMessage(false));
                    Gdx.app.exit();
                })
                .build();

        CustomSelect<LanguageType> languageList = widgetFactory.createLanguageSelectBox(menuSkin);


        Table buttonsTable = new Table();
        buttonsTable.background(menuSkin.getDrawable("asset_bg_banner"));
        buttonsTable.add(continueButton).padBottom(17f).row();
        buttonsTable.add(loadGameButton).padBottom(17f).row();
        buttonsTable.add(newGameButton).padBottom(17f).row();
        buttonsTable.add(optionsButton).padBottom(15f).row();
        buttonsTable.add(modsButton).padBottom(15f).row();
        buttonsTable.add(creditsButton).padBottom(15f).row();
        buttonsTable.add(quitButton).padBottom(15f).row();
        buttonsTable.add(languageList).padBottom(208f).width(277).row();
        buttonsTable.bottom();


        Table positioningTable = new Table();
        positioningTable.right().top();
        positioningTable.padTop(16f).padRight(115f);
        positioningTable.add(buttonsTable).expandY().fillY().width(380);

        this.continueGameButton = continueButton;
        this.loadGameButton = loadGameButton;

        disableButton(continueButton);
        disableButton(loadGameButton);

        return positioningTable;
    }

    public void savedGamesUpdated() {
        if (savedGameStore.hasSave()) {
            enableButton(loadGameButton);
        }
        if (savedGameStore.hasSave() || gameStarted) {
            enableButton(continueGameButton);
        }
    }

    public void gameStarted() {
        this.gameStarted = true;
    }

    private void disableButton(Container<TextButton> button) {
        button.addAction(Actions.alpha(0.5f));
        button.getActor().setDisabled(true);
        button.getActor().setTouchable(Touchable.disabled);
    }

    private void enableButton(Container<TextButton> button) {
        button.clearActions();
        button.addAction(Actions.fadeIn(0.5f));
        button.getActor().setDisabled(false);
        button.getActor().setTouchable(Touchable.enabled);
    }
}
