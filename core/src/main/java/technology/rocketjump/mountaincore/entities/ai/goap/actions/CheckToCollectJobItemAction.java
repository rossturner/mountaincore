package technology.rocketjump.mountaincore.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.environment.GameClock;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.jobs.model.Job;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

public class CheckToCollectJobItemAction extends Action {

	public CheckToCollectJobItemAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		Job assignedJob = parent.getAssignedJob();
		if (assignedJob != null) {
			if (assignedJob.getRequiredItemType() != null && !assignedJob.getType().isUsesWorkstationTool()) {
				if (!haveInventoryItem(assignedJob.getRequiredItemType(), assignedJob.getRequiredItemMaterial(), gameContext.getGameClock())) {
					completionType = CompletionType.SUCCESS;
					return;
				}
			}
		}
		completionType = CompletionType.FAILURE;
	}

	private boolean haveInventoryItem(ItemType itemTypeRequired, GameMaterial requiredItemMaterial, GameClock gameClock) {
		InventoryComponent inventoryComponent = parent.parentEntity.getComponent(InventoryComponent.class);
		if (requiredItemMaterial != null) {
			return inventoryComponent.findByItemTypeAndMaterial(itemTypeRequired, requiredItemMaterial, gameClock) != null;
		} else {
			return inventoryComponent.findByItemType(itemTypeRequired, gameClock) != null;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
	}
}
