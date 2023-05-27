package technology.rocketjump.mountaincore.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

public class EquipItemFromFoodAllocationAction extends Action {

	public EquipItemFromFoodAllocationAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (parent.getFoodAllocation() == null || parent.getFoodAllocation().getTargetEntity() == null) {
			completionType = CompletionType.FAILURE;
		} else {
			InventoryComponent inventoryComponent = parent.parentEntity.getOrCreateComponent(InventoryComponent.class);
			Entity itemInInventory = inventoryComponent.remove(parent.getFoodAllocation().getTargetEntity().getId());
			if (itemInInventory == null) {
				completionType = CompletionType.FAILURE;
			} else {
				EquippedItemComponent equippedItemComponent = parent.parentEntity.getOrCreateComponent(EquippedItemComponent.class);
				equippedItemComponent.clearHeldEquipment().forEach(e -> inventoryComponent.add(e, parent.parentEntity, parent.messageDispatcher, gameContext.getGameClock()));
				boolean successful = equippedItemComponent.setEquippedToAnyHand(itemInInventory, parent.parentEntity, parent.messageDispatcher);
				if (successful) {
					completionType = CompletionType.SUCCESS;
				} else {
					inventoryComponent.add(itemInInventory, parent.parentEntity, parent.messageDispatcher, gameContext.getGameClock());
					completionType = CompletionType.FAILURE;
				}

			}
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		// No state to write
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		// No state to write
	}
}
