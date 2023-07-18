package technology.rocketjump.mountaincore.materials.model;

/**
 * Represents how hard a material is compared to the "average" of its material type
 */
public enum MaterialHardness {

	VERY_HARD(1.5f, 1.2f),
	HARD(1.3f, 1.1f),
	AVERAGE(1.0f, 1.0f),
	SOFT(0.7f, 0.9f),
	VERY_SOFT(0.5f, 0.8f);

	public final float majorModifier;
	public final float minorModifier;

	MaterialHardness(float majorModifier, float minorModifier) {
		this.majorModifier = majorModifier;
		this.minorModifier = minorModifier;
	}
}
