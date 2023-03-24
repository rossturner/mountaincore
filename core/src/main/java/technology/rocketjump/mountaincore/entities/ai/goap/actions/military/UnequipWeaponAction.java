package technology.rocketjump.mountaincore.entities.ai.goap.actions.military;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

public class UnequipWeaponAction extends Action {

	public UnequipWeaponAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		InventoryComponent inventoryComponent = parent.parentEntity.getOrCreateComponent(InventoryComponent.class);
		EquippedItemComponent equippedItemComponent = parent.parentEntity.getOrCreateComponent(EquippedItemComponent.class);
		Entity currentlyEquipped = equippedItemComponent.clearMainHandItem();
		if (currentlyEquipped != null) {
			inventoryComponent.add(currentlyEquipped, parent.parentEntity, parent.messageDispatcher, gameContext.getGameClock());
		}

		Entity equippedOffHand = equippedItemComponent.clearOffHandItem();
		if (equippedOffHand != null) {
			inventoryComponent.add(equippedOffHand, parent.parentEntity, parent.messageDispatcher, gameContext.getGameClock());
		}
		completionType = CompletionType.SUCCESS;
	}

	@Override
	public boolean isInterruptible() {
		return false;
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