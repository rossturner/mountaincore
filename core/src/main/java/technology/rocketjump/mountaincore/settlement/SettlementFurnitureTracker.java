package technology.rocketjump.mountaincore.settlement;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.entities.components.creature.StatusComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.status.OnFireStatus;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.DoorwayEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.mountaincore.entities.tags.Tag;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.GameContextAware;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.FurnitureAssignmentRequest;
import technology.rocketjump.mountaincore.messaging.types.GetFurnitureByTagMessage;

import java.util.*;

import static java.util.Collections.emptyMap;

/**
 * This class is responsible for keeping track of all furniture (allocated or not) on the map BELONGING TO THE SETTLEMENT
 */
@Singleton
public class SettlementFurnitureTracker implements GameContextAware, Telegraph {

	private GameContext gameContext;

	private final Map<FurnitureType, Map<Long, Entity>> byFurnitureType = new HashMap<>();
	// Note that tags are mostly attached to assets, which can change over time, but furniture tends to have static assets so this shouldn't be an issue here
	private final Map<Class<? extends Tag>, Map<Long, Entity>> byTag = new HashMap<>();

	@Inject
	public SettlementFurnitureTracker(MessageDispatcher messageDispatcher) {
		messageDispatcher.addListener(this, MessageType.REQUEST_FURNITURE_ASSIGNMENT);
		messageDispatcher.addListener(this, MessageType.GET_FURNITURE_BY_TAG);
	}

	public void furnitureAdded(Entity entity) {
		if (entity.getLocationComponent().isUntracked()) {
			return;
		}

		FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
		if (attributes instanceof DoorwayEntityAttributes) {
			return;
		}
		FurnitureType furnitureType = attributes.getFurnitureType();

		byFurnitureType.computeIfAbsent(furnitureType, (f) -> new HashMap<>()).put(entity.getId(), entity);
		for (Tag tag : entity.getTags()) {
			byTag.computeIfAbsent(tag.getClass(), (f) -> new HashMap<>()).put(entity.getId(), entity);
		}
	}

	public void furnitureRemoved(Entity entity) {
		FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
		if (attributes instanceof DoorwayEntityAttributes) {
			return;
		}
		FurnitureType furnitureType = attributes.getFurnitureType();
		Map<Long, Entity> byFurnitureType = this.byFurnitureType.get(furnitureType);
		if (byFurnitureType != null) {
			byFurnitureType.remove(entity.getId());
		}
		for (Tag tag : entity.getTags()) {
			byTag.getOrDefault(tag.getClass(), emptyMap()).remove(entity.getId());
		}
	}

	public Collection<Entity> findByTag(Class<? extends Tag> tagType, boolean unassignedOnly) {
		return findEntitiesFrom(byTag.get(tagType), unassignedOnly);
	}

	public Collection<Entity> findByFurnitureType(FurnitureType furnitureType, boolean unassignedOnly) {
		return findEntitiesFrom(byFurnitureType.get(furnitureType), unassignedOnly);
	}

	private Collection<Entity> findEntitiesFrom(Map<Long, Entity> entityMap, boolean unassignedOnly) {
		if (entityMap != null) {
			if (unassignedOnly) {
				List<Entity> found = new ArrayList<>();
				for (Entity entity : entityMap.values()) {
					FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
					if (attributes.getAssignedToEntityId() == null) {
						found.add(entity);
					}
				}
				return found;
			} else {
				return entityMap.values();
			}
		} else {
			return Collections.emptyList();
		}
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.REQUEST_FURNITURE_ASSIGNMENT: {
				FurnitureAssignmentRequest request = (FurnitureAssignmentRequest) msg.extraInfo;
				List<Entity> matched = findByTag(request.requiredTag, true)
						.stream()
						.filter(entity -> {
							StatusComponent statusComponent = entity.getComponent(StatusComponent.class);
							return statusComponent == null || !statusComponent.contains(OnFireStatus.class);
						})
						.filter(request.filter)
						.toList();
				Vector2 requesterPosition = request.requestingEntity.getLocationComponent().getWorldOrParentPosition();
				long requesterRegionId = gameContext.getAreaMap().getTile(requesterPosition).getRegionId();
				Entity nearest = null;
				float nearestDistance2 = Float.MAX_VALUE;
				for (Entity entity : matched) {
					long entityRegionId = gameContext.getAreaMap().getTile(entity.getLocationComponent().getWorldPosition()).getRegionId();
					if (entityRegionId == requesterRegionId) {
						float dst2 = entity.getLocationComponent().getWorldPosition().dst2(requesterPosition);
						if (dst2 < nearestDistance2) {
							nearestDistance2 = dst2;
							nearest = entity;
						}
					}
				}

				if (nearest != null) {
					FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) nearest.getPhysicalEntityComponent().getAttributes();
					attributes.setAssignedToEntityId(request.requestingEntity.getId());
				}
				request.callback.accept(nearest);
				return true;
			}
			case MessageType.GET_FURNITURE_BY_TAG: {
				GetFurnitureByTagMessage message = (GetFurnitureByTagMessage) msg.extraInfo;
				Collection<Entity> found = findByTag(message.type(), false);
				message.callback().accept(found);
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this + ", " + msg);
		}
	}

	@Override
	public void clearContextRelatedState() {
		byFurnitureType.clear();
		byTag.clear();
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}
}
