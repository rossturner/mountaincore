package technology.rocketjump.mountaincore.materials.model;

/**
 * Represents how heavy a material is compared to the "average" of its material type
 */
public enum MaterialWeight {

	VERY_HEAVY(1.5f, 1.2f),
	HEAVY(1.3f, 1.1f),
	AVERAGE(1.0f, 1.0f),
	LIGHT(0.7f, 0.9f),
	VERY_LIGHT(0.5f, 0.8f);

	public final float majorModifier;
	public final float minorModifier;

	MaterialWeight(float majorModifier, float minorModifier) {
		this.majorModifier = majorModifier;
		this.minorModifier = minorModifier;
	}
}
