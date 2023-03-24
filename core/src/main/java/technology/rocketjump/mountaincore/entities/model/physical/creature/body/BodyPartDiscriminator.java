package technology.rocketjump.mountaincore.entities.model.physical.creature.body;

/**
 * Used to distinguish parts of the same type e.g. Right-Arm, Left-Arm
 */
public enum BodyPartDiscriminator {

	Left,
	Right,

	FrontRight,
	FrontLeft,
	BackRight,
	BackLeft,
	FirstLeft,
	SecondLeft,
	ThirdLeft,
	FourthLeft,
	FirstRight,
	SecondRight,
	ThirdRight,
	FourthRight;

	public String i18nKey() {
		return "BODY_STRUCTURE.DISCRIMINATOR."+name().toUpperCase();
	}
}
