package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.audio.model.SoundAsset;

public class RequestSoundStopMessage {

	public final SoundAsset soundAsset;
	public final long requesterId;

	public RequestSoundStopMessage(SoundAsset soundAsset, long requesterId) {
		this.soundAsset = soundAsset;
		this.requesterId = requesterId;
	}
}
