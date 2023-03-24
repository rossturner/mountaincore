package technology.rocketjump.mountaincore.entities.behaviour.items;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.mountaincore.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.mountaincore.entities.components.BehaviourComponent;
import technology.rocketjump.mountaincore.entities.components.Faction;
import technology.rocketjump.mountaincore.entities.components.FactionComponent;
import technology.rocketjump.mountaincore.entities.components.ItemAllocationComponent;
import technology.rocketjump.mountaincore.entities.components.creature.SteeringComponent;
import technology.rocketjump.mountaincore.entities.components.furniture.FurnitureStockpileComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.LocationComponent;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.jobs.model.JobPriority;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.RequestHaulingMessage;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import java.util.LinkedHashMap;
import java.util.Map;

public class ItemBehaviour implements BehaviourComponent {

	private LocationComponent locationComponent;
	private MessageDispatcher messageDispatcher;
	private Entity parentEntity;
	private GameContext gameContext;
	private final Map<Class<? extends BehaviourComponent>, BehaviourComponent> additionalBehaviours = new LinkedHashMap<>();

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.locationComponent = parentEntity.getLocationComponent();
		this.messageDispatcher = messageDispatcher;
		this.parentEntity = parentEntity;
		this.gameContext = gameContext;
		for (BehaviourComponent additionalBehavior : additionalBehaviours.values()) {
			additionalBehavior.init(parentEntity, messageDispatcher, gameContext);
		}
	}

	public void addAdditionalBehaviour(BehaviourComponent additionalBehaviour) {
		additionalBehaviour.init(parentEntity, messageDispatcher, gameContext);

		this.additionalBehaviours.put(additionalBehaviour.getClass(), additionalBehaviour);
	}

	@Override
	public ItemBehaviour clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		ItemBehaviour cloned = new ItemBehaviour();
		for (BehaviourComponent behaviourComponent : this.additionalBehaviours.values()) {
			BehaviourComponent clonedAdditional = (BehaviourComponent) behaviourComponent.clone(messageDispatcher, gameContext);
			cloned.additionalBehaviours.put(clonedAdditional.getClass(), clonedAdditional);
		}
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

		if (itemAllocationComponent.getNumUnallocated() > 0 && parentEntity.getOrCreateComponent(FactionComponent.class).getFaction().equals(Faction.SETTLEMENT)) {
			if (worldPosition != null && attributes.getItemPlacement().equals(ItemPlacement.ON_GROUND)) {
				// This should haul to a stockpile or to a higher priority stockpile
				messageDispatcher.dispatchMessage(MessageType.REQUEST_ENTITY_HAULING, new RequestHaulingMessage(parentEntity, parentEntity, false, JobPriority.NORMAL, null));
			}

			Entity container = parentEntity.getLocationComponent().getContainerEntity();
			if (container != null) {
				boolean forceRemoval = false;
				if (container.getComponent(FurnitureStockpileComponent.class) != null) {
					forceRemoval = !container.getComponent(FurnitureStockpileComponent.class).getStockpileSettings().canHold(parentEntity);
				}
				messageDispatcher.dispatchMessage(MessageType.REQUEST_ENTITY_HAULING, new RequestHaulingMessage(parentEntity, parentEntity, forceRemoval, JobPriority.NORMAL, null));
			}
		}

		for (BehaviourComponent additionalBehavior : additionalBehaviours.values()) {
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
