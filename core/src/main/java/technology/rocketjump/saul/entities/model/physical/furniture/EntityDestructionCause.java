package technology.rocketjump.saul.entities.model.physical.furniture;

public enum EntityDestructionCause {

	BURNED("ENTITY.DESTRUCTION_DESCRIPTION.BURNED"),
	OXIDISED("ENTITY.DESTRUCTION_DESCRIPTION.OXIDISED"),
	COMBAT_DAMAGE("ENTITY.DESTRUCTION_DESCRIPTION.COMBAT_DAMAGE");

	public final String i18nKey;

	EntityDestructionCause(String i18nKey) {
		this.i18nKey = i18nKey;
	}
}
