package technology.rocketjump.mountaincore.entities.components;

import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;

public class LiquidAmountChangedMessage {

	public final Entity parentEntity;
	public final GameMaterial liquidMaterial;
	public final float oldQuantity;
	public final float newQuantity;

	public LiquidAmountChangedMessage(Entity parentEntity, GameMaterial liquidMaterial, float oldQuantity, float newQuantity) {
		this.parentEntity = parentEntity;
		this.liquidMaterial = liquidMaterial;
		this.oldQuantity = oldQuantity;
		this.newQuantity = newQuantity;
	}
}
