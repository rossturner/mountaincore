package technology.rocketjump.mountaincore.entities.ai.goap.actions.trading;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.PlannedTrade;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.mountaincore.entities.behaviour.creature.TraderCreatureGroup;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

public class CancelTradeAction extends Action {

	public CancelTradeAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		PlannedTrade plannedTrade = parent.getPlannedTrade();
		if (plannedTrade != null) {
			parent.messageDispatcher.dispatchMessage(MessageType.CANCEL_ITEM_ALLOCATION, plannedTrade.getPaymentItemAllocation());
			if (plannedTrade.getHaulingAllocation() != null) {
				parent.messageDispatcher.dispatchMessage(MessageType.HAULING_ALLOCATION_CANCELLED, plannedTrade.getHaulingAllocation());
			}

			if (parent.parentEntity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour &&
				creatureBehaviour.getCreatureGroup() instanceof TraderCreatureGroup traderCreatureGroup) {
				traderCreatureGroup.getPlannedTrades().remove(plannedTrade);
			}
		}
		if (parent.getAssignedHaulingAllocation() != null) {
			parent.messageDispatcher.dispatchMessage(MessageType.HAULING_ALLOCATION_CANCELLED, parent.getAssignedHaulingAllocation());
		}
		completionType = CompletionType.SUCCESS;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
	}
}
