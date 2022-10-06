package technology.rocketjump.saul.screens.menus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.GameSaveMessage;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.MenuButtonFactory;

@Singleton
public class TopLevelMenu implements Menu {
    private final Skin menuSkin;
    private final MenuButtonFactory menuButtonFactory;
    private final MessageDispatcher messageDispatcher;
    private final Stack sceneStack = new Stack();
    private final Image discordIconImage;
    private final Image twitchIconImage;
    private final Image bannerPoleImage;

    @Inject
    public TopLevelMenu(GuiSkinRepository skinRepository, MenuButtonFactory menuButtonFactory, MessageDispatcher messageDispatcher) {
        this.menuSkin = skinRepository.getMenuSkin();
        this.menuButtonFactory = menuButtonFactory;
        this.messageDispatcher = messageDispatcher;

        this.discordIconImage = new Image(menuSkin.getDrawable("icon_discord"), Scaling.fit);
        this.twitchIconImage = new Image(menuSkin.getDrawable("icon_twitch"), Scaling.fit);
        this.bannerPoleImage = new Image(menuSkin.getDrawable("asset_bg_banner_pole"), Scaling.fit);

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

//        discordButton.getActor().add(discordIconImage).size(32, 32);
//        twitchButton.getActor().add(twitchIconImage);

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
                .withAction(() -> {
                    //todo: thinking the gameStarted should be in a context somewhere, not in here?
//                    if (gameStarted) {
//                        messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_GAME");
//                    } else {
//                        messageDispatcher.dispatchMessage(MessageType.TRIGGER_QUICKLOAD, (PersistenceCallback) wasSuccessful -> {
//                            if (wasSuccessful) {
//                                gameStarted = true;
//                            }
//                        });
//                    }
                })
                .build();

        Container<TextButton> loadGameButton = menuButtonFactory.createButton("MENU.LOAD_GAME", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_1)
                .withHeaderFont(47)
                .withAction(() -> {
                    messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.LOAD_GAME_MENU);
                })
                .build();

        Container<TextButton> newGameButton = menuButtonFactory.createButton("MENU.NEW_GAME", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_1)
                .withHeaderFont(47)
                .withAction(() -> {
                    messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.EMBARK_MENU);
                })
                .build();

        Container<TextButton> optionsButton = menuButtonFactory.createButton("MENU.OPTIONS", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_1)
                .withHeaderFont(47)
                .withScaleBy(-0.1f)
                .withAction(() -> {
                    messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.OPTIONS_MENU);
                })
                .build();

        Container<TextButton> modsButton = menuButtonFactory.createButton("MENU.MODS", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_2)
                .withHeaderFont(47)
                .withScaleBy(-0.1f)
                .withAction(() -> {
                    messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.MODS_MENU);
                })
                .build();

        Container<TextButton> creditsButton = menuButtonFactory.createButton("MENU.CREDITS", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_3)
                .withHeaderFont(47)
                .withScaleBy(-0.1f)
                .build();

        Container<TextButton> quitButton = menuButtonFactory.createButton("MENU.QUIT", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_4)
                .withHeaderFont(47)
                .withScaleBy(-0.1f)
                .withAction(() -> {
                    messageDispatcher.dispatchMessage(MessageType.PERFORM_SAVE, new GameSaveMessage(false));
                    Gdx.app.exit();
                })
                .build();



        Table buttonsTable = new Table();
        buttonsTable.background(menuSkin.getDrawable("asset_bg_banner"));
        buttonsTable.add(continueButton).padBottom(17f).width(307).row();
        buttonsTable.add(loadGameButton).padBottom(17f).width(307).row();
        buttonsTable.add(newGameButton).padBottom(17f).width(307).row();
        buttonsTable.add(optionsButton).padBottom(15f).width(277).row();
        buttonsTable.add(modsButton).padBottom(15f).width(277).row();
        buttonsTable.add(creditsButton).padBottom(15f).width(277).row();
        buttonsTable.add(quitButton).padBottom(208f).width(277).row();
        buttonsTable.bottom();

        Table positioningTable = new Table();
        positioningTable.right().top();
        positioningTable.padTop(16f).padRight(115f);
        positioningTable.add(buttonsTable).expandY().fillY().width(380);

        return positioningTable;
    }
}
