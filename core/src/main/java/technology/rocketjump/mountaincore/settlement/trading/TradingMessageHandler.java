package technology.rocketjump.mountaincore.settlement.trading;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.environment.GameClock;
import technology.rocketjump.mountaincore.environment.model.Season;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.GameContextAware;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.settlement.SettlerTracker;
import technology.rocketjump.mountaincore.settlement.trading.model.TraderInfo;

import static technology.rocketjump.mountaincore.invasions.InvasionMessageHandler.selectInvasionWorldPosition;
import static technology.rocketjump.mountaincore.messaging.MessageType.*;

@Singleton
public class TradingMessageHandler implements Telegraph, GameContextAware {

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
			default ->
					throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.getClass().getSimpleName() + ", " + msg);
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
			traderInfo.setNextVisitDayOfYear(null);
		}
	}

	private void setupNextTraderArrival() {
		GameClock clock = gameContext.getGameClock();
		Season currentSeason = clock.getCurrentSeason();
		Season nextSeason = currentSeason.getNext();
		if (nextSeason.equals(Season.WINTER)) {
			nextSeason = Season.SPRING;
		}

		int targetDayOfYear = 1;
		for (Season seasonCounter = Season.SPRING; seasonCounter != nextSeason; seasonCounter = seasonCounter.getNext()) {
			targetDayOfYear += clock.DAYS_IN_SEASON;
		}
		targetDayOfYear += gameContext.getRandom().nextInt(1, Math.max((clock.DAYS_IN_SEASON / 2), 2) + 1);
		gameContext.getSettlementState().getTraderInfo().setNextVisitDayOfYear(targetDayOfYear);
	}

	private void pickNextTraderArrivalTime() {
		gameContext.getSettlementState().getTraderInfo().setHoursUntilTraderArrives(
				gameContext.getRandom().nextDouble(
						0.25 * gameContext.getGameClock().HOURS_IN_DAY,
						0.4 * gameContext.getGameClock().HOURS_IN_DAY
				)
		);
	}

	private void triggerTradeCaravan() {
		Vector2 tradeSpawnLocation = selectInvasionWorldPosition(gameContext, settlerTracker);
		if (tradeSpawnLocation == null) {
			// Should only happen when all settlers are dead or map edge is not navigable
			Logger.warn("Could not find a valid position to spawn traders to");
			return;
		}

		tradeCaravanGenerator.generateTradeCaravan(tradeSpawnLocation, gameContext.getSettlementState().getTraderInfo());
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}

}
