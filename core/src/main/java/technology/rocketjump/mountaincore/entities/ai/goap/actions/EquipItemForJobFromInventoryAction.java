package technology.rocketjump.mountaincore.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.mountaincore.entities.model.physical.creature.HaulingComponent;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.mountaincore.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.mountaincore.entities.ai.goap.actions.Action.CompletionType.SUCCESS;

public class EquipItemForJobFromInventoryAction extends Action {

	public EquipItemForJobFromInventoryAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		// See if item is in inventory to equip
		ItemType requiredItemType = parent.getAssignedJob().getRequiredItemType();
		GameMaterial requiredMaterial = parent.getAssignedJob().getRequiredItemMaterial();

		HaulingComponent haulingComponent = parent.parentEntity.getComponent(HaulingComponent.class);
		EquippedItemComponent equippedItemComponent = parent.parentEntity.getOrCreateComponent(EquippedItemComponent.class);
		InventoryComponent inventoryComponent = parent.parentEntity.getOrCreateComponent(InventoryComponent.class);

		if (haulingComponent != null && haulingComponent.getHauledEntity() != null) {
			if (matches(haulingComponent.getHauledEntity(), requiredItemType, requiredMaterial)) {
				if (equippedItemComponent.getMainHandItem() != null) {
					inventoryComponent.add(equippedItemComponent.clearMainHandItem(), parent.parentEntity, parent.messageDispatcher, gameContext.getGameClock());
				}

				equippedItemComponent.setMainHandItem(haulingComponent.clearHauledEntity(), parent.parentEntity, parent.messageDispatcher);
				completionType = SUCCESS;
			} else {
				Logger.error("Hauling the wrong item to equip");
				completionType = FAILURE;
			}
			return;
		}


		InventoryComponent.InventoryEntry itemInInventory;
		if (requiredMaterial != null) {
			itemInInventory = inventoryComponent.findByItemTypeAndMaterial(requiredItemType, requiredMaterial, gameContext.getGameClock());
		} else {
			itemInInventory = inventoryComponent.findByItemType(requiredItemType, gameContext.getGameClock());
		}
		if (itemInInventory != null && equippedItemComponent.isMainHandEnabled()) {
			inventoryComponent.remove(itemInInventory.entity.getId());
			if (equippedItemComponent.getMainHandItem() != null) {
				inventoryComponent.add(equippedItemComponent.clearMainHandItem(), parent.parentEntity, parent.messageDispatcher, gameContext.getGameClock());
			}
			equippedItemComponent.setMainHandItem(itemInInventory.entity, parent.parentEntity, parent.messageDispatcher);
			completionType = SUCCESS;
		} else {
			// Interrupt entire goal so we don't then GoToLocation
			parent.setInterrupted(true);
			completionType = FAILURE;
		}
	}

	private boolean matches(Entity entity, ItemType requiredItemType, GameMaterial requiredMaterial) {
		if (entity.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes itemEntityAttributes) {
			if (requiredMaterial != null) {
				return itemEntityAttributes.getItemType().equals(requiredItemType) && itemEntityAttributes.getPrimaryMaterial().equals(requiredMaterial);
			} else {
				return itemEntityAttributes.getItemType().equals(requiredItemType);
			}
		} else {
			return false;
		}
	}

	@Override
	public boolean isApplicable(GameContext gameContext) {
		if (parent.getAssignedJob() != null && parent.getAssignedJob().getCraftingRecipe() != null && parent.getAssignedJob().getCraftingRecipe().getCraftingType().isUsesWorkstationTool()) {
			return false;
		} else if (parent.getAssignedJob() != null && parent.getAssignedJob().getType().isUsesWorkstationTool()) {
			return false;
		} else if (parent.getAssignedJob() != null && parent.getAssignedJob().getRequiredItemType() != null) {
			return parent.getAssignedJob().getRequiredItemType().isEquippedWhileWorkingOnJob();
		} else {
			return false;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		// No state to write
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		// No state to read
	}

}
