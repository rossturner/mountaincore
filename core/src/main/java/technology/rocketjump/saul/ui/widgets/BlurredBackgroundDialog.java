package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.ClickableSoundsListener;
import technology.rocketjump.saul.ui.i18n.I18nText;

public class BlurredBackgroundDialog extends GameDialog {

	public BlurredBackgroundDialog(I18nText titleText, Skin uiSkin, MessageDispatcher messageDispatcher, Window.WindowStyle windowStyle, SoundAssetDictionary soundAssetDictionary) {
		super(titleText, uiSkin, messageDispatcher, windowStyle);

		Button exitButton = new Button(uiSkin, "btn_exit");
		exitButton.addListener(new ChangeListener() {
			@Override
			public void changed(ChangeEvent event, Actor actor) {
				if (fullScreenOverlay != null) {
					fullScreenOverlay.remove();
				}
				dialog.hide();
				dispose();
			}
		});
		exitButton.addListener(new ChangeCursorOnHover(exitButton, GameCursor.SELECT, messageDispatcher));
		exitButton.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
		dialog.getContentTable().add(exitButton).expandX().align(Align.topLeft);
		dialog.getContentTable().row();
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
