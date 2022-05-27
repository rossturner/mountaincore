package technology.rocketjump.saul.messaging.types;

import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.furniture.EntityDestructionCause;
import technology.rocketjump.saul.materials.model.GameMaterial;

public class FurnitureDamagedMessage {

	public final Entity targetEntity;
	public final EntityDestructionCause destructionCause;
	public final GameMaterial replacementPrimaryMaterial;
	public final Color metalColor;
	public final Color otherColor;

	public FurnitureDamagedMessage(Entity targetEntity, EntityDestructionCause destructionCause,
								   GameMaterial replacementPrimaryMaterial, Color metalColor, Color otherColor) {
		this.targetEntity = targetEntity;
		this.destructionCause = destructionCause;
		this.replacementPrimaryMaterial = replacementPrimaryMaterial;
		this.metalColor = metalColor;
		this.otherColor = otherColor;
	}
}
