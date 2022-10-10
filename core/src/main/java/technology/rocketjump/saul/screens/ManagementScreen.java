package technology.rocketjump.saul.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.utils.HexColors;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.widgets.*;

import static technology.rocketjump.saul.rendering.camera.DisplaySettings.GUI_DESIGN_SIZE;

public abstract class ManagementScreen implements GameScreen, GameContextAware {

    protected final MessageDispatcher messageDispatcher;
    protected final I18nTranslator i18nTranslator;
    protected final I18nWidgetFactory i18nWidgetFactory;

    protected final OrthographicCamera camera = new OrthographicCamera();
    protected final Table containerTable;
    protected final Skin uiSkin;
    protected final Stage stage;
    protected final I18nLabel titleLabel;
    protected GameContext gameContext;

    public ManagementScreen(MessageDispatcher messageDispatcher,
                                   GuiSkinRepository guiSkinRepository, I18nWidgetFactory i18nWidgetFactory,
                                   I18nTranslator i18nTranslator, IconButtonFactory iconButtonFactory) {
        this.uiSkin = guiSkinRepository.getDefault();
        this.messageDispatcher = messageDispatcher;
        this.i18nTranslator = i18nTranslator;
        this.i18nWidgetFactory = i18nWidgetFactory;

        containerTable = new Table(uiSkin);
        containerTable.setFillParent(true);
        containerTable.center().top();

        ExtendViewport viewport = new ExtendViewport(GUI_DESIGN_SIZE.x, GUI_DESIGN_SIZE.y);
        stage = new Stage(viewport);
        stage.addActor(containerTable);

        IconButton backButton = iconButtonFactory.create("GUI.BACK_LABEL", "arrow-left", HexColors.get("#D9D9D9"), ButtonStyle.DEFAULT);
        backButton.setAction(() -> messageDispatcher.dispatchMessage(MessageType.SWITCH_SCREEN, "MAIN_GAME"));
        Container<IconButton> backButtonContainer = new Container<>(backButton);
        backButtonContainer.left().bottom();
        stage.addActor(backButtonContainer);

        titleLabel = i18nWidgetFactory.createLabel(getTitleI18nKey());
    }

    public abstract String getTitleI18nKey();



    @Override
    public void show() {
        clearContextRelatedState();

        InputMultiplexer inputMultiplexer = new InputMultiplexer();
        inputMultiplexer.addProcessor(stage);
        inputMultiplexer.addProcessor(new ManagementScreenInputHandler(messageDispatcher));
        Gdx.input.setInputProcessor(inputMultiplexer);

        resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public abstract void reset();

    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, width, height);

        stage.getViewport().update(width, height, true);

        reset();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.4f, 0.4f, 0.4f, 1); // MODDING expose default background color
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        stage.act(delta);

        stage.draw();
    }

    @Override
    public void hide() {
        clearContextRelatedState();
    }

    @Override
    public void showDialog(GameDialog dialog) {
        dialog.show(stage);
    }

    @Override
    public void onContextChange(GameContext gameContext) {
        this.gameContext = gameContext;
    }

}
