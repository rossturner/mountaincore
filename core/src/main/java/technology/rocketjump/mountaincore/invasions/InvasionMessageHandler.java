package technology.rocketjump.mountaincore.invasions;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang3.NotImplementedException;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.GameContextAware;
import technology.rocketjump.mountaincore.invasions.model.InvasionDefinition;
import technology.rocketjump.mountaincore.invasions.model.InvasionTrigger;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.misc.VectorUtils;
import technology.rocketjump.mountaincore.settlement.SettlerTracker;
import technology.rocketjump.mountaincore.settlement.notifications.Notification;
import technology.rocketjump.mountaincore.settlement.notifications.NotificationType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Singleton
public class InvasionMessageHandler implements Telegraph, GameContextAware {

	private static final int MIN_SETTLERS_TO_TRIGGER_POPULATION_INVASION = 20;
	private final MessageDispatcher messageDispatcher;
	private final SettlerTracker settlerTracker;
	private final InvasionGenerator invasionGenerator;
	private GameContext gameContext;

	@Inject
	public InvasionMessageHandler(MessageDispatcher messageDispatcher, SettlerTracker settlerTracker, InvasionGenerator invasionGenerator) {
		this.messageDispatcher = messageDispatcher;
		this.settlerTracker = settlerTracker;
		this.invasionGenerator = invasionGenerator;

		messageDispatcher.addListener(this, MessageType.HOUR_ELAPSED);
		messageDispatcher.addListener(this, MessageType.DAY_ELAPSED);
		messageDispatcher.addListener(this, MessageType.TRIGGER_INVASION);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.HOUR_ELAPSED -> {
				onHourElapsed();
				return true;
			}
			case MessageType.TRIGGER_INVASION -> {
				triggerInvasion((InvasionDefinition) msg.extraInfo);
				return true;
			}
			case MessageType.DAY_ELAPSED -> {
				onDayElapsed();
				return true;
			}
			default -> throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.getClass().getSimpleName() + ", " + msg);
		}
	}

	private void onHourElapsed() {
		Double hoursUntilInvasion = gameContext.getSettlementState().getHoursUntilInvasion();
		if (hoursUntilInvasion != null) {
			hoursUntilInvasion -= 1.0;
			if (hoursUntilInvasion < 0) {
				messageDispatcher.dispatchMessage(MessageType.TRIGGER_INVASION, gameContext.getSettlementState().getIncomingInvasion());
				gameContext.getSettlementState().setHoursUntilInvasion(null);
				gameContext.getSettlementState().setIncomingInvasion(null);
			} else {
				gameContext.getSettlementState().setHoursUntilInvasion(hoursUntilInvasion);
			}
		}
	}

	private void triggerInvasion(InvasionDefinition invasionDefinition) {
		Vector2 invasionLocation = selectInvasionWorldPosition(gameContext, settlerTracker);
		if (invasionLocation == null) {
			// Should only happen when all settlers are dead or map edge is not navigable
			Logger.warn("Could not find a valid position to launch invasion from");
			return;
		}

		invasionGenerator.generateInvasionParticipants(invasionDefinition, invasionLocation, calculatePointsBudget(invasionDefinition.getTriggeredBy()));

		messageDispatcher.dispatchMessage(4.5f, MessageType.POST_NOTIFICATION, new Notification(NotificationType.INVASION, invasionLocation, null));
	}

	private void onDayElapsed() {
		Map<InvasionDefinition, Integer> invasionChecks = gameContext.getSettlementState().daysUntilNextInvasionCheck;
		Iterator<Map.Entry<InvasionDefinition, Integer>> iterator = invasionChecks.entrySet().iterator();

		while (iterator.hasNext()) {
			Map.Entry<InvasionDefinition, Integer> entry = iterator.next();
			InvasionDefinition currentDefinition = entry.getKey();
			entry.setValue(entry.getValue() - 1);

			if (entry.getValue() <= 0) {
				if (shouldTrigger(currentDefinition)) {
					// Extend time before checking other invasions
					for (InvasionDefinition otherDefinition : invasionChecks.keySet()) {
						if (!otherDefinition.equals(currentDefinition)) {
							int otherInvasionCheckDays = invasionChecks.get(otherDefinition);
							otherInvasionCheckDays += 5;
							invasionChecks.put(otherDefinition, otherInvasionCheckDays);
						}
					}
					entry.setValue(currentDefinition.getMinDaysBetweenInvasions() + gameContext.getRandom().nextInt(currentDefinition.getInvasionHappensWithinDays()));

					// Actually set invasion to happen today
					gameContext.getSettlementState().setIncomingInvasion(currentDefinition);
					gameContext.getSettlementState().setHoursUntilInvasion(
							(gameContext.getGameClock().HOURS_IN_DAY / 3.0) +
									gameContext.getRandom().nextDouble(gameContext.getGameClock().HOURS_IN_DAY / 3.0)
					);
				} else {
					// Check again in a short while
					entry.setValue(gameContext.getRandom().nextInt(currentDefinition.getInvasionHappensWithinDays()));
				}
			}
		}
	}

	private boolean shouldTrigger(InvasionDefinition invasionDefinition) {
		if (invasionDefinition.getTriggeredBy().equals(InvasionTrigger.POPULATION)) {
			return settlerTracker.getLiving().size() >= MIN_SETTLERS_TO_TRIGGER_POPULATION_INVASION;
		} else {
			throw new NotImplementedException(invasionDefinition.getTriggeredBy().name());
		}
	}

	private int calculatePointsBudget(InvasionTrigger invasionTrigger) {
		if (invasionTrigger.equals(InvasionTrigger.POPULATION)) {
			return settlerTracker.getLiving().size() * 10;
		} else {
			throw new NotImplementedException(invasionTrigger.name());
		}
	}

	public static Vector2 selectInvasionWorldPosition(GameContext gameContext, SettlerTracker settlerTracker) {
		if (settlerTracker.getLiving().isEmpty()) {
			return null;
		}

		MapTile randomSettlerTile = null;
		while (randomSettlerTile == null) {
			Entity randomSettler = new ArrayList<>(settlerTracker.getLiving()).get(gameContext.getRandom().nextInt(settlerTracker.getLiving().size()));
			randomSettlerTile = gameContext.getAreaMap().getTile(randomSettler.getLocationComponent().getWorldOrParentPosition());
		}

		int targetRegionId = randomSettlerTile.getRegionId();
		List<MapTile> eligibleBorderTiles = getNavigableMapEdgeTiles(targetRegionId, gameContext);

		if (eligibleBorderTiles.isEmpty()) {
			return null;
		} else {
			MapTile invasionTile = eligibleBorderTiles.get(gameContext.getRandom().nextInt(eligibleBorderTiles.size()));
			// Figure out which edge is map edge
			if (gameContext.getAreaMap().getTile(invasionTile.getTileX(), invasionTile.getTileY() + 1) == null) {
				return VectorUtils.toVector(invasionTile.getTilePosition()).add(0, 0.48f);
			} else if (gameContext.getAreaMap().getTile(invasionTile.getTileX(), invasionTile.getTileY() - 1) == null) {
				return VectorUtils.toVector(invasionTile.getTilePosition()).add(0, -0.48f);
			} else if (gameContext.getAreaMap().getTile(invasionTile.getTileX() - 1, invasionTile.getTileY()) == null) {
				return VectorUtils.toVector(invasionTile.getTilePosition()).add(-0.48f, 0f);
			} else {
				return VectorUtils.toVector(invasionTile.getTilePosition()).add(0.48f, 0f);
			}
		}
	}

	public static List<MapTile> getNavigableMapEdgeTiles(int targetRegionId, GameContext gameContext) {
		List<MapTile> eligibleBorderTiles = new ArrayList<>();
		for (int x = 0; x < gameContext.getAreaMap().getWidth(); x++) {
			MapTile bottomEdgeTile = gameContext.getAreaMap().getTile(x, 0);
			if (bottomEdgeTile.getRegionId() == targetRegionId && bottomEdgeTile.isNavigable(null)) {
				eligibleBorderTiles.add(bottomEdgeTile);
			}
			MapTile topEdgeTile = gameContext.getAreaMap().getTile(x, gameContext.getAreaMap().getHeight() - 1);
			if (topEdgeTile.getRegionId() == targetRegionId && topEdgeTile.isNavigable(null)) {
				eligibleBorderTiles.add(topEdgeTile);
			}
		}
		for (int y = 1; y < gameContext.getAreaMap().getHeight() - 1; y++) {
			MapTile leftEdgeTile = gameContext.getAreaMap().getTile(0, y);
			if (leftEdgeTile.getRegionId() == targetRegionId && leftEdgeTile.isNavigable(null)) {
				eligibleBorderTiles.add(leftEdgeTile);
			}
			MapTile rightEdgeTile = gameContext.getAreaMap().getTile(gameContext.getAreaMap().getWidth() - 1, y);
			if (rightEdgeTile.getRegionId() == targetRegionId && rightEdgeTile.isNavigable(null)) {
				eligibleBorderTiles.add(rightEdgeTile);
			}
		}
		return eligibleBorderTiles;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}

}
