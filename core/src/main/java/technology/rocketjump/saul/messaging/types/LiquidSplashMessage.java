package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.materials.model.GameMaterial;

public class LiquidSplashMessage {

	public final Entity targetEntity;
	public final GameMaterial liquidMaterial;

	public LiquidSplashMessage(Entity targetEntity, GameMaterial liquidMaterial) {
		this.targetEntity = targetEntity;
		this.liquidMaterial = liquidMaterial;
	}
}
