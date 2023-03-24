package technology.rocketjump.mountaincore.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.ui.i18n.I18nText;

/**
 * No title dialog, just content
 * Use this if you want a basic greyish dialog
 */
public class NoTitleDialog extends GameDialog {

    public NoTitleDialog(Skin uiSkin, MessageDispatcher messageDispatcher, SoundAssetDictionary soundAssetDictionary) {
        super(I18nText.BLANK, uiSkin, messageDispatcher, soundAssetDictionary);
    }

    @Override
    public void dispose() {

    }

}
