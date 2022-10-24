package technology.rocketjump.saul.rendering;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.ui.fonts.FontRepository;
import technology.rocketjump.saul.ui.fonts.GameFont;
import technology.rocketjump.saul.ui.widgets.I18nWidgetFactory;

import static technology.rocketjump.saul.rendering.camera.DisplaySettings.GUI_DESIGN_SIZE;

@Singleton
public class InfoWindow implements Telegraph {

	private final Label label;
	private final Stage stage;
	private final Table alignmentTable;

	private boolean isDisplayed = false;

	@Inject
	public InfoWindow(FontRepository fontRepository,
					  MessageDispatcher messageDispatcher, I18nWidgetFactory i18NWidgetFactory) {
		ExtendViewport viewport = new ExtendViewport(GUI_DESIGN_SIZE.x, GUI_DESIGN_SIZE.y);
		stage = new Stage(viewport);

		Skin uiSkin = new Skin(new FileHandle("assets/ui/libgdx-default/uiskin.json")); // MODDING expose this or change uiskin.json

		alignmentTable = new Table(uiSkin);
		alignmentTable.setFillParent(true);

		GameFont largestFont = fontRepository.getLargestFont();
		Label.LabelStyle labelStyle = new Label.LabelStyle(largestFont.getBitmapFont(), Color.WHITE);
		label = i18NWidgetFactory.createLabel("GUI.SAVING_PROMPT");
		label.setStyle(labelStyle);

		alignmentTable.add(label).top().pad(70f);

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

}
