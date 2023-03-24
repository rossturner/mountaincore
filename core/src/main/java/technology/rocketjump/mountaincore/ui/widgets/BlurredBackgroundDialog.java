package technology.rocketjump.mountaincore.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.ui.i18n.I18nText;

/**
 * hacky dialog to allow the "X" to live outside the dialog contents
 */
public class BlurredBackgroundDialog extends GameDialog {

	public BlurredBackgroundDialog(I18nText titleText, Skin uiSkin, MessageDispatcher messageDispatcher, SoundAssetDictionary soundAssetDictionary) {
		super(titleText, uiSkin, messageDispatcher, uiSkin.get("empty_dialog", Window.WindowStyle.class), soundAssetDictionary);
		dialog.setFillParent(true);
	}

	@Override
	public void dispose() {

	}

	public Dialog getDialog() {
		return dialog;
	}

}
