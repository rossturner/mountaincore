package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.cooking.model.CookingRecipe;
import technology.rocketjump.saul.entities.model.Entity;

public class CookingCompleteMessage {
	public final Entity targetFurnitureEntity;
	public final CookingRecipe cookingRecipe;

	public CookingCompleteMessage(Entity targetFurnitureEntity, CookingRecipe cookingRecipe) {
		this.targetFurnitureEntity = targetFurnitureEntity;
		this.cookingRecipe = cookingRecipe;
	}
}
