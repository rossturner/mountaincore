package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import technology.rocketjump.saul.ui.i18n.I18nText;

public class BlurredBackgroundDialog extends GameDialog {

	public BlurredBackgroundDialog(I18nText titleText, Skin uiSkin, MessageDispatcher messageDispatcher, Window.WindowStyle windowStyle) {
		super(titleText, uiSkin, messageDispatcher, windowStyle);
	}

	@Override
	public void dispose() {

	}

	public Dialog getDialog() {
		return dialog;
	}

	@Override
	public void show(Stage stage) {


		super.show(stage);
	}
}
