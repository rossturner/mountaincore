package technology.rocketjump.mountaincore.messaging.types;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.mountaincore.audio.model.ActiveSoundEffect;
import technology.rocketjump.mountaincore.audio.model.SoundAsset;
import technology.rocketjump.mountaincore.entities.model.Entity;

public class RequestSoundMessage {

	public final SoundAsset soundAsset;
	public final Long requesterId;
	public final Vector2 fixedPosition;
	public final SoundRequestCallback callback;

	public RequestSoundMessage(SoundAsset soundAsset, Long requesterId, Vector2 position, SoundRequestCallback callback) {
		this.soundAsset = soundAsset;
		this.requesterId = requesterId;
		this.fixedPosition = position;
		this.callback = callback;
	}

	/**
	 * This constructor is to be used for UI sounds and other sounds that should always be played
	 */
	public RequestSoundMessage(SoundAsset soundAsset) {
		this(soundAsset, (SoundRequestCallback) null);
	}

	public RequestSoundMessage(SoundAsset soundAsset, SoundRequestCallback callback) {
		this.soundAsset = soundAsset;
		this.requesterId = null;
		this.fixedPosition = null;
		this.callback = callback;
	}

	public RequestSoundMessage(SoundAsset soundAsset, Entity entity) {
		this(soundAsset, entity, null);
	}

	public RequestSoundMessage(SoundAsset soundAsset, Entity entity, SoundRequestCallback callback) {
		this(soundAsset, entity.getId(), entity.getLocationComponent().getWorldOrParentPosition(), callback);
	}

	public interface SoundRequestCallback {

		void soundActivated(ActiveSoundEffect activeSoundEffect);

	}
}
