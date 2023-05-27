package technology.rocketjump.mountaincore.entities.ai.goap.actions.nourishment;

import com.alibaba.fastjson.JSONObject;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.EntityNeed;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.ai.memory.Memory;
import technology.rocketjump.mountaincore.entities.ai.memory.MemoryType;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.components.LiquidAllocation;
import technology.rocketjump.mountaincore.entities.components.LiquidContainerComponent;
import technology.rocketjump.mountaincore.entities.components.creature.HappinessComponent;
import technology.rocketjump.mountaincore.entities.components.creature.MemoryComponent;
import technology.rocketjump.mountaincore.entities.components.creature.NeedsComponent;
import technology.rocketjump.mountaincore.entities.components.creature.StatusComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.mountaincore.entities.model.physical.creature.status.alcohol.Drunk;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.mountaincore.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.mountaincore.entities.ai.goap.actions.Action.CompletionType.SUCCESS;
import static technology.rocketjump.mountaincore.entities.components.LiquidAllocation.LiquidAllocationType.FROM_RIVER;
import static technology.rocketjump.mountaincore.entities.components.creature.HappinessComponent.HappinessModifier.DRANK_FROM_RIVER;

public class ConsumeLiquidFromContainerAction extends Action {

	private static final double AMOUNT_DRINK_NEED_RESTORED = 70.0;
	public static final float TIME_TO_SPEND_DRINKING_SECONDS = 4.5f;

	protected float elapsedTime;

	public ConsumeLiquidFromContainerAction(AssignedGoal parent) {
		super(parent);
	}

	protected float getTimeToSpendDrinking() {
		return TIME_TO_SPEND_DRINKING_SECONDS;
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		LiquidAllocation liquidAllocation = parent.getLiquidAllocation();
		if (liquidAllocation == null) {
			completionType = FAILURE;
			return;
		}

		Entity inventoryContainer = tryEquipContainerFromInventory(gameContext);

		GameMaterial consumedLiquid = GameMaterial.NULL_MATERIAL;
		elapsedTime += deltaTime;
		if (elapsedTime > getTimeToSpendDrinking()) {
			// Just going to assume we're on the correct position, doesn't matter too much if we were pushed away
			MapTile targetZoneTile = gameContext.getAreaMap().getTile(liquidAllocation.getTargetZoneTile().getTargetTile());
			Entity targetFurniture = getFirstFurnitureEntity(targetZoneTile);
			if (targetFurniture != null) {
				LiquidContainerComponent liquidContainerComponent = targetFurniture.getComponent(LiquidContainerComponent.class);
				if (liquidContainerComponent != null) {
					LiquidAllocation success = liquidContainerComponent.cancelAllocationAndDecrementQuantity(liquidAllocation);
					parent.setLiquidAllocation(null);
					if (success != null) {
						consumedLiquid = liquidContainerComponent.getTargetLiquidMaterial();
						completionType = SUCCESS;
					} else {
						Logger.error("Failed to cancel liquid allocation correctly");
						completionType = FAILURE;
					}
				} else {
					Logger.error("Target furniture for " + this.getClass().getSimpleName() + " does not have " + LiquidContainerComponent.class.getSimpleName());
					completionType = FAILURE;
				}
			} else if (targetZoneTile.getFloor().isRiverTile()) {
				// No real liquid to remove
				consumedLiquid = targetZoneTile.getFloor().getMaterial();
				completionType = SUCCESS;
			} else if (inventoryContainer != null) {
				completionType = FAILURE;
				LiquidContainerComponent container = inventoryContainer.getComponent(LiquidContainerComponent.class);
				LiquidAllocation success = container.cancelAllocationAndDecrementQuantity(liquidAllocation);
				parent.setLiquidAllocation(null);
				parent.parentEntity.getComponent(EquippedItemComponent.class).clearMainHandItem();
				parent.parentEntity.getComponent(InventoryComponent.class).add(inventoryContainer, parent.parentEntity, parent.messageDispatcher, gameContext.getGameClock());

				if (success != null) {
					consumedLiquid = container.getTargetLiquidMaterial();
					completionType = SUCCESS;
				}
			} else {
				Logger.error("Not found target for " + this.getClass().getSimpleName() + ", could be removed furniture");
				completionType = FAILURE;
			}

			if (completionType.equals(SUCCESS)) {
				effectsOfDrinkConsumption(consumedLiquid, liquidAllocation, gameContext);
			}
		}
	}

	private Entity tryEquipContainerFromInventory(GameContext gameContext) {
		LiquidAllocation liquidAllocation = parent.getLiquidAllocation();
		if (LiquidAllocation.LiquidAllocationType.REQUESTER_INVENTORY == liquidAllocation.getType()) {
			InventoryComponent inventory = parent.parentEntity.getComponent(InventoryComponent.class);
			EquippedItemComponent equipped = parent.parentEntity.getOrCreateComponent(EquippedItemComponent.class);
			if (inventory == null) {
				return null;
			} else {
				Long containerId = liquidAllocation.getTargetContainerId();
				Entity inInventory = inventory.getById(containerId);
				Entity mainHandItem = equipped.getMainHandItem();
				if (mainHandItem != null && containerId.equals(mainHandItem.getId())) {
					return mainHandItem;
				} else if (inInventory != null && equipped.isMainHandEnabled()) {
					inInventory = inventory.remove(inInventory.getId());
					if (equipped.getMainHandItem() != null) {
						inventory.add(equipped.clearMainHandItem(), parent.parentEntity, parent.messageDispatcher, gameContext.getGameClock());
					}
					equipped.setMainHandItem(inInventory, parent.parentEntity, parent.messageDispatcher);
					return inInventory;
				}
			}
		}
		return null;
	}

	protected void effectsOfDrinkConsumption(GameMaterial consumedLiquid, LiquidAllocation liquidAllocation, GameContext gameContext) {
		HappinessComponent happinessComponent = parent.parentEntity.getComponent(HappinessComponent.class);
		if (liquidAllocation != null && FROM_RIVER.equals(liquidAllocation.getType()) && happinessComponent != null) {
			happinessComponent.add(DRANK_FROM_RIVER);
		}

		if (consumedLiquid == null) {
			Logger.error("Null material consumed as liquid");
			return;
		}

		if (consumedLiquid.isQuenchesThirst()) {
			NeedsComponent needsComponent = parent.parentEntity.getComponent(NeedsComponent.class);
			needsComponent.setValue(EntityNeed.DRINK, needsComponent.getValue(EntityNeed.DRINK) + AMOUNT_DRINK_NEED_RESTORED);
		} else {
			Logger.warn("Consuming liquid which does not quench thirst: " + consumedLiquid.toString());
		}

		if (consumedLiquid.isAlcoholic()) {
			if (happinessComponent != null) {
				happinessComponent.add(HappinessComponent.HappinessModifier.DRANK_ALCOHOL);
			}
			MemoryComponent memoryComponent = parent.parentEntity.getComponent(MemoryComponent.class);
			if (memoryComponent != null) {
				memoryComponent.addShortTerm(new Memory(MemoryType.CONSUMED_ALCOHOLIC_DRINK, gameContext.getGameClock()), gameContext.getGameClock());
			}
			parent.parentEntity.getComponent(StatusComponent.class).apply(new Drunk());
		}

	}

	public static Entity getFirstFurnitureEntity(MapTile targetTile) {
		for (Entity entity : targetTile.getEntities()) {
			if (entity.getType().equals(EntityType.FURNITURE)) {
				return entity;
			}
		}
		return null;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("elapsed", elapsedTime);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		elapsedTime = asJson.getFloatValue("elapsed");
	}
}
