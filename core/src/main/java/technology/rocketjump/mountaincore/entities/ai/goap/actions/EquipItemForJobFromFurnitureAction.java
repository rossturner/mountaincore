package technology.rocketjump.mountaincore.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.components.furniture.DecorationInventoryComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import java.util.Optional;

import static technology.rocketjump.mountaincore.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.mountaincore.entities.ai.goap.actions.Action.CompletionType.SUCCESS;

public class EquipItemForJobFromFurnitureAction extends Action {

	public EquipItemForJobFromFurnitureAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		// See if item is in inventory to equip
		ItemType requiredItemType = parent.getAssignedJob().getRequiredItemType();
		if (requiredItemType == null) {
			Logger.error("No item type in " +getSimpleName());
			completionType = FAILURE;
			return;
		}
		GameMaterial requiredMaterial = parent.getAssignedJob().getRequiredItemMaterial();

		getTargetFurniture(gameContext, parent)
				.ifPresentOrElse(furniture -> {
					DecorationInventoryComponent inventoryComponent = furniture.getComponent(DecorationInventoryComponent.class);
					if (inventoryComponent == null) {
						parent.setInterrupted(true);
						completionType = FAILURE;
					} else {
						Entity itemInInventory;
						if (requiredMaterial != null) {
							itemInInventory = inventoryComponent.findByItemTypeAndMaterial(requiredItemType, requiredMaterial);
						} else {
							itemInInventory = inventoryComponent.findByItemType(requiredItemType);
						}
						EquippedItemComponent equippedItemComponent = parent.parentEntity.getOrCreateComponent(EquippedItemComponent.class);
						if (itemInInventory != null && equippedItemComponent.isMainHandEnabled()) {
							if (equippedItemComponent.getMainHandItem() != null) {
								Entity oldEquippedItem = equippedItemComponent.clearMainHandItem();
								parent.parentEntity.getComponent(InventoryComponent.class).add(oldEquippedItem, parent.parentEntity, parent.messageDispatcher, gameContext.getGameClock());
							}
							inventoryComponent.remove(itemInInventory.getId());
							equippedItemComponent.setMainHandItem(itemInInventory, parent.parentEntity, parent.messageDispatcher);
							completionType = SUCCESS;
						} else {
							parent.setInterrupted(true);
							completionType = FAILURE;
						}
					}
				}, () -> {
					parent.setInterrupted(true);
					completionType = FAILURE;
				});

	}

	public static Optional<Entity> getTargetFurniture(GameContext gameContext, AssignedGoal assignedGoal) {
		MapTile furnitureLocationTile = gameContext.getAreaMap().getTile(assignedGoal.getAssignedJob().getSecondaryLocation());
		return furnitureLocationTile.getEntities().stream()
				.filter(entity -> entity.getType().equals(EntityType.FURNITURE))
				.findAny();
	}

	@Override
	public boolean isApplicable(GameContext gameContext) {
		return parent.getAssignedJob() != null && (
				(parent.getAssignedJob().getCraftingRecipe() != null && parent.getAssignedJob().getCraftingRecipe().getCraftingType().isUsesWorkstationTool()) ||
				(parent.getAssignedJob().getType().isUsesWorkstationTool())
		);
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
