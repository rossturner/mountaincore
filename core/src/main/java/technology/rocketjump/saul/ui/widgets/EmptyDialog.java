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
 */
public class EmptyDialog extends GameDialog {

    public EmptyDialog(Skin uiSkin, MessageDispatcher messageDispatcher) {
        super(I18nText.BLANK, uiSkin, messageDispatcher);

        Button exitButton = new Button(uiSkin, "btn_exit");
        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });
        dialog.getContentTable().add(exitButton).expandX().align(Align.topLeft);
        dialog.getContentTable().row();
    }

    @Override
    public void dispose() {

    }
}
