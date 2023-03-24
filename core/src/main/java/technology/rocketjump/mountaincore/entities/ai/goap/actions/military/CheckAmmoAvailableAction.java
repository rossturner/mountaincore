package technology.rocketjump.mountaincore.entities.ai.goap.actions.military;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.mountaincore.entities.model.physical.item.AmmoType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.mountaincore.entities.model.EntityType.ITEM;

public class CheckAmmoAvailableAction extends Action {
	public CheckAmmoAvailableAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		EquippedItemComponent equippedItemComponent = parent.parentEntity.getComponent(EquippedItemComponent.class);
		if (equippedItemComponent != null && equippedItemComponent.getMainHandItem() != null &&
				equippedItemComponent.getMainHandItem().getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes itemAttributes &&
				itemAttributes.getItemType().getWeaponInfo() != null && itemAttributes.getItemType().getWeaponInfo().getRequiresAmmoType() != null) {
			AmmoType requiredAmmoType = itemAttributes.getItemType().getWeaponInfo().getRequiresAmmoType();
			boolean ammoHeld = parent.parentEntity.getComponent(InventoryComponent.class).getInventoryEntries()
					.stream()
					.anyMatch(e -> e.entity.getType().equals(ITEM) &&
							requiredAmmoType.equals(((ItemEntityAttributes) e.entity.getPhysicalEntityComponent().getAttributes()).getItemType().getIsAmmoType()));
			if (!ammoHeld) {
				completionType = CompletionType.FAILURE;
				return;
			}
		}
		completionType = CompletionType.SUCCESS;
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
