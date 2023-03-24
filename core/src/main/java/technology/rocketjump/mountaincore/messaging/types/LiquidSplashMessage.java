package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;

public class LiquidSplashMessage {

	public final Entity targetEntity;
	public final GameMaterial liquidMaterial;

	public LiquidSplashMessage(Entity targetEntity, GameMaterial liquidMaterial) {
		this.targetEntity = targetEntity;
		this.liquidMaterial = liquidMaterial;
	}
}
