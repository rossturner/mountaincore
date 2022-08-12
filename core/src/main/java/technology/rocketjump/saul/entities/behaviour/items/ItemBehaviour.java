package technology.rocketjump.saul.entities.behaviour.items;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
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

public class ItemBehaviour implements BehaviourComponent {

	private LocationComponent locationComponent;
	private MessageDispatcher messageDispatcher;
	private Entity parentEntity;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.locationComponent = parentEntity.getLocationComponent();
		this.messageDispatcher = messageDispatcher;
		this.parentEntity = parentEntity;
	}

	@Override
	public ItemBehaviour clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		ItemBehaviour cloned = new ItemBehaviour();
		cloned.init(parentEntity, messageDispatcher, gameContext);
		return cloned;
	}

	@Override
	public void update(float deltaTime) {
		// Do nothing, does not update every frame
	}

	@Override
	public void updateWhenPaused() {

	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		ItemEntityAttributes attributes = (ItemEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
		ItemAllocationComponent itemAllocationComponent = parentEntity.getComponent(ItemAllocationComponent.class);
		if (locationComponent.getWorldPosition() != null && attributes.getItemPlacement().equals(ItemPlacement.ON_GROUND) && itemAllocationComponent.getNumUnallocated() > 0) {
			// Has some unallocated on ground
			MapTile tile = gameContext.getAreaMap().getTile(locationComponent.getWorldPosition());
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
	}

	@Override
	public SteeringComponent getSteeringComponent() {
		return null;
	}

	@Override
	public boolean isUpdateEveryFrame() {
		return false;
	}

	@Override
	public boolean isUpdateInfrequently() {
		return true;
	}

	@Override
	public boolean isJobAssignable() {
		return false;
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
