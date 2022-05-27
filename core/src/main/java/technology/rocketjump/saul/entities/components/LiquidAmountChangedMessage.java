package technology.rocketjump.saul.entities.components;

import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.materials.model.GameMaterial;

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
