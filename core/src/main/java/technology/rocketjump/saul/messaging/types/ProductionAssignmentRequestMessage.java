package technology.rocketjump.saul.messaging.types;

import technology.rocketjump.saul.crafting.model.CraftingRecipe;
import technology.rocketjump.saul.jobs.model.CraftingType;

import java.util.List;

public class ProductionAssignmentRequestMessage {

	public final CraftingType craftingType;
	public final ProductionAssignmentCallback callback;

	public ProductionAssignmentRequestMessage(CraftingType craftingType, ProductionAssignmentCallback callback) {
		this.craftingType = craftingType;
		this.callback = callback;
	}

	public interface ProductionAssignmentCallback {

		void productionAssignmentCallback(List<CraftingRecipe> potentialCraftingRecipes);

	}
}
