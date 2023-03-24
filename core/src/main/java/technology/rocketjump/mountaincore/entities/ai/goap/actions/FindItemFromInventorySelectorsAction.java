package technology.rocketjump.mountaincore.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.GoalSelector;
import technology.rocketjump.mountaincore.entities.ai.goap.ScheduleCategory;
import technology.rocketjump.mountaincore.entities.ai.goap.condition.GoalSelectionByInventory;
import technology.rocketjump.mountaincore.entities.ai.goap.condition.GoalSelectionCondition;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.environment.GameClock;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.LookupItemTypeMessage;
import technology.rocketjump.mountaincore.messaging.types.RequestHaulingAllocationMessage;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

import static technology.rocketjump.mountaincore.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.mountaincore.entities.ai.goap.actions.Action.CompletionType.SUCCESS;

public class FindItemFromInventorySelectorsAction extends Action implements ItemTypeLookupCallback {

	private ItemType foundItemType;
	private GameContext gameContext;

	public FindItemFromInventorySelectorsAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		this.gameContext = gameContext;
		GameClock gameClock = gameContext.getGameClock();

		Collection<ScheduleCategory> applicableScheduleCategories = EnumSet.allOf(ScheduleCategory.class);
		if (parent.parentEntity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour) {
			applicableScheduleCategories = creatureBehaviour.getCurrentSchedule().getCurrentApplicableCategories(gameClock);
		}


		Integer amountRequired = 0;
		String itemTypeName = null;

		for (GoalSelector selector : parent.goal.getSelectors()) {
			if (applicableScheduleCategories.contains(selector.scheduleCategory)) {
				for (GoalSelectionCondition condition : selector.conditions) {
					if (amountRequired == 0 && condition instanceof GoalSelectionByInventory inventoryCondition) {
						Integer targetQuantity = inventoryCondition.targetQuantity;
						if (inventoryCondition.apply(parent.parentEntity, gameContext)) {
							amountRequired = targetQuantity - inventoryCondition.getCurrentQuantity(gameClock, parent.parentEntity.getComponent(InventoryComponent.class));
							itemTypeName = inventoryCondition.itemTypeName;
						}
					}
				}
			}
		}


		if (itemTypeName != null) {
			parent.messageDispatcher.dispatchMessage(MessageType.LOOKUP_ITEM_TYPE, new LookupItemTypeMessage(itemTypeName, this));
		} else {
			//something gone wrong here
		}

		if (foundItemType != null) {
			parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_HAULING_ALLOCATION, new RequestHaulingAllocationMessage(
					parent.parentEntity,
					parent.parentEntity.getLocationComponent().getWorldOrParentPosition(), foundItemType,
					null,
					true, amountRequired, null, (allocation) -> {
				if (allocation != null) {
					parent.setAssignedHaulingAllocation(allocation);
					completionType = SUCCESS;
				}
			}));

		}

		if (completionType == null) {
			completionType = FAILURE;
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

	@Override
	public void itemTypeFound(Optional<ItemType> itemTypeLookup) {
		itemTypeLookup.ifPresent(itemType -> {
			foundItemType = itemType;
		});
	}

	@Override
	public void itemTypesFound(List<ItemType> itemTypes) {
		if (itemTypes != null && !itemTypes.isEmpty()) {
			if (itemTypes.size() == 1) {
				foundItemType = itemTypes.get(0);
			} else if (gameContext != null) {
				foundItemType = itemTypes.get(gameContext.getRandom().nextInt(itemTypes.size()));
			}
		}
	}


}
