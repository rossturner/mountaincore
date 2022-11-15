package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.ui.cursor.GameCursor;
import technology.rocketjump.saul.ui.eventlistener.ChangeCursorOnHover;
import technology.rocketjump.saul.ui.eventlistener.ClickableSoundsListener;
import technology.rocketjump.saul.ui.i18n.I18nText;

/**
 * No title dialog, just content
 * Use this if you want a basic greyish dialog
 */
public class NoTitleDialog extends GameDialog {

    private final SoundAssetDictionary soundAssetDictionary;
    private boolean addedCursorChangeListeners = false;

    public NoTitleDialog(Skin uiSkin, MessageDispatcher messageDispatcher, SoundAssetDictionary soundAssetDictionary) {
        super(I18nText.BLANK, uiSkin, messageDispatcher);
        this.soundAssetDictionary = soundAssetDictionary;

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

    @Override
    public void show(Stage stage) {
        if (!addedCursorChangeListeners) {
            for (Actor child : dialog.getButtonTable().getChildren()) {
                child.addListener(new ChangeCursorOnHover(child, GameCursor.SELECT, messageDispatcher));
                child.addListener(new ClickableSoundsListener(messageDispatcher, soundAssetDictionary));
            }
            addedCursorChangeListeners = true;
        }

        super.show(stage);
    }
}
