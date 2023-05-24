package technology.rocketjump.mountaincore.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.cooking.model.CookingRecipe;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.CollectItemFurnitureBehaviour;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeWithMaterial;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.jobs.model.Skill;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CollectItemsBehaviourTag extends Tag {

	@Override
	public String getTagName() {
		return "COLLECT_ITEMS_BEHAVIOUR";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return EnumUtils.isValidEnum(CollectItemBehaviourArg.class, args.get(0)) &&
				Integer.valueOf(args.get(1)) > 0;
	}

	@Override
	public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (entity.getBehaviourComponent() == null) {
			// Don't apply to furniture which already doesn't have a BehaviourComponent e.g. when placing from UI
			return;
		}

		if (!entity.getBehaviourComponent().getClass().equals(CollectItemFurnitureBehaviour.class)) {
			// Only switch behaviour if already different
			CollectItemFurnitureBehaviour newBehaviour = new CollectItemFurnitureBehaviour();
			newBehaviour.setHaulingJobType(tagProcessingUtils.jobTypeDictionary.getByName("HAULING"));
			newBehaviour.init(entity, messageDispatcher, gameContext);

			CollectItemBehaviourArg behaviourArg = CollectItemBehaviourArg.valueOf(args.get(0));
			switch (behaviourArg) {
				case COOKING_INGREDIENTS:
					Set<ItemTypeWithMaterial> cookingIngredients = new HashSet<>();
					for (CookingRecipe cookingRecipe : tagProcessingUtils.cookingRecipeDictionary.getAll()) {
						cookingIngredients.addAll(cookingRecipe.getInputItemOptions());
					}
					if (args.size() > 4) {
						//todo: feels bit funky?
						newBehaviour.setIncludeFromFurniture(true); //force fetching from other furniture, may cause infinite loop
						List<String> specificIngredients = args.subList(4, args.size());
						cookingIngredients.removeIf(itemTypeWithMaterial -> !specificIngredients.contains(itemTypeWithMaterial.getItemType().getItemTypeName()));
					}
					newBehaviour.setItemsToCollect(new ArrayList<>(cookingIngredients));
					break;
				case PREPARED_FOOD:
					Set<ItemTypeWithMaterial> itemsToCollect = new HashSet<>();
					for (CookingRecipe cookingRecipe : tagProcessingUtils.cookingRecipeDictionary.getAll()) {
						if (cookingRecipe.getOutputItemType() != null & cookingRecipe.getOutputMaterial() != null) {
							ItemTypeWithMaterial itemToCollect = new ItemTypeWithMaterial();
							itemToCollect.setItemType(cookingRecipe.getOutputItemType());
							itemToCollect.setMaterial(cookingRecipe.getOutputMaterial());
							itemsToCollect.add(itemToCollect);
						}
					}
					newBehaviour.setItemsToCollect(new ArrayList<>(itemsToCollect));
					newBehaviour.setHaulingJobInterruptible(false);
					break;
				default:
					throw new NotImplementedException("Not yet implemented, argument to " + this.getTagName() + " tag: " + behaviourArg.name());
			}
			newBehaviour.setMaxNumItemStacks(Integer.valueOf(args.get(1)));

			if (args.size() > 2) {
				Skill specifiedProfession = tagProcessingUtils.skillDictionary.getByName(args.get(2));
				if (specifiedProfession == null) {
					Logger.error("Unrecognised profession " + args.get(2) + " in " + getTagName() + " tag");
				} else {
					newBehaviour.setRequiredProfession(specifiedProfession);
				}
			}

			if (args.size() > 3) {
				boolean allowDuplicates = args.get(3).equalsIgnoreCase("ALLOW_DUPLICATES");
				newBehaviour.setAllowDuplicates(allowDuplicates);
			}

			entity.replaceBehaviourComponent(newBehaviour);
		}
	}

	public enum CollectItemBehaviourArg {

		COOKING_INGREDIENTS, PREPARED_FOOD

	}
}
