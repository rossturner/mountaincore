package technology.rocketjump.saul.ui;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.Consciousness;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.environment.model.GameSpeed;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.settlement.SettlerTracker;

import static technology.rocketjump.saul.gamecontext.GameState.SELECT_SPAWN_LOCATION;
import static technology.rocketjump.saul.gamecontext.GameState.STARTING_SPAWN;

@Singleton
public class GameSpeedMessageHandler implements Telegraph, GameContextAware {

	private final SettlerTracker settlerTracker;
	private final MessageDispatcher messageDispatcher;

	private GameContext gameContext;
	private boolean overrideSpeedActive;
	private GameSpeed preOverrideSpeed;

	@Inject
	public GameSpeedMessageHandler(MessageDispatcher messageDispatcher, SettlerTracker settlerTracker) {
		this.settlerTracker = settlerTracker;
		this.messageDispatcher = messageDispatcher;

		messageDispatcher.addListener(this, MessageType.SET_GAME_SPEED);
		messageDispatcher.addListener(this, MessageType.SETTLER_FELL_ASLEEP);
		messageDispatcher.addListener(this, MessageType.SETTLER_WOKE_UP);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.SETTLER_FELL_ASLEEP: {
				boolean allAsleep = true;
				for (Entity entity : settlerTracker.getLiving()) {
					CreatureEntityAttributes attributes = (CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
					if (Consciousness.AWAKE.equals(attributes.getConsciousness())) {
						allAsleep = false;
						break;
					}
				}

				if (allAsleep) {
					preOverrideSpeed = gameContext.getGameClock().getCurrentGameSpeed();
					messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, GameSpeed.SPEED4);
					overrideSpeedActive = true;
				}
				return true;
			}
			case MessageType.SETTLER_WOKE_UP: {
				if (overrideSpeedActive) {
					messageDispatcher.dispatchMessage(MessageType.SET_GAME_SPEED, preOverrideSpeed);
				}
				overrideSpeedActive = false;
				return true;
			}
			case MessageType.SET_GAME_SPEED: {
				if (gameContext == null || gameContext.getSettlementState().getGameState().equals(SELECT_SPAWN_LOCATION) ||
						gameContext.getSettlementState().getGameState().equals(STARTING_SPAWN)) {
					return true;
				}
				GameSpeed selectedSpeed = (GameSpeed) msg.extraInfo;
				if (selectedSpeed.equals(GameSpeed.PAUSED)) {
					if (gameContext.getGameClock().isPaused()) {
						gameContext.getGameClock().setPaused(false);
						messageDispatcher.dispatchMessage(MessageType.GAME_PAUSED, gameContext.getGameClock().isPaused());
						selectedSpeed = gameContext.getGameClock().getCurrentGameSpeed(); // To re-highlight the previously picked speed
					} else {
						gameContext.getGameClock().setPaused(true);
						messageDispatcher.dispatchMessage(MessageType.GAME_PAUSED, gameContext.getGameClock().isPaused());
					}
				} else {
					overrideSpeedActive = false;
					gameContext.getGameClock().setPaused(false);
					messageDispatcher.dispatchMessage(MessageType.GAME_PAUSED, gameContext.getGameClock().isPaused());
					gameContext.getGameClock().setCurrentGameSpeed(selectedSpeed);
				}

				messageDispatcher.dispatchMessage(MessageType.GAME_SPEED_CHANGED, selectedSpeed);
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	@Override
	public void clearContextRelatedState() {
		preOverrideSpeed = null;
		overrideSpeedActive = false;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
		messageDispatcher.dispatchMessage(MessageType.GAME_PAUSED, gameContext.getGameClock().isPaused());
	}

}
