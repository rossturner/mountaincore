package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.cooking.model.CookingRecipe;
import technology.rocketjump.mountaincore.entities.model.Entity;

public class CookingCompleteMessage {
	public final Entity targetFurnitureEntity;
	public final CookingRecipe cookingRecipe;

	public CookingCompleteMessage(Entity targetFurnitureEntity, CookingRecipe cookingRecipe) {
		this.targetFurnitureEntity = targetFurnitureEntity;
		this.cookingRecipe = cookingRecipe;
	}
}
