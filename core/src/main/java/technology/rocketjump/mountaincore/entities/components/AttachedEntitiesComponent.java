package technology.rocketjump.mountaincore.entities.components;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.AttachedEntity;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemHoldPosition;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.persistence.EnumParser;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.List;

public class AttachedEntitiesComponent implements ParentDependentEntityComponent {

	private Entity parentEntity;

	private final List<AttachedEntity> attachedEntities = new ArrayList<>();

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		AttachedEntitiesComponent clone = new AttachedEntitiesComponent();
		return clone;
	}

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.parentEntity = parentEntity;
	}

	public void addAttachedEntity(Entity other) {
		attachedEntities.add(new AttachedEntity(other, ItemHoldPosition.UNSPECIFIED));
		other.getLocationComponent().setContainerEntity(parentEntity);
		other.getLocationComponent().setWorldPosition(null, false);
		other.getLocationComponent().setOrientation(parentEntity.getLocationComponent().getOrientation());
	}

	public void addAttachedEntity(Entity other, ItemHoldPosition position) {
		attachedEntities.add(new AttachedEntity(other, position));
		other.getLocationComponent().setContainerEntity(parentEntity);
		other.getLocationComponent().setWorldPosition(null, false);
		other.getLocationComponent().setOrientation(parentEntity.getLocationComponent().getOrientation());
	}

	public List<AttachedEntity> getAttachedEntities() {
		return attachedEntities;
	}

	public void remove(Entity removedEntity) {
		if (attachedEntities.removeIf(attachedEntity -> attachedEntity.entity.getId() == removedEntity.getId())) {
			removedEntity.getLocationComponent().setContainerEntity(null);
		}
	}

	public void destroyAllEntities(MessageDispatcher messageDispatcher) {
		attachedEntities.stream().map(a -> a.entity).toList().forEach(e -> messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY_AND_ALL_INVENTORY, e));
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		JSONArray attachedEntitiesJson = new JSONArray();
		for (AttachedEntity attachedEntity : attachedEntities) {
			attachedEntity.entity.writeTo(savedGameStateHolder);

			JSONObject attachmentJson = new JSONObject(true);
			attachmentJson.put("entity", attachedEntity.entity.getId());
			attachmentJson.put("position", attachedEntity.holdPosition.name());
			attachedEntitiesJson.add(attachmentJson);
		}
		asJson.put("entities", attachedEntitiesJson);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		JSONArray attachedEntitiesJson = asJson.getJSONArray("entities");
		for (int cursor = 0; cursor < attachedEntitiesJson.size(); cursor++) {
			JSONObject attachmentJson = attachedEntitiesJson.getJSONObject(cursor);
			Entity entity = savedGameStateHolder.entities.get(attachmentJson.getLong("entity"));
			ItemHoldPosition position = EnumParser.getEnumValue(attachmentJson, "position", ItemHoldPosition.class, ItemHoldPosition.UNSPECIFIED);
			if (entity == null) {
				throw new InvalidSaveException("Entities persisted in wrong order");
			} else {
				attachedEntities.add(new AttachedEntity(entity, position));
			}
		}
	}
}
