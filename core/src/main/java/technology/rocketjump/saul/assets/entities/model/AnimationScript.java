package technology.rocketjump.saul.assets.entities.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import technology.rocketjump.saul.audio.model.SoundAsset;
import technology.rocketjump.saul.particles.model.ParticleEffectType;

import java.util.List;

public class AnimationScript {
	private float duration;
	private List<RotationFrame> rotations;
	private List<TranslationFrame> translations;
	private List<ScalingFrame> scalings;
	private List<SoundCueFrame> soundCues;
	private List<ParticleEffectCueFrame> particleEffectCues;

	public float getDuration() {
		return duration;
	}

	public void setDuration(float duration) {
		this.duration = duration;
	}

	public List<RotationFrame> getRotations() {
		return rotations;
	}

	public void setRotations(List<RotationFrame> rotations) {
		this.rotations = rotations;
	}

	public List<TranslationFrame> getTranslations() {
		return translations;
	}

	public void setTranslations(List<TranslationFrame> translations) {
		this.translations = translations;
	}

	public List<ScalingFrame> getScalings() {
		return scalings;
	}

	public void setScalings(List<ScalingFrame> scalings) {
		this.scalings = scalings;
	}

	public List<SoundCueFrame> getSoundCues() {
		return soundCues;
	}

	public void setSoundCues(List<SoundCueFrame> soundCues) {
		this.soundCues = soundCues;
	}

	public List<ParticleEffectCueFrame> getParticleEffectCues() {
		return particleEffectCues;
	}

	public void setParticleEffectCues(List<ParticleEffectCueFrame> particleEffectCues) {
		this.particleEffectCues = particleEffectCues;
	}

	public abstract static class Frame {
		private float atTime;

		public float getAtTime() {
			return atTime;
		}

		public void setAtTime(float atTime) {
			this.atTime = atTime;
		}
	}

	public static class RotationFrame extends Frame {
		private int roll;


		public int getRoll() {
			return roll;
		}

		public void setRoll(int roll) {
			this.roll = roll;
		}
	}

	public static class TranslationFrame extends Frame {
		@JsonUnwrapped
		private StorableVector2 vector2;

		public StorableVector2 getVector2() {
			return vector2;
		}

		public void setVector2(StorableVector2 vector2) {
			this.vector2 = vector2;
		}
	}

	public static class ScalingFrame extends Frame {
		@JsonUnwrapped
		private StorableVector2 vector2;

		public StorableVector2 getVector2() {
			return vector2;
		}

		public void setVector2(StorableVector2 vector2) {
			this.vector2 = vector2;
		}
	}

	public static class SoundCueFrame extends Frame {
		private String soundAssetName;
		@JsonIgnore
		private SoundAsset soundAsset;

		public String getSoundAssetName() {
			return soundAssetName;
		}

		public void setSoundAssetName(String soundAssetName) {
			this.soundAssetName = soundAssetName;
		}

		public SoundAsset getSoundAsset() {
			return soundAsset;
		}

		public void setSoundAsset(SoundAsset soundAsset) {
			this.soundAsset = soundAsset;
		}
	}

	public static class ParticleEffectCueFrame extends Frame {

		private String particleEffectName;
		@JsonIgnore
		private ParticleEffectType particleEffectType;

		public String getParticleEffectName() {
			return particleEffectName;
		}

		public void setParticleEffectName(String particleEffectName) {
			this.particleEffectName = particleEffectName;
		}

		public ParticleEffectType getParticleEffectType() {
			return particleEffectType;
		}

		public void setParticleEffectType(ParticleEffectType particleEffectType) {
			this.particleEffectType = particleEffectType;
		}
	}
}
