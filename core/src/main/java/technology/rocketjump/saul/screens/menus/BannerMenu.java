package technology.rocketjump.saul.screens.menus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.MenuButtonFactory;

public abstract class BannerMenu implements Menu {
    public static final String DISCORD_URL = "https://discord.gg/M57GrFp";
    protected final Skin menuSkin;
    protected final MenuButtonFactory menuButtonFactory;
    protected final MessageDispatcher messageDispatcher;
    protected final Image discordIconImage;
    protected final Image twitchIconImage;
    protected final Image bannerPoleImage;
    protected final I18nTranslator i18nTranslator;

    private final Stack sceneStack = new Stack();


    public BannerMenu(GuiSkinRepository skinRepository, MenuButtonFactory menuButtonFactory, MessageDispatcher messageDispatcher, I18nTranslator i18nTranslator) {
        this.menuSkin = skinRepository.getMenuSkin();
        this.menuButtonFactory = menuButtonFactory;
        this.messageDispatcher = messageDispatcher;
        this.i18nTranslator = i18nTranslator;
        this.discordIconImage = new Image(menuSkin.getDrawable("icon_discord"), Scaling.fit);
        this.twitchIconImage = new Image(menuSkin.getDrawable("icon_twitch"), Scaling.fit);
        this.bannerPoleImage = new Image(menuSkin.getDrawable("asset_bg_banner_pole"), Scaling.fit); //TODO: Decide on what to do about the pole

        bannerPoleImage.setAlign(Align.top);

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

    public abstract void savedGamesUpdated();

    private Table buildSocialMediaLayer() {
        Container<TextButton> discordButton = menuButtonFactory.createButton("MENU.JOIN_DISCORD", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_3_36PT)
                .withAction(() -> {
                    Gdx.net.openURI(DISCORD_URL);
                })
                .build();

        Container<TextButton> twitchButton = menuButtonFactory.createButton("MENU.LINK_TWITCH_ACCOUNT", menuSkin, MenuButtonFactory.ButtonStyle.BTN_BANNER_4_36PT)
                .withAction(() -> {
                    messageDispatcher.dispatchMessage(MessageType.SWITCH_MENU, MenuType.OPTIONS_MENU);
                })
                .build();

        discordButton.getActor().add(discordIconImage).size(100, 86).padLeft(20f).padRight(88f);
        discordButton.getActor().getLabel().setAlignment(Align.right);

        twitchButton.getActor().add(twitchIconImage).size(90, 100).padLeft(20f).padRight(100f);
        twitchButton.getActor().getLabel().setAlignment(Align.right);

        Table table = new Table();
        table.defaults().padBottom(88f);
        table.add(twitchButton).padLeft(82f);
        table.add(discordButton).padLeft(72f);
        table.bottom().left();

        return table;
    }

    private Actor buildMainMenuLayer() {
        //main banner
        Table mainBanner = new Table();
        mainBanner.background(menuSkin.getDrawable("asset_bg_banner"));

        Table mainBannerComponents = new Table();
        addMainBannerComponents(mainBannerComponents);
        mainBanner.add(mainBannerComponents).top().padTop(478f).expand().fillX();

        //secondary/side banner
        Table secondaryBanner = new Table();
        secondaryBanner.defaults().maxWidth(576f);
        secondaryBanner.setBackground(menuSkin.getDrawable("asset_bg_options_banner_rolled"));

        float secondaryBannerPadTop = 0f;
        String secondaryBannerTitleI18nKey = getSecondaryBannerTitleI18nKey();
        if (secondaryBannerTitleI18nKey != null) {
            Label secondaryBannerTitle = new Label(i18nTranslator.getTranslatedString(secondaryBannerTitleI18nKey).toString(), menuSkin, "secondary_banner_title");
            secondaryBannerTitle.setAlignment(Align.center);
            secondaryBanner.add(secondaryBannerTitle).top().padTop(110f).row();
        }

        Table secondaryBannerComponents = new Table();
        secondaryBannerComponents.defaults().maxWidth(576f);
        addSecondaryBannerComponents(secondaryBannerComponents);
        if (!secondaryBannerComponents.getChildren().isEmpty()) {
            secondaryBannerPadTop = 32f; //this deals with the asset_secondary_banner_bg not having same padding baked in asset like rolled is
            secondaryBanner.setBackground(menuSkin.getDrawable("asset_secondary_banner_bg"));
            secondaryBanner.add(secondaryBannerComponents).top().padTop(258f).expand().fillX();
        }


        Table positioningTable = new Table();
        positioningTable.right().top();
        positioningTable.padRight(230f);
        positioningTable.add(secondaryBanner).width(644f).top().padTop(secondaryBannerPadTop).padRight(128f);
        positioningTable.add(mainBanner).padTop(32f);
        return positioningTable;
    }

    protected String getSecondaryBannerTitleI18nKey() {
        return null;
    }

    protected abstract void addSecondaryBannerComponents(Table secondaryBanner);

    protected abstract void addMainBannerComponents(Table mainBanner);

    protected void disableButton(Container<TextButton> button) {
        button.addAction(Actions.alpha(0.5f));
        button.getActor().setDisabled(true);
        button.getActor().setTouchable(Touchable.disabled);
    }

    protected void enableButton(Container<TextButton> button) {
        button.clearActions();
        button.addAction(Actions.alpha(1f));
        button.getActor().setDisabled(false);
        button.getActor().setTouchable(Touchable.enabled);
    }

    public void rebuild() {
        sceneStack.clear();
        sceneStack.add(bannerPoleImage);
        sceneStack.add(buildSocialMediaLayer());
        sceneStack.add(buildMainMenuLayer());
    }

    @Override
    public boolean showVersionDetails() {
        return true;
    }
}
