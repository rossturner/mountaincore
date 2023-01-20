package technology.rocketjump.saul.assets.entities.model;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.List;

public class AnimationScript {
	private float duration;
	private List<RotationFrame> rotations;
	private List<TranslationFrame> translations;

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

	private static class Frame {
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
}
