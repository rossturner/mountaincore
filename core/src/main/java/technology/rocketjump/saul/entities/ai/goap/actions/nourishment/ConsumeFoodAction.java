package technology.rocketjump.saul.entities.ai.goap.actions.nourishment;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.saul.audio.model.SoundAsset;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.EntityNeed;
import technology.rocketjump.saul.entities.ai.goap.actions.Action;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.components.creature.HappinessComponent;
import technology.rocketjump.saul.entities.components.creature.NeedsComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.saul.entities.model.physical.creature.status.Poisoned;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.RequestSoundMessage;
import technology.rocketjump.saul.messaging.types.StatusMessage;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.SUCCESS;
import static technology.rocketjump.saul.entities.components.creature.HappinessComponent.HappinessModifier.ATE_NICELY_PREPARED_FOOD;

public class ConsumeFoodAction extends Action {

	private static final double AMOUNT_FOOD_NEED_RESTORED = 50;
	private static final float TIME_TO_SPEND_EATING_SECONDS = 6;

	private float elapsedTime;
	private boolean activeSoundTriggered;

	public ConsumeFoodAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (parent.getFoodAllocation() == null || parent.getFoodAllocation().getTargetEntity() == null) {
			completionType = FAILURE;
			return;
		}

		if (!activeSoundTriggered) {
			Entity targetEntity = parent.getFoodAllocation().getTargetEntity();
			ItemEntityAttributes itemEntityAttributes = (ItemEntityAttributes) targetEntity.getPhysicalEntityComponent().getAttributes();

			SoundAsset consumptionSoundAsset = itemEntityAttributes.getItemType().getConsumeSoundAsset();
			if (consumptionSoundAsset != null) {
				parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(consumptionSoundAsset,
						parent.parentEntity.getId(), parent.parentEntity.getLocationComponent().getWorldOrParentPosition(), null));
			}
			activeSoundTriggered = true;
		}

		elapsedTime += deltaTime;
		if (elapsedTime > TIME_TO_SPEND_EATING_SECONDS) {
			Entity targetEntity = parent.getFoodAllocation().getTargetEntity();
			ItemEntityAttributes itemEntityAttributes = (ItemEntityAttributes) targetEntity.getPhysicalEntityComponent().getAttributes();
			itemEntityAttributes.setQuantity(itemEntityAttributes.getQuantity() - 1);

			if (itemEntityAttributes.getQuantity() <= 0) {
				parent.messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, targetEntity);
			} else {
				placeFoodBackIntoInventory(gameContext, targetEntity);
			}

			GameMaterial primaryMaterial = itemEntityAttributes.getMaterial(itemEntityAttributes.getItemType().getPrimaryMaterialType());
			// FIXME Investigate how primary material is sometimes null. Perhaps generated materials?
			if (primaryMaterial != null && primaryMaterial.isPoisonous()) {
				parent.messageDispatcher.dispatchMessage(MessageType.APPLY_STATUS, new StatusMessage(parent.parentEntity, Poisoned.class, null));
			}

			NeedsComponent needsComponent = parent.parentEntity.getComponent(NeedsComponent.class);
			needsComponent.setValue(EntityNeed.FOOD, needsComponent.getValue(EntityNeed.FOOD) + AMOUNT_FOOD_NEED_RESTORED);

			if (parent.getFoodAllocation().isPreparedMeal()) {
				HappinessComponent happinessComponent = parent.parentEntity.getComponent(HappinessComponent.class);
				happinessComponent.add(ATE_NICELY_PREPARED_FOOD);
			}
			completionType = SUCCESS;
		}
	}

	private void placeFoodBackIntoInventory(GameContext gameContext, Entity targetEntity) {
		// Place (back) into inventory
		InventoryComponent parentInventory = parent.parentEntity.getOrCreateComponent(InventoryComponent.class);
		EquippedItemComponent equippedItemComponent = parent.parentEntity.getComponent(EquippedItemComponent.class);
		if (equippedItemComponent != null && equippedItemComponent.getEquippedItem().equals(targetEntity)) {
			equippedItemComponent.clearEquippedItem();
			parent.parentEntity.removeComponent(EquippedItemComponent.class);
			parentInventory.add(targetEntity, parent.parentEntity, parent.messageDispatcher, gameContext.getGameClock());
		} else {
			Entity containerEntity = targetEntity.getLocationComponent().getContainerEntity();
			if (containerEntity != null && !containerEntity.equals(parent.parentEntity)) {
				// Currently within a different inventory
				InventoryComponent otherInventoryComponent = containerEntity.getComponent(InventoryComponent.class);
				otherInventoryComponent.remove(targetEntity.getId());
				parentInventory.add(targetEntity, parent.parentEntity, parent.messageDispatcher, gameContext.getGameClock());
			}
		}
	}

	@Override
	public void actionInterrupted(GameContext gameContext) {
		Entity targetEntity = parent.getFoodAllocation().getTargetEntity();
		if (targetEntity != null) {
			placeFoodBackIntoInventory(gameContext, targetEntity);
		}
		completionType = CompletionType.FAILURE;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("elapsed", elapsedTime);
		if (activeSoundTriggered) {
			asJson.put("soundTriggered", true);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		elapsedTime = asJson.getFloatValue("elapsed");
		this.activeSoundTriggered = asJson.getBooleanValue("soundTriggered");
	}

}
