package technology.rocketjump.mountaincore.entities.model.physical.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.mountaincore.entities.components.EntityComponent;
import technology.rocketjump.mountaincore.entities.components.ItemAllocation;
import technology.rocketjump.mountaincore.entities.components.ItemAllocationComponent;
import technology.rocketjump.mountaincore.entities.components.ParentDependentEntityComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.EntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.misc.Destructible;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

public class HaulingComponent implements ParentDependentEntityComponent, Destructible {

	private Entity hauledEntity;
	private transient Long hauledEntityId; // Only used during loading
	private transient boolean initialised; // Only used during loading

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (hauledEntityId != null) {
			this.hauledEntity = gameContext.getEntities().get(hauledEntityId);
			if (this.hauledEntity == null) {
				Logger.error("Could not find entity with ID {}", hauledEntityId);
			}
		}
		initialised = true;
	}


	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (hauledEntity != null) {
			messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, hauledEntity);
		}
	}

	public Entity getHauledEntity() {
		return hauledEntity;
	}

	public void setHauledEntity(Entity hauledEntity, MessageDispatcher messageDispatcher, Entity parentEntity) {
		this.hauledEntity = hauledEntity;
		EntityAttributes attributes = hauledEntity.getPhysicalEntityComponent().getAttributes();
		int quantity = 1;
		if (attributes instanceof ItemEntityAttributes itemAttributes) {
			itemAttributes.setItemPlacement(ItemPlacement.BEING_CARRIED);
			quantity = itemAttributes.getQuantity();
			messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, hauledEntity);
		}

		ItemAllocationComponent itemAllocationComponent = hauledEntity.getComponent(ItemAllocationComponent.class);
		if (itemAllocationComponent != null) {
			itemAllocationComponent.createAllocation(quantity, parentEntity, ItemAllocation.Purpose.HAULING);
		}
		hauledEntity.getLocationComponent().setWorldPosition(null, false);
		hauledEntity.getLocationComponent().setContainerEntity(parentEntity);
	}

	public Entity clearHauledEntity() {
		hauledEntity.getLocationComponent().setContainerEntity(null);
		ItemAllocationComponent itemAllocationComponent = hauledEntity.getComponent(ItemAllocationComponent.class);
		if (itemAllocationComponent != null) {
			itemAllocationComponent.cancelAll(ItemAllocation.Purpose.HAULING);
		}
		Entity temp = hauledEntity;
		hauledEntity = null;
		return temp;
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		HaulingComponent clonedComponent = new HaulingComponent();
		if (hauledEntity != null) {
			Logger.warn("Cloning " + this.getClass().getSimpleName() + " but not cloning hauled entity");
		}
		return clonedComponent;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (hauledEntity != null) {
			hauledEntity.writeTo(savedGameStateHolder);
			asJson.put("entity", hauledEntity.getId());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		Long entityId = asJson.getLong("entity");
		if (entityId != null) {
			this.hauledEntityId = entityId;
		}
	}
}
