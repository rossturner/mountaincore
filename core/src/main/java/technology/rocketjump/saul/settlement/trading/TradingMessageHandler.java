package technology.rocketjump.saul.settlement.trading;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.NotImplementedException;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.environment.model.Season;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.settlement.SettlerTracker;
import technology.rocketjump.saul.settlement.notifications.Notification;
import technology.rocketjump.saul.settlement.notifications.NotificationType;
import technology.rocketjump.saul.settlement.trading.model.TraderInfo;

import static technology.rocketjump.saul.invasions.InvasionMessageHandler.selectInvasionWorldPosition;
import static technology.rocketjump.saul.messaging.MessageType.*;

@Singleton
public class TradingMessageHandler implements Telegraph, GameContextAware {

	private static final int MIN_SETTLERS_TO_TRIGGER_POPULATION_INVASION = 20;
	private final MessageDispatcher messageDispatcher;
	private final SettlerTracker settlerTracker;
	private final TradeCaravanGenerator tradeCaravanGenerator;
	private GameContext gameContext;

	@Inject
	public TradingMessageHandler(MessageDispatcher messageDispatcher, SettlerTracker settlerTracker, TradeCaravanGenerator tradeCaravanGenerator) {
		this.messageDispatcher = messageDispatcher;
		this.settlerTracker = settlerTracker;
		this.tradeCaravanGenerator = tradeCaravanGenerator;

		messageDispatcher.addListener(this, HOUR_ELAPSED);
		messageDispatcher.addListener(this, MessageType.DAY_ELAPSED);
		messageDispatcher.addListener(this, MessageType.TRIGGER_TRADE_CARAVAN);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case HOUR_ELAPSED -> {
				onHourElapsed();
				return false;
			}
			case DAY_ELAPSED -> {
				onDayElapsed();
				return false;
			}
			case TRIGGER_TRADE_CARAVAN -> {
				triggerTradeCaravan();
				return true;
			}
			default -> throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.getClass().getSimpleName() + ", " + msg);
		}
	}

	private void onHourElapsed() {
		TraderInfo traderInfo = gameContext.getSettlementState().getTraderInfo();
		Double hoursUntilTraderArrives = traderInfo.getHoursUntilTraderArrives();
		if (hoursUntilTraderArrives != null) {
			hoursUntilTraderArrives -= 1.0;
			if (hoursUntilTraderArrives <= 0) {
				messageDispatcher.dispatchMessage(MessageType.TRIGGER_TRADE_CARAVAN);
				traderInfo.setHoursUntilTraderArrives(null);
				setupNextTraderArrival();
			} else {
				traderInfo.setHoursUntilTraderArrives(hoursUntilTraderArrives);
			}
		}
	}

	private void onDayElapsed() {
		TraderInfo traderInfo = gameContext.getSettlementState().getTraderInfo();
		if (traderInfo.getNextVisitDayOfYear() == null) {
			setupNextTraderArrival();
		} else if (traderInfo.getNextVisitDayOfYear() == gameContext.getGameClock().getDayOfYear()) {
			pickNextTraderArrivalTime();
		}
	}

	private void setupNextTraderArrival() {
		Season currentSeason = gameContext.getGameClock().getCurrentSeason();
	}

	private void pickNextTraderArrivalTime() {
		throw new NotImplementedException("TODO");
	}

	private void triggerTradeCaravan() {
		Vector2 tradeSpawnLocation = selectInvasionWorldPosition(gameContext, settlerTracker);
		if (tradeSpawnLocation == null) {
			// Should only happen when all settlers are dead or map edge is not navigable
			Logger.warn("Could not find a valid position to spawn traders to");
			return;
		}

		tradeCaravanGenerator.generateTradeCaravan(tradeSpawnLocation, gameContext.getSettlementState().getTraderInfo());

		messageDispatcher.dispatchMessage(4.5f, POST_NOTIFICATION, new Notification(NotificationType.TRADER_ARRIVED, tradeSpawnLocation, null));
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}

}
