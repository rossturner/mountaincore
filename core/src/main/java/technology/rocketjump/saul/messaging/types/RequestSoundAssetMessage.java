package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.audio.model.SoundAsset;

import java.util.function.Consumer;

public class RequestSoundAssetMessage {

	public final String assetName;
	public final Consumer<SoundAsset> callback;

	public RequestSoundAssetMessage(String assetName, Consumer<SoundAsset> callback) {
		this.assetName = assetName;
		this.callback = callback;
	}
}
