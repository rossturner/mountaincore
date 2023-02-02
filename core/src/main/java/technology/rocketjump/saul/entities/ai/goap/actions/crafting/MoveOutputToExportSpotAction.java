package technology.rocketjump.saul.entities.ai.goap.actions.crafting;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.SpecialGoal;
import technology.rocketjump.saul.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.saul.entities.ai.goap.actions.Action;
import technology.rocketjump.saul.entities.ai.goap.actions.InitialisableAction;
import technology.rocketjump.saul.entities.behaviour.furniture.CraftingStationBehaviour;
import technology.rocketjump.saul.entities.behaviour.furniture.ProductionExportFurnitureBehaviour;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.components.ItemAllocationComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.rooms.HaulingAllocation;
import technology.rocketjump.saul.rooms.HaulingAllocationBuilder;

public class MoveOutputToExportSpotAction extends Action implements InitialisableAction {

	private AssignedGoal subGoal;

	public MoveOutputToExportSpotAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void init() {
		if (subGoal != null) {
			subGoal.setParentGoal(this.parent);
			subGoal.init(parent.parentEntity, parent.messageDispatcher);
		}
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		if (subGoal == null) {
			initialiseHaulingOfOutput(gameContext);
		} else {
			if (subGoal.isComplete()) {
				completionType = CompletionType.SUCCESS;
			} else {
				subGoal.update(deltaTime, gameContext);
			}
		}
	}

	private void initialiseHaulingOfOutput(GameContext gameContext) {
		Entity craftingStation = gameContext.getEntities().get(parent.getAssignedJob().getTargetId());
		if (craftingStation != null && craftingStation.getBehaviourComponent() instanceof CraftingStationBehaviour craftingStationBehaviour
				&& craftingStationBehaviour.getCurrentCraftingAssignment() != null) {
			ProductionExportFurnitureBehaviour targetExportBehaviour = gameContext.getAreaMap().getTile(craftingStationBehaviour.getCurrentCraftingAssignment().getOutputLocation())
					.getEntities().stream().filter(entity -> entity.getBehaviourComponent() instanceof ProductionExportFurnitureBehaviour)
					.map(entity -> (ProductionExportFurnitureBehaviour)entity.getBehaviourComponent())
					.findFirst().orElse(null);

			if (targetExportBehaviour != null && targetExportBehaviour.getSelectedItemType() != null) {
				InventoryComponent inventoryComponent = craftingStation.getComponent(InventoryComponent.class);
				InventoryComponent.InventoryEntry itemEntry = inventoryComponent.findByItemTypeAndMaterial(targetExportBehaviour.getSelectedItemType(), targetExportBehaviour.getSelectedMaterial(), gameContext.getGameClock());

				if (itemEntry != null) {
					ItemAllocationComponent allocationComponent = itemEntry.entity.getComponent(ItemAllocationComponent.class);
					ItemEntityAttributes itemEntityAttributes = (ItemEntityAttributes) itemEntry.entity.getPhysicalEntityComponent().getAttributes();
					int haulingQuantity = Math.min(allocationComponent.getNumUnallocated(), itemEntityAttributes.getItemType().getMaxHauledAtOnce());

					if (haulingQuantity > 0) {
						HaulingAllocation haulingAllocation = HaulingAllocationBuilder.createWithItemAllocation(haulingQuantity, itemEntry.entity, parent.parentEntity)
								.toEntity(targetExportBehaviour.getParentEntity());

						subGoal = new AssignedGoal(SpecialGoal.HAUL_ITEM.getInstance(), parent.parentEntity, parent.messageDispatcher);
						subGoal.setAssignedHaulingAllocation(haulingAllocation);
						subGoal.setParentGoal(this.parent);
					} else {
						completionType = CompletionType.FAILURE;
					}
					return;
				}
			}
		}
		completionType = CompletionType.FAILURE;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (subGoal != null) {
			JSONObject subGoalJson = new JSONObject(true);
			subGoal.writeTo(subGoalJson, savedGameStateHolder);
			asJson.put("subGoal", subGoalJson);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		JSONObject subGoalJson = asJson.getJSONObject("subGoal");
		if (subGoalJson != null) {
			subGoal = new AssignedGoal();
			subGoal.readFrom(subGoalJson, savedGameStateHolder, relatedStores);
		}
	}
}
