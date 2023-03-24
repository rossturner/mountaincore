package technology.rocketjump.mountaincore.entities.ai.goap.actions.trading;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.GetRoomsByComponentMessage;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rooms.components.behaviour.TradeDepotBehaviour;
import technology.rocketjump.mountaincore.settlement.notifications.Notification;
import technology.rocketjump.mountaincore.settlement.notifications.NotificationType;
import technology.rocketjump.mountaincore.ui.Selectable;

import static technology.rocketjump.mountaincore.messaging.MessageType.POST_NOTIFICATION;

public class MerchantNotificationToPlayerAction extends Action {

	public MerchantNotificationToPlayerAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		parent.messageDispatcher.dispatchMessage(MessageType.GET_ROOMS_BY_COMPONENT,
				new GetRoomsByComponentMessage(TradeDepotBehaviour.class, (rooms -> {
					if (rooms.isEmpty()) {
						parent.messageDispatcher.dispatchMessage(4.5f, POST_NOTIFICATION, new Notification(NotificationType.TRADER_ARRIVED_NO_DEPOT,
								parent.parentEntity.getLocationComponent().getWorldOrParentPosition(),
								new Selectable(parent.parentEntity, 0)));
						completionType = CompletionType.FAILURE;
					} else {
						parent.messageDispatcher.dispatchMessage(4.5f, POST_NOTIFICATION, new Notification(NotificationType.TRADER_ARRIVED,
								parent.parentEntity.getLocationComponent().getWorldOrParentPosition(),
								new Selectable(parent.parentEntity, 0)));
						completionType = CompletionType.SUCCESS;
					}
				})));
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
	}
}
