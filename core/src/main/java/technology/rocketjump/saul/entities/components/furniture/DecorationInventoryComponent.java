package technology.rocketjump.saul.entities.components.furniture;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.saul.entities.components.EntityComponent;
import technology.rocketjump.saul.entities.components.InfrequentlyUpdatableComponent;
import technology.rocketjump.saul.entities.components.ItemAllocationComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static technology.rocketjump.saul.entities.model.EntityType.ITEM;

public class DecorationInventoryComponent implements InfrequentlyUpdatableComponent {

	private Map<Long, Entity> decorationEntities = new LinkedHashMap<>();
	private Entity parentEntity;
	private GameContext gameContext;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.gameContext = gameContext;
		this.parentEntity = parentEntity;
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		DecorationInventoryComponent cloned = new DecorationInventoryComponent();
		for (Entity decorationItem : decorationEntities.values()) {
			cloned.add(decorationItem.clone(messageDispatcher, gameContext));
		}
		return cloned;
	}

	@Override
	public void infrequentUpdate(double elapsedTime) {
		for (Entity entity : new ArrayList<>(decorationEntities.values())) {
			entity.infrequentUpdate(gameContext);
		}
	}

	public void clear() {
		for (Entity decorationEntity : decorationEntities.values()) {
			decorationEntity.getLocationComponent(true).setContainerEntity(null);
		}
		decorationEntities.clear();
		// Only expecting this to be used to lose any reference to decoration entities
	}

	public void add(Entity entity) {
		decorationEntities.put(entity.getId(), entity);
		entity.getLocationComponent(true).setWorldPosition(null, false, false);
		entity.getLocationComponent(true).setContainerEntity(parentEntity);
		entity.getLocationComponent(true).setOrientation(EntityAssetOrientation.DOWN);
	}

	public Collection<Entity> getDecorationEntities() {
		return decorationEntities.values();
	}

	public Entity findByItemType(ItemType itemType) {
		for (Entity item : decorationEntities.values()) {
			if (ITEM.equals(item.getType())) {
				ItemEntityAttributes attributes = (ItemEntityAttributes) item.getPhysicalEntityComponent().getAttributes();
				if (itemType.equals(attributes.getItemType())) {
					return item;
				}
			}
		}
		return null;
	}

	public Entity findByItemTypeAndMaterial(ItemType itemType, GameMaterial material) {
		for (Entity item : decorationEntities.values()) {
			if (ITEM.equals(item.getType())) {
				ItemEntityAttributes attributes = (ItemEntityAttributes) item.getPhysicalEntityComponent().getAttributes();
				if (itemType.equals(attributes.getItemType()) && material.equals(attributes.getMaterial(material.getMaterialType()))) {
					return item;
				}
			}
		}
		return null;
	}

	public Entity remove(long entityToRemoveId) {
		Entity removed = decorationEntities.remove(entityToRemoveId);
		if (removed == null) {
			return null;
		} else {
			removed.getLocationComponent(true).setContainerEntity(null);
			if (removed.getType().equals(ITEM)) {
				ItemAllocationComponent itemAllocationComponent = removed.getOrCreateComponent(ItemAllocationComponent.class);
				itemAllocationComponent.cancelAll();
			}
			return removed;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (!decorationEntities.isEmpty()) {
			JSONArray decorationEntitiesJson = new JSONArray();
			for (Entity decorationItem : decorationEntities.values()) {
				decorationItem.writeTo(savedGameStateHolder);
				decorationEntitiesJson.add(decorationItem.getId());
			}
			asJson.put("decorations", decorationEntitiesJson);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		JSONArray decorations = asJson.getJSONArray("decorations");
		if (decorations != null) {
			for (int cursor = 0; cursor < decorations.size(); cursor++) {
				Entity entity = savedGameStateHolder.entities.get(decorations.getLong(cursor));
				if (entity == null) {
					throw new InvalidSaveException("Could not find entity by ID " + decorations.getLong(cursor));
				} else {
					decorationEntities.put(entity.getId(), entity);
				}
			}
		}
	}
}
