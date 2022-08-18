package technology.rocketjump.saul.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.components.creature.MilitaryComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.saul.entities.model.physical.creature.HaulingComponent;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.SUCCESS;

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
		Long assignedWeaponId = militaryComponent.getAssignedWeaponId();

		if (assignedWeaponId == null) {
			completionType = SUCCESS;
		} else {
			Entity weaponInInventory = inventoryComponent.getById(assignedWeaponId);
			if (weaponInInventory == null) {
				completionType = FAILURE;
			} else {
				inventoryComponent.remove(weaponInInventory.getId());
				equippedItemComponent.setMainHandItem(weaponInInventory, parent.parentEntity, parent.messageDispatcher);
				completionType = SUCCESS;
			}
		}

		Long assignedShieldId = militaryComponent.getAssignedShieldId();
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
