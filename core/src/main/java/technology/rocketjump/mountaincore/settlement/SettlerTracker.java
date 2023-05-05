package technology.rocketjump.mountaincore.settlement;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Consciousness;
import technology.rocketjump.mountaincore.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.GameContextAware;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.MessageType.SettlerLocateDrinkStatusMessage;
import technology.rocketjump.mountaincore.messaging.types.LocateSettlersMessage;
import technology.rocketjump.mountaincore.settlement.notifications.Notification;
import technology.rocketjump.mountaincore.settlement.notifications.NotificationType;
import technology.rocketjump.mountaincore.ui.Selectable;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is responsible for keeping track of all items (allocated or not) on the map
 *
 * TODO merge this into creature tracker and use faction and sapience to distinguish things like settlers
 */
@Singleton
public class SettlerTracker implements GameContextAware, Telegraph {

	private final Map<Long, Entity> livingSettlers = new HashMap<>();
	private final Map<Long, Entity> deadSettlers = new HashMap<>();
	private final Map<Long, Entity> trappedSettlers = new HashMap<>();

	private final MessageDispatcher messageDispatcher;
	private GameContext gameContext;

	@Inject
	public SettlerTracker(MessageDispatcher messageDispatcher) {
		this.messageDispatcher = messageDispatcher;
		this.messageDispatcher.addListener(this, MessageType.LOCATE_SETTLERS_IN_REGION);
		this.messageDispatcher.addListener(this, MessageType.SETTLER_LOCATE_DRINK_STATUS);
	}

	public void settlerAdded(Entity entity) {
		if (entity.getLocationComponent().isUntracked()) {
			return;
		}

		CreatureEntityAttributes attributes = (CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
		if (attributes.getConsciousness().equals(Consciousness.DEAD)) {
			deadSettlers.put(entity.getId(), entity);
		} else {
			livingSettlers.put(entity.getId(), entity);
		}
	}

	public void settlerRemoved(Entity entity) {
		livingSettlers.remove(entity.getId());
		trappedSettlers.remove(entity.getId());
		deadSettlers.remove(entity.getId());
	}

	public void settlerDied(Entity entity) {
		livingSettlers.remove(entity.getId());
		trappedSettlers.remove(entity.getId());
		deadSettlers.put(entity.getId(), entity);
	}

	public Collection<Entity> getLiving() {
		return livingSettlers.values();
	}

	public Collection<Entity> getDead() {
		return deadSettlers.values();
	}

	public int count() {
		return livingSettlers.size();
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {
		livingSettlers.clear();
		deadSettlers.clear();
		trappedSettlers.clear();
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.LOCATE_SETTLERS_IN_REGION -> {
				LocateSettlersMessage message = (LocateSettlersMessage) msg.extraInfo;
				List<Entity> foundSettlers = livingSettlers.values().stream()
						.filter(entity -> {
							Vector2 worldPosition = entity.getLocationComponent().getWorldOrParentPosition();
							MapTile mapTile = gameContext.getAreaMap().getTile(worldPosition);
							if (mapTile != null) {
								return mapTile.getRegionId() == message.regionId;
							} else {
								return false;
							}
						})
						.toList();
				message.callback.accept(foundSettlers);
				return true;
			}
			case MessageType.SETTLER_LOCATE_DRINK_STATUS -> {
				SettlerLocateDrinkStatusMessage message = (SettlerLocateDrinkStatusMessage) msg.extraInfo;
				Entity settler = message.settler();
				if (settler.isSettler()) {
					int sizeBefore = trappedSettlers.size();
					if (message.drinkFound()) {
						trappedSettlers.remove(settler.getId());
					} else {
						trappedSettlers.put(settler.getId(), settler);
						if (sizeBefore == 0) {
							Notification notification = new Notification(NotificationType.SETTLER_STUCK,
									settler.getLocationComponent().getWorldOrParentPosition(), new Selectable(settler, 0));
							messageDispatcher.dispatchMessage(MessageType.POST_NOTIFICATION, notification);
						}
					}
				}

				return true;
			}
			default ->
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}
}
