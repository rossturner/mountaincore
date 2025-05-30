package technology.rocketjump.mountaincore.entities.model.physical.creature.status;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.behaviour.effects.FireEffectBehaviour;
import technology.rocketjump.mountaincore.entities.components.AttachedEntitiesComponent;
import technology.rocketjump.mountaincore.entities.components.BehaviourComponent;
import technology.rocketjump.mountaincore.entities.components.ItemAllocation;
import technology.rocketjump.mountaincore.entities.components.ItemAllocationComponent;
import technology.rocketjump.mountaincore.entities.components.creature.HappinessComponent;
import technology.rocketjump.mountaincore.entities.model.physical.AttachedEntity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.DeathReason;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.misc.VectorUtils;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;

public class OnFireStatus extends StatusEffect {

	private boolean litFire;
	private boolean initialised;

	public OnFireStatus() {
		super(Death.class, Double.MAX_VALUE, DeathReason.BURNING, null);
	}

	@Override
	public void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher) {
		initialised = true;
		if (!litFire) {
			messageDispatcher.dispatchMessage(MessageType.ADD_FIRE_TO_ENTITY, parentEntity);
			litFire = true;

			ItemAllocationComponent itemAllocationComponent = parentEntity.getComponent(ItemAllocationComponent.class);
			if (itemAllocationComponent != null) {
				for (ItemAllocation itemAllocation : new ArrayList<>(itemAllocationComponent.getAll())) {
					messageDispatcher.dispatchMessage(MessageType.CANCEL_ITEM_ALLOCATION, itemAllocation);
				}
				itemAllocationComponent.createAllocation(itemAllocationComponent.getNumUnallocated(), parentEntity, ItemAllocation.Purpose.ON_FIRE);
			}
		}

		HappinessComponent happinessComponent = parentEntity.getComponent(HappinessComponent.class);
		if (happinessComponent != null) {
			happinessComponent.add(HappinessComponent.HappinessModifier.ON_FIRE);
		}
	}

	@Override
	public void onRemoval(GameContext gameContext, MessageDispatcher messageDispatcher) {
		AttachedEntitiesComponent attachedEntitiesComponent = parentEntity.getComponent(AttachedEntitiesComponent.class);
		if (attachedEntitiesComponent != null) {
			for (AttachedEntity attachedEntity : attachedEntitiesComponent.getAttachedEntities()) {
				BehaviourComponent behaviourComponent = attachedEntity.entity.getBehaviourComponent();
				if (behaviourComponent instanceof FireEffectBehaviour) {
					((FireEffectBehaviour)behaviourComponent).setToFade();
				}
			}
		}

		ItemAllocationComponent itemAllocationComponent = parentEntity.getComponent(ItemAllocationComponent.class);
		if (itemAllocationComponent != null) {
			itemAllocationComponent.cancelAll();
		}

		messageDispatcher.dispatchMessage(MessageType.FIRE_REMOVED, VectorUtils.toGridPoint(parentEntity.getLocationComponent().getWorldPosition()));
	}

	@Override
	public boolean checkForRemoval(GameContext gameContext) {
		if (initialised) {
			AttachedEntitiesComponent attachedEntitiesComponent = parentEntity.getComponent(AttachedEntitiesComponent.class);
			return attachedEntitiesComponent == null || attachedEntitiesComponent.getAttachedEntities().stream()
					.noneMatch(a -> a.entity.getBehaviourComponent() instanceof FireEffectBehaviour);
		} else {
			return false;
		}
	}

	@Override
	public String getI18Key() {
		return "STATUS.ON_FIRE";
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);

		if (litFire) {
			asJson.put("litFire", true);
		}
		if (initialised) {
			asJson.put("initialised", true);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		this.litFire = asJson.getBooleanValue("litFire");
		this.initialised = asJson.getBooleanValue("initialised");
	}

}
