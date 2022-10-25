package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import technology.rocketjump.saul.ui.i18n.I18nText;

/**
 * No title dialog, just content
 * Use this if you want a basic greyish dialog
 */
public class NoTitleDialog extends GameDialog {

    public NoTitleDialog(Skin uiSkin, MessageDispatcher messageDispatcher) {
        super(I18nText.BLANK, uiSkin, messageDispatcher);

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
        dialog.getContentTable().add(exitButton).expandX().align(Align.topLeft);
        dialog.getContentTable().row();
    }

    @Override
    public void dispose() {

    }
}
