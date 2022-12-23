package technology.rocketjump.saul.rendering;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.ui.i18n.DisplaysText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;
import technology.rocketjump.saul.ui.skins.MainGameSkin;

import static technology.rocketjump.saul.rendering.camera.DisplaySettings.GUI_DESIGN_SIZE;

@Singleton
public class InfoWindow implements Telegraph, DisplaysText {

	private final Stage stage;
	private final Table alignmentTable;
	private final I18nTranslator i18nTranslator;
	private final MainGameSkin uiSkin;

	private boolean isDisplayed = false;

	@Inject
	public InfoWindow(GuiSkinRepository guiSkinRepository,
	                  MessageDispatcher messageDispatcher, I18nTranslator i18nTranslator) {
		this.i18nTranslator = i18nTranslator;
		this.uiSkin = guiSkinRepository.getMainGameSkin();
		ExtendViewport viewport = new ExtendViewport(GUI_DESIGN_SIZE.x, GUI_DESIGN_SIZE.y);
		stage = new Stage(viewport);


		alignmentTable = new Table();
		alignmentTable.setFillParent(true);



		stage.addActor(alignmentTable);
		stage.act();

		messageDispatcher.addListener(this, MessageType.SHOW_AUTOSAVE_PROMPT);
		messageDispatcher.addListener(this, MessageType.HIDE_AUTOSAVE_PROMPT);
	}

	public void render() {
		if (isDisplayed) {
			stage.draw();
		}
	}

	public void onResize(int screenWidth, int screenHeight) {
		stage.getViewport().update(screenWidth, screenHeight, true);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.SHOW_AUTOSAVE_PROMPT: {
				this.isDisplayed = true;
				return true;
			}
			case MessageType.HIDE_AUTOSAVE_PROMPT: {
				this.isDisplayed = false;
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	@Override
	public void rebuildUI() {
		alignmentTable.clearChildren();
		Label label = new Label(i18nTranslator.translate("GUI.SAVING_PROMPT"), uiSkin, "white_text_default-font-23");
		alignmentTable.add(label).top().pad(70f);
	}
}
