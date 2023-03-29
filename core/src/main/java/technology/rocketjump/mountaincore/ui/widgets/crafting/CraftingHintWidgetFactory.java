package technology.rocketjump.mountaincore.ui.widgets.crafting;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.crafting.CraftingRecipeDictionary;
import technology.rocketjump.mountaincore.crafting.model.CraftingRecipe;
import technology.rocketjump.mountaincore.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.jobs.model.CraftingType;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.ui.eventlistener.TooltipFactory;
import technology.rocketjump.mountaincore.ui.i18n.I18nString;
import technology.rocketjump.mountaincore.ui.i18n.I18nText;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;
import technology.rocketjump.mountaincore.ui.i18n.I18nWordClass;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class CraftingHintWidgetFactory {
	private final CraftingRecipeDictionary craftingRecipeDictionary;
	private final I18nTranslator i18nTranslator;
	private final FurnitureTypeDictionary furnitureTypeDictionary;
	private final TooltipFactory tooltipFactory;

	@Inject
	public CraftingHintWidgetFactory(CraftingRecipeDictionary craftingRecipeDictionary, I18nTranslator i18nTranslator,
									 FurnitureTypeDictionary furnitureTypeDictionary, TooltipFactory tooltipFactory) {
		this.craftingRecipeDictionary = craftingRecipeDictionary;
		this.i18nTranslator = i18nTranslator;
		this.furnitureTypeDictionary = furnitureTypeDictionary;
		this.tooltipFactory = tooltipFactory;
	}

	public void addComplexTooltip(Actor button, Skin skin, ItemType itemType, GameMaterial material) {
		Table tooltipTable = new Table();
		tooltipTable.defaults().padBottom(30);

		String headerText = i18nTranslator.getItemDescription(1, material, itemType, null).toString();
		tooltipTable.add(new Label(headerText, skin.get("complex-tooltip-header", Label.LabelStyle.class))).center().row();

		String itemDescriptionText = i18nTranslator.getTranslatedString(itemType.getI18nKey(), I18nWordClass.TOOLTIP).toString();
		Label descriptionLabel = new Label(itemDescriptionText, skin);
		descriptionLabel.setWrap(true);
		tooltipTable.add(descriptionLabel).width(700).center().row();

		for (String hint : getCraftingRecipeDescriptions(itemType, material)) {
			Label requirementLabel = new Label(hint, skin);
			requirementLabel.setWrap(true);
			tooltipTable.add(requirementLabel).width(700).center().row();
		}

		tooltipFactory.complexTooltip(button, tooltipTable, TooltipFactory.TooltipBackground.LARGE_PATCH_DARK);
	}

	public Set<String> getCraftingRecipeDescriptions(ItemType itemType, GameMaterial material) {
		java.util.List<CraftingRecipe> suggestedRecipes = craftingRecipeDictionary.getByOutputItemTypeAndMaterial(itemType, material);
		I18nText outputItemTypeText = i18nTranslator.getTranslatedString(itemType.getI18nKey(), I18nWordClass.PLURAL);
		Set<String> recipeHints = new LinkedHashSet<>();
		for (CraftingRecipe recipe : suggestedRecipes) {
			CraftingType craftingType = recipe.getCraftingType();
			java.util.List<FurnitureType> furnitureTypes = furnitureTypeDictionary.getByCraftingType(craftingType);

			java.util.List<I18nText> inputItemTypeTexts = recipe.getInput()
					.stream()
					.map(input -> i18nTranslator.getTranslatedString(input.getItemType().getI18nKey(), I18nWordClass.PLURAL))
					.toList();
			I18nText professionText = i18nTranslator.getTranslatedString(craftingType.getProfessionRequired().getI18nKey(), I18nWordClass.PLURAL);

			for (FurnitureType furnitureType : furnitureTypes) {
				I18nText furnitureTypeText = i18nTranslator.getTranslatedString(furnitureType.getI18nKey());
				I18nText lastInputItemText = inputItemTypeTexts.get(inputItemTypeTexts.size() - 1);

				Map<String, I18nString> replacements = new HashMap<>();
				replacements.put("outputItemType", outputItemTypeText);
				replacements.put("furnitureType", furnitureTypeText);
				replacements.put("profession", professionText);
				replacements.put("lastInputItemType", lastInputItemText);

				String requirementRecipeKey = "CONSTRUCTION.REQUIREMENT_RECIPE_SINGULAR";

				if (inputItemTypeTexts.size() > 1) {
					requirementRecipeKey = "CONSTRUCTION.REQUIREMENT_RECIPE_MULTIPLE";
					java.util.List<I18nText> firstInputItemTypeTexts = inputItemTypeTexts.subList(0, inputItemTypeTexts.size() - 1);

					String commaText = firstInputItemTypeTexts.stream()
							.map(I18nText::toString).collect(Collectors.joining(", "));
					replacements.put("inputItemTypes", new I18nText(commaText));
				}

				recipeHints.add(i18nTranslator.getTranslatedWordWithReplacements(requirementRecipeKey, replacements).toString());
			}
		}
		return recipeHints;
	}

}
