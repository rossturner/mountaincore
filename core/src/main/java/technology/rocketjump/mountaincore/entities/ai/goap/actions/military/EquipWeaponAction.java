package technology.rocketjump.mountaincore.entities.ai.goap.actions.military;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.components.creature.MilitaryComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.mountaincore.entities.model.physical.creature.HaulingComponent;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

public class EquipWeaponAction extends Action {

	public EquipWeaponAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		InventoryComponent inventoryComponent = parent.parentEntity.getOrCreateComponent(InventoryComponent.class);
		EquippedItemComponent equippedItemComponent = parent.parentEntity.getOrCreateComponent(EquippedItemComponent.class);
		HaulingComponent haulingComponent = parent.parentEntity.getComponent(HaulingComponent.class);

		Entity currentlyEquipped = equippedItemComponent.clearMainHandItem();
		if (currentlyEquipped != null) {
			inventoryComponent.add(currentlyEquipped, parent.parentEntity, parent.messageDispatcher, gameContext.getGameClock());
		}
		if (haulingComponent != null) {
			Entity hauledEntity = haulingComponent.getHauledEntity();
			if (hauledEntity != null) {
				haulingComponent.clearHauledEntity();
				inventoryComponent.add(hauledEntity, parent.parentEntity, parent.messageDispatcher, gameContext.getGameClock());
				parent.parentEntity.removeComponent(HaulingComponent.class);
			}
		}

		MilitaryComponent militaryComponent = parent.parentEntity.getComponent(MilitaryComponent.class);
		Long assignedWeaponId = militaryComponent == null ? null : militaryComponent.getAssignedWeaponId();

		if (assignedWeaponId == null) {
			completionType = CompletionType.SUCCESS;
		} else {
			Entity weaponInInventory = inventoryComponent.getById(assignedWeaponId);
			if (weaponInInventory != null && equippedItemComponent.isMainHandEnabled()) {
				inventoryComponent.remove(weaponInInventory.getId());
				equippedItemComponent.setMainHandItem(weaponInInventory, parent.parentEntity, parent.messageDispatcher);
				completionType = CompletionType.SUCCESS;
			} else {
				completionType = CompletionType.FAILURE;
			}
		}

		Long assignedShieldId = militaryComponent == null ? null : militaryComponent.getAssignedShieldId();
		if (assignedShieldId != null) {
			Entity shieldInInventory = inventoryComponent.getById(assignedShieldId);
			if (shieldInInventory != null) {
				inventoryComponent.remove(shieldInInventory.getId());
				equippedItemComponent.setOffHandItem(shieldInInventory, parent.parentEntity, parent.messageDispatcher);
			}
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