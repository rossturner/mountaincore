package technology.rocketjump.saul.entities.ai.goap.actions.nourishment;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.actions.Action;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.components.ItemAllocationComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.saul.entities.model.physical.item.ItemHoldPosition;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.Optional;

import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.SUCCESS;
import static technology.rocketjump.saul.entities.components.ItemAllocation.Purpose.FOOD_ALLOCATION;
import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.saul.misc.VectorUtils.toVector;

/**
 * This Action hopes that the current location is a workspace of a table to place the currently equipped item onto
 */
public class PlaceFoodOrDrinkOnFurnitureAction extends Action {

	public PlaceFoodOrDrinkOnFurnitureAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		Vector2 parentPosition = parent.parentEntity.getLocationComponent().getWorldOrParentPosition();
		MapTile parentTile = gameContext.getAreaMap().getTile(parentPosition);

		EquippedItemComponent equippedItemComponent = parent.parentEntity.getComponent(EquippedItemComponent.class);


		if (parentTile == null || equippedItemComponent == null || equippedItemComponent.getMainHandItem() == null ||
				parent.getFoodAllocation() == null || equippedItemComponent.getMainHandItem().getId() != parent.getFoodAllocation().getTargetEntity().getId()) {
			completionType = FAILURE;
		} else {

			for (MapTile neighbourTile : gameContext.getAreaMap().getOrthogonalNeighbours(parentTile.getTileX(), parentTile.getTileY()).values()) {
				Optional<Entity> neighbourfurniture = neighbourTile.getEntities().stream().filter(entity -> entity.getType().equals(EntityType.FURNITURE)).findAny();
				if (neighbourfurniture.isPresent()) {
					Entity furnitureEntity = neighbourfurniture.get();
					FurnitureEntityAttributes furnitureAttributes = (FurnitureEntityAttributes) furnitureEntity.getPhysicalEntityComponent().getAttributes();
					int workspaceIndex = 0;
					for (FurnitureLayout.Workspace workspace : furnitureAttributes.getCurrentLayout().getWorkspaces()) {
						if (workspace.getAccessedFrom().cpy().add(toGridPoint(furnitureEntity.getLocationComponent().getWorldPosition()))
								.equals(parentTile.getTilePosition())) {
							// This is a valid workspace
							ItemHoldPosition preferredPosition = null;
							if (workspaceIndex < ItemHoldPosition.WORKSPACES.size()) {
								preferredPosition = ItemHoldPosition.WORKSPACES.get(workspaceIndex);
							}

							InventoryComponent furnitureInventory = furnitureEntity.getOrCreateComponent(InventoryComponent.class);
							// Face towards location
							parent.parentEntity.getLocationComponent().setFacing(
									toVector(workspace.getLocation().cpy().add(toGridPoint(furnitureEntity.getLocationComponent().getWorldPosition()))).sub(
											parent.parentEntity.getLocationComponent().getWorldPosition()
									)
							);

							Entity item = equippedItemComponent.clearMainHandItem(); // FIXME Maybe this should always remove the component from its parent
							furnitureInventory.add(item, furnitureEntity, parent.messageDispatcher,
									gameContext.getGameClock(), preferredPosition);
							// Need to create allocation or unassigned food will be assigned to someone else
							ItemAllocationComponent itemAllocationComponent = item.getOrCreateComponent(ItemAllocationComponent.class);
							if (itemAllocationComponent.getNumUnallocated() > 0) {
								itemAllocationComponent.createAllocation(
										itemAllocationComponent.getNumUnallocated(),
										furnitureEntity, FOOD_ALLOCATION);
							}
							parent.parentEntity.removeComponent(EquippedItemComponent.class);
							completionType = SUCCESS;
							return;
						}

						workspaceIndex++;
					}
				} // else try next neighbour
			}
			completionType = FAILURE; // Could not find a neighbouring workspace to place item onto
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		// No state
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		// No state
	}
}
