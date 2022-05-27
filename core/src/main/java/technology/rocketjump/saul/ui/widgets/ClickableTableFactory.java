package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.audio.model.SoundAsset;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.RequestSoundMessage;
import technology.rocketjump.saul.ui.skins.GuiSkinRepository;

@Singleton
public class ClickableTableFactory {

	private final MessageDispatcher messageDispatcher;
	private final Skin uiSkin;
	private final SoundAsset onEnterSoundAsset;
	private final SoundAsset onClickSoundAsset;

	@Inject
	public ClickableTableFactory(MessageDispatcher messageDispatcher, GuiSkinRepository guiSkinRepository, SoundAssetDictionary soundAssetDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.uiSkin = guiSkinRepository.getDefault();

		this.onEnterSoundAsset = soundAssetDictionary.getByName("MenuHover");
		this.onClickSoundAsset = soundAssetDictionary.getByName("MenuClick");
	}

	public ClickableTable create() {
		ClickableTable clickableTable = new ClickableTable(uiSkin);
		clickableTable.setOnEnter(() -> {
			messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(onEnterSoundAsset));
		});
		clickableTable.setOnClickSoundAction(() -> {
			messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(onClickSoundAsset));
		});
		return clickableTable;
	}
}
