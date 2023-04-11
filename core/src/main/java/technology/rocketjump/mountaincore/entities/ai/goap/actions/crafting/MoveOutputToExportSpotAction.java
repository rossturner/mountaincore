package technology.rocketjump.mountaincore.entities.ai.goap.actions.crafting;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SpecialGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.InitialisableAction;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.CraftingStationBehaviour;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.ProductionExportFurnitureBehaviour;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.components.ItemAllocationComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;
import technology.rocketjump.mountaincore.rooms.HaulingAllocationBuilder;

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
	public void actionInterrupted(GameContext gameContext) {
		super.actionInterrupted(gameContext);
		if (subGoal != null) {
			subGoal.setInterrupted(true);
			subGoal.destroy(null, parent.messageDispatcher, gameContext);
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
