package technology.rocketjump.saul.entities.behaviour.items;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.saul.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.saul.entities.components.BehaviourComponent;
import technology.rocketjump.saul.entities.components.ItemAllocationComponent;
import technology.rocketjump.saul.entities.components.creature.SteeringComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.LocationComponent;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.model.JobPriority;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.RequestHaulingMessage;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.rooms.Room;
import technology.rocketjump.saul.rooms.components.StockpileComponent;

import java.util.LinkedHashMap;
import java.util.Map;

public class ItemBehaviour implements BehaviourComponent {

	private LocationComponent locationComponent;
	private MessageDispatcher messageDispatcher;
	private Entity parentEntity;
	private GameContext gameContext;
	private Map<Class<? extends BehaviourComponent>, BehaviourComponent> additionalBehaviors = new LinkedHashMap<>();

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.locationComponent = parentEntity.getLocationComponent();
		this.messageDispatcher = messageDispatcher;
		this.parentEntity = parentEntity;
		this.gameContext = gameContext;
		for (BehaviourComponent additionalBehavior : additionalBehaviors.values()) {
			additionalBehavior.init(parentEntity, messageDispatcher, gameContext);
		}
	}

	public void addAdditionalBehaviour(BehaviourComponent additionalBehaviour) {
		additionalBehaviour.init(parentEntity, messageDispatcher, gameContext);

		this.additionalBehaviors.put(additionalBehaviour.getClass(), additionalBehaviour);
	}

	@Override
	public ItemBehaviour clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		ItemBehaviour cloned = new ItemBehaviour();
		cloned.additionalBehaviors = this.additionalBehaviors;
		cloned.init(parentEntity, messageDispatcher, gameContext);
		return cloned;
	}

	@Override
	public void update(float deltaTime) {
		// Do nothing, does not update every frame
		//TODO: Yagni: iterate additionalBehaviours to update too
	}

	@Override
	public void updateWhenPaused() {
		//TODO: Yagni: iterate additionalBehaviours to update too
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		ItemEntityAttributes attributes = (ItemEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		ItemAllocationComponent itemAllocationComponent = parentEntity.getComponent(ItemAllocationComponent.class);
		Vector2 worldPosition = locationComponent.getWorldPosition();
		MapTile tile = gameContext.getAreaMap().getTile(worldPosition);

		if (worldPosition != null && attributes.getItemPlacement().equals(ItemPlacement.ON_GROUND) && itemAllocationComponent.getNumUnallocated() > 0) {
			// Has some unallocated on ground
			boolean inStockpile = false;
			if (tile.getRoomTile() != null) {
				Room room = tile.getRoomTile().getRoom();
				StockpileComponent stockpileComponent = room.getComponent(StockpileComponent.class);
				if (stockpileComponent != null && stockpileComponent.canHold(parentEntity)) {
					inStockpile = true;
				}
			}

			if (!inStockpile) {
				// Not in a stockpile and some unallocated, so see if we can be hauled to a stockpile
				messageDispatcher.dispatchMessage(MessageType.REQUEST_ENTITY_HAULING, new RequestHaulingMessage(parentEntity, parentEntity, false, JobPriority.NORMAL, null));
			}
		}

		for (BehaviourComponent additionalBehavior : additionalBehaviors.values()) {
			additionalBehavior.infrequentUpdate(gameContext);
		}
	}

	@Override
	public SteeringComponent getSteeringComponent() {
		return null;
	}

	@Override
	public boolean isUpdateEveryFrame() {
		return false;
		//TODO: Yagni: iterate additionalBehaviours
	}

	@Override
	public boolean isUpdateInfrequently() {
		return true;
		//TODO: Yagni: iterate additionalBehaviours
	}

	@Override
	public boolean isJobAssignable() {
		return false;
		//TODO: Yagni: iterate additionalBehaviours
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		// No state to write
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		// No state to read
	}
}
