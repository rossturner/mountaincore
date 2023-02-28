package technology.rocketjump.saul.ui.eventlistener;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import technology.rocketjump.saul.audio.model.SoundAsset;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.RequestSoundMessage;

public class ClickableSoundsListener extends ClickListener {
    private final MessageDispatcher messageDispatcher;

    private final SoundAsset onEnterSoundAsset;
    private final SoundAsset onClickSoundAsset;
    private boolean entered = false;

    public ClickableSoundsListener(MessageDispatcher messageDispatcher, SoundAssetDictionary soundAssetDictionary) {
        this(messageDispatcher, soundAssetDictionary, "MenuClick");
    }

    public ClickableSoundsListener(MessageDispatcher messageDispatcher, SoundAssetDictionary soundAssetDictionary, String clickSoundName) {
        this.messageDispatcher = messageDispatcher;
        this.onEnterSoundAsset = soundAssetDictionary.getByName("MenuHover");
        this.onClickSoundAsset = soundAssetDictionary.getByName(clickSoundName);
    }


    @Override
    public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
        super.enter(event, x, y, pointer, fromActor);
        if (!entered) {
            entered = true;
            messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(onEnterSoundAsset));
        }
    }

    @Override
    public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
        super.exit(event, x, y, pointer, toActor);
        Actor toCheck = toActor;
        entered = false;
        while (toCheck != null) {
            boolean thisListenerStillOver = toCheck.getListeners().contains(this, true);
            if (thisListenerStillOver) {
                entered = true;
                break;
            }
            toCheck = toCheck.getParent();
        }
    }

    @Override
    public void clicked(InputEvent event, float x, float y) {
        super.clicked(event, x, y);
        messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(onClickSoundAsset));
    }
}
