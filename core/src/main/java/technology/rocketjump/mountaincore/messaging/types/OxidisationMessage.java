package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;

public class OxidisationMessage {

	public final Entity targetEntity;
	public final GameMaterial oxidisedMaterial;

	public OxidisationMessage(Entity targetEntity, GameMaterial oxidisedMaterial) {
		this.targetEntity = targetEntity;
		this.oxidisedMaterial = oxidisedMaterial;
	}
}
