package technology.rocketjump.mountaincore.entities.ai.goap.actions.invasion;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.ItemTypeLookupCallback;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.LookupItemTypeMessage;
import technology.rocketjump.mountaincore.messaging.types.RequestHaulingAllocationMessage;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class SelectItemToStealAction extends Action implements ItemTypeLookupCallback, RequestHaulingAllocationMessage.ItemAllocationCallback {

	// Note that this is in order of preference
	private static final List<String> stockpileGroupNames = List.of("WEAPONS", "FOOD", "GRANARY");

	private List<ItemType> itemTypes = new ArrayList<>();
	private HaulingAllocation foundAllocation;

	public SelectItemToStealAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		for (String stockpileGroupName : stockpileGroupNames) {
			parent.messageDispatcher.dispatchMessage(MessageType.LOOKUP_ITEM_TYPES_BY_STOCKPILE_GROUP, new LookupItemTypeMessage(stockpileGroupName, this));

			for (ItemType itemType : itemTypes) {
				parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_HAULING_ALLOCATION, new RequestHaulingAllocationMessage(
						parent.parentEntity, parent.parentEntity.getLocationComponent().getWorldOrParentPosition(), itemType,
						null, true, itemType.getMaxStackSize(), null, this));

				if (foundAllocation != null) {
					parent.setAssignedHaulingAllocation(foundAllocation);
					if (parent.parentEntity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour &&
							creatureBehaviour.getCreatureGroup() != null) {
						creatureBehaviour.getCreatureGroup().setHomeLocation(foundAllocation.getSourcePosition());
					}
					completionType = CompletionType.SUCCESS;
					break;
				}
			}

			if (completionType != null) {
				break;
			}
		}


		if (completionType == null) {
			completionType = CompletionType.FAILURE;
		}
	}

	@Override
	public void itemTypeFound(Optional<ItemType> itemTypeLookup) {
		itemTypeLookup.ifPresent(itemTypes::add);
	}

	@Override
	public void itemTypesFound(List<ItemType> itemTypes) {
		this.itemTypes.addAll(itemTypes);
		Collections.shuffle(this.itemTypes);
	}

	@Override
	public void allocationFound(HaulingAllocation haulingAllocation) {
		this.foundAllocation = haulingAllocation;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
	}
}
