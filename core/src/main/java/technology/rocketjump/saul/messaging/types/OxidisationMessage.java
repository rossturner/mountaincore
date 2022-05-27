package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.materials.model.GameMaterial;

public class OxidisationMessage {

	public final Entity targetEntity;
	public final GameMaterial oxidisedMaterial;

	public OxidisationMessage(Entity targetEntity, GameMaterial oxidisedMaterial) {
		this.targetEntity = targetEntity;
		this.oxidisedMaterial = oxidisedMaterial;
	}
}
