package technology.rocketjump.saul.screens.menus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
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

    @Inject
    public TopLevelMenu(GuiSkinRepository skinRepository, MenuButtonFactory menuButtonFactory, MessageDispatcher messageDispatcher) {
        this.menuSkin = skinRepository.getMenuSkin();
        this.menuButtonFactory = menuButtonFactory;
        this.messageDispatcher = messageDispatcher;

        this.discordIconImage = new Image(menuSkin.getDrawable("icon_discord"), Scaling.fit);
        this.twitchIconImage = new Image(menuSkin.getDrawable("icon_twitch"), Scaling.fit);

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
        Container<TextButton> discordButton = menuButtonFactory.createButton("MENU.JOIN_DISCORD", menuSkin, MenuButtonFactory.ButtonStyle.DEFAULT)
                .withHeaderFont(36)
                .build();
        Container<TextButton> twitchButton = menuButtonFactory.createButton("MENU.LINK_TWITCH_ACCOUNT", menuSkin, MenuButtonFactory.ButtonStyle.DEFAULT)
                .withHeaderFont(36)
                .build();

//        discordButton.getActor().add(discordIconImage);
//        twitchButton.getActor().add(twitchIconImage);

        Table table = new Table();
        table.defaults().padLeft(41f).padBottom(44f);
        table.add(discordButton);
        table.add(twitchButton);
        table.bottom().left();
        return table;
    }


    private Actor buildMainMenuLayer() {

        Container<TextButton> continueButton = menuButtonFactory.createButton("MENU.CONTINUE_GAME", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_1)
                .withHeaderFont(47)
                .withScaleUpOnHoverBy(0.2f)
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
                .withScaleUpOnHoverBy(0.2f)
                .withAction(() -> {
                    messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.LOAD_GAME_MENU);
                })
                .build();

        Container<TextButton> newGameButton = menuButtonFactory.createButton("MENU.NEW_GAME", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_1)
                .withHeaderFont(47)
                .withScaleUpOnHoverBy(0.2f)
                .withAction(() -> {
                    messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.EMBARK_MENU);
                })
                .build();

        Container<TextButton> optionsButton = menuButtonFactory.createButton("MENU.OPTIONS", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_1)
                .withHeaderFont(47)
                .withScaleBy(-0.1f)
                .withScaleUpOnHoverBy(0.2f)
                .withAction(() -> {
                    messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.OPTIONS_MENU);
                })
                .build();

        Container<TextButton> modsButton = menuButtonFactory.createButton("MENU.MODS", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_2)
                .withHeaderFont(47)
                .withScaleBy(-0.1f)
                .withScaleUpOnHoverBy(0.2f)
                .withAction(() -> {
                    messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.MODS_MENU);
                })
                .build();

        Container<TextButton> creditsButton = menuButtonFactory.createButton("MENU.CREDITS", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_3)
                .withHeaderFont(47)
                .withScaleBy(-0.1f)
                .withScaleUpOnHoverBy(0.2f)
                .build();

        Container<TextButton> quitButton = menuButtonFactory.createButton("MENU.QUIT", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_4)
                .withHeaderFont(47)
                .withScaleBy(-0.1f)
                .withScaleUpOnHoverBy(0.2f)
                .withAction(() -> {
                    messageDispatcher.dispatchMessage(MessageType.PERFORM_SAVE, new GameSaveMessage(false));
                    Gdx.app.exit();
                })
                .build();




        Table table = new Table();
        table.add(continueButton).padBottom(17f).row();
        table.add(loadGameButton).padBottom(17f).row();
        table.add(newGameButton).padBottom(17f).row();
        table.add(optionsButton).padBottom(15f).row();
        table.add(modsButton).padBottom(15f).row();
        table.add(creditsButton).padBottom(15f).row();
        table.add(quitButton).padBottom(15f).row();

        return table;
    }
}
