package technology.rocketjump.saul.entities.model.physical.item;

// MODDING expose this as a data-driven class
public enum ItemQuality {

	AWFUL(0.35f, 0.6f, 1.3f, "ITEM.QUALITY.PREFIX.AWFUL"),
	POOR(0.7f, 0.8f, 1.15f, "ITEM.QUALITY.PREFIX.POOR"),
	STANDARD(1f, 1f, 1f, null), // no descriptive prefix on standard
	SUPERIOR(1.25f, 1.2f, 0.9f, "ITEM.QUALITY.PREFIX.SUPERIOR"),
	MASTERWORK(1.8f, 1.4f, 0.75f, "ITEM.QUALITY.PREFIX.MASTERWORK");

	public final float valueMultiplier;
	public final float combatMultiplier;
	public final float jobDurationMultiplier;
	public final String i18nKey;

	ItemQuality(float valueMultiplier, float combatMultiplier, float jobDurationMultiplier, String i18nKey) {
		this.valueMultiplier = valueMultiplier;
		this.combatMultiplier = combatMultiplier;
		this.jobDurationMultiplier = jobDurationMultiplier;
		this.i18nKey = i18nKey;
	}
}
