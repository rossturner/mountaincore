package technology.rocketjump.saul.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.components.WeaponSelectionComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.saul.entities.model.physical.creature.HaulingComponent;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.Optional;

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
		WeaponSelectionComponent weaponSelectionComponent = parent.parentEntity.getOrCreateComponent(WeaponSelectionComponent.class);

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

		Optional<ItemType> selectedWeapon = weaponSelectionComponent.getSelectedWeapon();

		if (selectedWeapon.isEmpty()) {
			completionType = SUCCESS;
		} else {
			InventoryComponent.InventoryEntry inventoryEntry = inventoryComponent.findByItemType(selectedWeapon.get(), gameContext.getGameClock());
			if (inventoryEntry == null) {
				completionType = FAILURE;
			} else {
				inventoryComponent.remove(inventoryEntry.entity.getId());
				equippedItemComponent.setMainHandItem(inventoryEntry.entity, parent.parentEntity, parent.messageDispatcher);
				completionType = SUCCESS;
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
