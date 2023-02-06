package technology.rocketjump.saul.crafting;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.crafting.model.CraftingRecipe;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.saul.jobs.CraftingTypeDictionary;
import technology.rocketjump.saul.jobs.model.CraftingType;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static technology.rocketjump.saul.entities.behaviour.furniture.CraftingStationBehaviour.CRAFTING_BONUS_VALUE;

@Singleton
public class CraftingRecipeDictionary {

	private final Map<CraftingType, List<CraftingRecipe>> byCraftingType = new HashMap<>();
	private final Map<String, CraftingRecipe> byName = new HashMap<>();
	private final CraftingTypeDictionary craftingTypeDictionary;
	private final ItemTypeDictionary itemTypeDictionary;
	private final GameMaterialDictionary materialDictionary;

	@Inject
	public CraftingRecipeDictionary(CraftingTypeDictionary craftingTypeDictionary, ItemTypeDictionary itemTypeDictionary,
									GameMaterialDictionary materialDictionary) throws IOException {
		this.craftingTypeDictionary = craftingTypeDictionary;
		this.itemTypeDictionary = itemTypeDictionary;
		this.materialDictionary = materialDictionary;
		for (CraftingType craftingType : craftingTypeDictionary.getAll()) {
			byCraftingType.put(craftingType, new ArrayList<>());
		}

		FileHandle craftingRecipesJsonFile = Gdx.files.internal("assets/definitions/crafting/craftingRecipes.json");
		ObjectMapper objectMapper = new ObjectMapper();
		List<CraftingRecipe> craftingRecipes = objectMapper.readValue(craftingRecipesJsonFile.readString(),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, CraftingRecipe.class));

		for (CraftingRecipe craftingRecipe : craftingRecipes) {
			initCraftingRecipe(craftingRecipe);
			byCraftingType.get(craftingRecipe.getCraftingType()).add(craftingRecipe);
			byName.put(craftingRecipe.getRecipeName(), craftingRecipe);
		}

		for (ItemType itemType : itemTypeDictionary.getAll()) {
			if (itemType.getBaseValuePerItem() == 0) {
				Logger.warn("0 base value found for item " + itemType.getItemTypeName());

				craftingRecipes.stream().filter(recipe ->
								itemType.equals(recipe.getOutput().getItemType())
						)
						.findAny()
						.ifPresentOrElse(recipe -> {
							int totalItemsOutput = recipe.getOutput().getQuantity();
							int totalValueInput = recipe.getInput().stream().map(i -> i.getItemType().getBaseValuePerItem() * i.getQuantity()).reduce(0, Integer::sum);

							StringBuilder stringBuilder = new StringBuilder();
							int suggestedValue = Math.max(1, Math.round((float) totalValueInput / (float) totalItemsOutput * CRAFTING_BONUS_VALUE));
							stringBuilder.append("Suggesting baseValuePerItem of ").append(suggestedValue);
							stringBuilder.append(" - ").append(totalItemsOutput).append("x created from ");
							for (QuantifiedItemTypeWithMaterial input : recipe.getInput()) {
								if (input.getItemType() != null) {
									stringBuilder.append("( ").append(input.getQuantity()).append(" * ").append(input.getItemType().getItemTypeName()).append(" at ").append(input.getItemType().getBaseValuePerItem()).append(") ");
								} else if (input.isLiquid()) {
									stringBuilder.append("( Liquid ").append(input.getMaterial().getMaterialName()).append(" * ").append(input.getQuantity()).append(")");
								}
							}
							stringBuilder.append("\n");
							Logger.warn(stringBuilder.toString());
						}, () -> Logger.warn("No recipe to craft this item is present\n"));
			}
		}

	}

	private void initCraftingRecipe(CraftingRecipe craftingRecipe) {
		CraftingType relatedCraftingType = craftingTypeDictionary.getByName(craftingRecipe.getCraftingTypeName());
		craftingRecipe.setCraftingType(relatedCraftingType);
		if (craftingRecipe.getItemTypeRequiredName() != null) {
			ItemType itemType = itemTypeDictionary.getByName(craftingRecipe.getItemTypeRequiredName());
			if (itemType != null) {
				craftingRecipe.setItemTypeRequired(itemType);
			} else {
				Logger.error("Could not find item type with name {} for recipe {}", craftingRecipe.getItemTypeRequiredName(), craftingRecipe.getRecipeName());
			}
		}
		for (QuantifiedItemTypeWithMaterial quantifiedItemType : craftingRecipe.getInput()) {
			initialise(craftingRecipe, quantifiedItemType);
		}
		initialise(craftingRecipe, craftingRecipe.getOutput());

		if (craftingRecipe.getInput().stream().filter(QuantifiedItemTypeWithMaterial::isLiquid).count() > 1) {
			throw new RuntimeException("Crafting recipe can not have more than 1 input liquid, found in " + craftingRecipe.getRecipeName());
		}
	}

	private void initialise(CraftingRecipe craftingRecipe, QuantifiedItemTypeWithMaterial quantifiedItemType) {
		if (quantifiedItemType.getItemTypeName() != null) {
			ItemType itemType = itemTypeDictionary.getByName(quantifiedItemType.getItemTypeName());
			if (itemType != null) {
				quantifiedItemType.setItemType(itemType);
			} else {
				Logger.error("Could not find item type with name {} for recipe {}", quantifiedItemType.getItemTypeName(), craftingRecipe.getRecipeName());
			}
		}

		if (quantifiedItemType.getMaterialName() != null) {
			GameMaterial material = materialDictionary.getByName(quantifiedItemType.getMaterialName());
			if (material == null) {
				Logger.error("Could not find material with name " + quantifiedItemType.getMaterialName() + " required for recipe " + craftingRecipe.getRecipeName());
			} else {
				quantifiedItemType.setMaterial(material);
			}
		}
	}

	public List<CraftingRecipe> getByCraftingType(CraftingType craftingType) {
		return byCraftingType.get(craftingType);
	}

	public List<CraftingRecipe> getByOutputItemTypeAndMaterial(ItemType itemType, GameMaterial material) {
		return byName.values().stream()
				.filter(recipe -> {
					QuantifiedItemTypeWithMaterial output = recipe.getOutput();
					return itemType.equals(output.getItemType()) &&
							(material == null || output.getMaterial() == null || material.equals(output.getMaterial()));
				})
				.toList();
	}

	public CraftingRecipe getByName(String recipeName) {
		return byName.get(recipeName);
	}
}
