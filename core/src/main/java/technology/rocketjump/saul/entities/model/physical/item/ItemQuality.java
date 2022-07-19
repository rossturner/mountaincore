package technology.rocketjump.saul.entities.model.physical.item;

// MODDING expose this as a data-driven class
public enum ItemQuality {

	Awful(0.35f, 1.3f),
	Poor(0.7f, 1.15f),
	Standard(1f, 1f),
	Superior(1.25f, 0.9f),
	Masterwork(1.8f, 0.75f);

	public final float valueMultiplier;
	public final float jobDurationMultiplier;

	ItemQuality(float valueMultiplier, float jobDurationMultiplier) {
		this.valueMultiplier = valueMultiplier;
		this.jobDurationMultiplier = jobDurationMultiplier;
	}
}
