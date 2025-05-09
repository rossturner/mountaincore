package technology.rocketjump.mountaincore.entities.components;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.nourishment.LocateDrinkAction;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.jobs.LiquidMessageHandler;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.ItemPrimaryMaterialChangedMessage;
import technology.rocketjump.mountaincore.misc.Destructible;
import technology.rocketjump.mountaincore.misc.VectorUtils;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.zones.Zone;
import technology.rocketjump.mountaincore.zones.ZoneClassification;
import technology.rocketjump.mountaincore.zones.ZoneTile;

import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class LiquidContainerComponent implements ParentDependentEntityComponent, Destructible {

	private static final int MIN_CAPACITY_TO_CLASS_AS_HIGH_CAPACITY = 10;
	public static final float SMALL_AMOUNT = 0.1f;
	public static DecimalFormat oneDecimalFormat = new DecimalFormat("#.#");

	private Entity parentEntity;
	private MessageDispatcher messageDispatcher;
	private GameContext gameContext;

	private GameMaterial targetLiquidMaterial;

	private float liquidQuantity = 0;
	private Set<LiquidAllocation> allocations = new HashSet<>();

	private int maxLiquidCapacity;
	private Zone liquidContainerAccessZone;
	private boolean alwaysInactive = false;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.parentEntity = parentEntity;
		this.messageDispatcher = messageDispatcher;
		this.gameContext = gameContext;

		// Create zone in area around furniture
		if (parentEntity.getType().equals(EntityType.FURNITURE) && parentEntity.getBehaviourComponent() != null &&
				liquidContainerAccessZone == null) {
			// Skip when BehaviourComponent is null as this implies its a furniture for placing via UI
			FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();

			if (targetLiquidMaterial == null) {
				Logger.error("Creating LiquidContainer zone with no targetLiquidMaterial specified");
			} else {
				if (!attributes.getCurrentLayout().getWorkspaces().isEmpty()) {
					liquidContainerAccessZone = new Zone(new ZoneClassification(ZoneClassification.ZoneType.LIQUID_SOURCE, true, targetLiquidMaterial,
							maxLiquidCapacity >= MIN_CAPACITY_TO_CLASS_AS_HIGH_CAPACITY));

					GridPoint2 furniturePosition = VectorUtils.toGridPoint(parentEntity.getLocationComponent().getWorldPosition());
					MapTile furnitureTile = gameContext.getAreaMap().getTile(furniturePosition);
					for (FurnitureLayout.Workspace workspace : attributes.getCurrentLayout().getWorkspaces()) {
						GridPoint2 workspaceLocation = furniturePosition.cpy().add(workspace.getAccessedFrom());
						MapTile workspaceTile = gameContext.getAreaMap().getTile(workspaceLocation);
						if (workspaceTile != null && workspaceTile.isNavigable(parentEntity)) {
							liquidContainerAccessZone.add(workspaceTile, furnitureTile);
						}
					}

					if (liquidQuantity == 0) {
						liquidContainerAccessZone.setActive(false);
					}

					gameContext.getAreaMap().addZone(liquidContainerAccessZone);
				}
			}
		}

		updateParentMaterial();
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (liquidContainerAccessZone != null) {
			gameContext.getAreaMap().removeZone(liquidContainerAccessZone);
		}
		liquidContainerAccessZone = null;
		if (liquidQuantity > 0 && targetLiquidMaterial != null && messageDispatcher != null) {
			messageDispatcher.dispatchMessage(MessageType.LIQUID_AMOUNT_CHANGED, new LiquidAmountChangedMessage(parentEntity, targetLiquidMaterial, this.liquidQuantity, 0));
		}
		targetLiquidMaterial = null;
		liquidQuantity = 0;

		for (LiquidAllocation allocation : new HashSet<>(allocations)) {
			this.cancelAllocation(allocation);
		}

	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		LiquidContainerComponent cloned = new LiquidContainerComponent();
		cloned.messageDispatcher = messageDispatcher;
		cloned.parentEntity = this.parentEntity;
		cloned.targetLiquidMaterial = this.targetLiquidMaterial;
		cloned.liquidQuantity = this.liquidQuantity;
		cloned.maxLiquidCapacity = this.maxLiquidCapacity;
		if (messageDispatcher != null) {
			messageDispatcher.dispatchMessage(MessageType.LIQUID_AMOUNT_CHANGED, new LiquidAmountChangedMessage(parentEntity, targetLiquidMaterial, 0, this.liquidQuantity));
		}
		return cloned;
	}

	public GameMaterial getTargetLiquidMaterial() {
		return targetLiquidMaterial;
	}

	public void setTargetLiquidMaterial(GameMaterial targetLiquidMaterial) {
		GameMaterial oldMaterial = this.targetLiquidMaterial;
		if (targetLiquidMaterial == null) {
			if (parentEntity.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes) {
				((ItemEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes()).removeMaterial(GameMaterialType.LIQUID);
			} else if (parentEntity.getPhysicalEntityComponent().getAttributes() instanceof FurnitureEntityAttributes) {
				((FurnitureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes()).removeMaterial(GameMaterialType.LIQUID);
			}
		} else {
			if (liquidContainerAccessZone != null) {
				liquidContainerAccessZone.getClassification().setTargetMaterial(targetLiquidMaterial);
			}
		}
		this.targetLiquidMaterial = targetLiquidMaterial;

		if (messageDispatcher != null && !Objects.equals(oldMaterial, this.targetLiquidMaterial)) {
			messageDispatcher.dispatchMessage(MessageType.LIQUID_AMOUNT_CHANGED, new LiquidAmountChangedMessage(parentEntity, oldMaterial, this.liquidQuantity, 0));
			messageDispatcher.dispatchMessage(MessageType.LIQUID_AMOUNT_CHANGED, new LiquidAmountChangedMessage(parentEntity, this.targetLiquidMaterial, 0, this.liquidQuantity));
		}
	}

	public float getLiquidQuantity() {
		return liquidQuantity;
	}

	public void setLiquidQuantity(float liquidQuantity) {
		float oldLiquidQuantity = this.liquidQuantity;
		this.liquidQuantity = liquidQuantity;
		if (liquidQuantity <= SMALL_AMOUNT) {
			this.liquidQuantity = 0;
			if (liquidContainerAccessZone != null) {
				liquidContainerAccessZone.setActive(false);
			}
		} else {
			updateParentMaterial();
			if (liquidContainerAccessZone != null && !alwaysInactive) {
				if (liquidQuantity < usableLiquidAmount()) {
					liquidContainerAccessZone.setActive(false);
				} else {
					liquidContainerAccessZone.setActive(true);
				}
			}
		}
		if (parentEntity != null && messageDispatcher != null) {
			messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, parentEntity);
			messageDispatcher.dispatchMessage(MessageType.LIQUID_AMOUNT_CHANGED, new LiquidAmountChangedMessage(parentEntity, this.targetLiquidMaterial, oldLiquidQuantity, this.liquidQuantity));
		}
	}

	public float usableLiquidAmount() {
		return targetLiquidMaterial != null && targetLiquidMaterial.isQuenchesThirst() ? LocateDrinkAction.LIQUID_AMOUNT_FOR_DRINK_CONSUMPTION : 1;
	}

	public float getNumAllocated() {
		float totalAllocated = 0f;
		for (LiquidAllocation allocation : allocations) {
			totalAllocated += allocation.getAllocationAmount();
		}
		return totalAllocated;
	}

	public LiquidAllocation createAllocationFromInventory(float amountRequired, Entity requestingEntity) {
		if (getNumUnallocated() < amountRequired) {
			return null;
		} else {
			MapTile targetTile = gameContext.getAreaMap().getTile(requestingEntity.getLocationComponent().getWorldOrParentPosition());
			LiquidAllocation allocation = new LiquidAllocation(LiquidAllocation.LiquidAllocationType.REQUESTER_INVENTORY, new ZoneTile(targetTile, targetTile), amountRequired, targetLiquidMaterial);
			allocation.setTargetContainerId(parentEntity.getId());
			allocation.setRequesterEntityId(requestingEntity.getId());
			allocations.add(allocation);
			return allocation;
		}

	}

	public LiquidAllocation createAllocation(float amountRequired, Entity requestingEntity) {
		if (getNumUnallocated() < amountRequired) {
			return null;
		} else {
			ZoneTile zoneTile = LiquidMessageHandler.pickTileInZone(this.liquidContainerAccessZone, gameContext.getRandom(), gameContext.getAreaMap());
			if (zoneTile == null) {
				return null;
			}
			LiquidAllocation allocation = new LiquidAllocation(LiquidAllocation.LiquidAllocationType.FROM_LIQUID_CONTAINER, zoneTile, amountRequired, targetLiquidMaterial);
			allocation.setTargetContainerId(parentEntity.getId());
			allocation.setRequesterEntityId(requestingEntity.getId());
			allocations.add(allocation);
			return allocation;
		}
	}

	public LiquidAllocation createAllocationDueToParentHauling(float amountRequired, Entity requestingEntity) {
		if (getNumUnallocated() < amountRequired) {
			return null;
		} else {
			LiquidAllocation allocation = new LiquidAllocation(LiquidAllocation.LiquidAllocationType.PARENT_HAULING, new ZoneTile() /* null object */, amountRequired, targetLiquidMaterial);
			allocation.setTargetContainerId(parentEntity.getId());
			allocation.setRequesterEntityId(requestingEntity.getId());
			allocations.add(allocation);
			return allocation;
		}
	}

	public LiquidAllocation assignCraftingAllocation(float amountRequired) {
		if (getNumUnallocated() < amountRequired) {
			return null;
		} else {
			LiquidAllocation allocation = new LiquidAllocation(LiquidAllocation.LiquidAllocationType.CRAFTING_ASSIGNMENT, new ZoneTile() /* null object */, amountRequired, targetLiquidMaterial);
			allocation.setTargetContainerId(parentEntity.getId());
			allocation.setRequesterEntityId(parentEntity.getId());
			allocations.add(allocation);
			return allocation;
		}
	}

	public LiquidAllocation cancelAllocation(LiquidAllocation liquidAllocation) {
		if (liquidAllocation != null && allocations.contains(liquidAllocation)) {
			if (liquidAllocation.getState().equals(ItemAllocation.AllocationState.ACTIVE)) {
				allocations.remove(liquidAllocation);
				liquidAllocation.setState(ItemAllocation.AllocationState.CANCELLED);
				return liquidAllocation;
			} else {
				Logger.error("Cancelling LiquidAllocation with state " + liquidAllocation.getState());
			}
		} else {
			Logger.error("Attempting to cancel allocation at wrong container");
		}
		return null;
	}

	public LiquidAllocation cancelAllocationAndDecrementQuantity(LiquidAllocation liquidAllocation) {
		liquidAllocation = cancelAllocation(liquidAllocation);
		if (liquidAllocation != null) {
			setLiquidQuantity(liquidQuantity - liquidAllocation.getAllocationAmount());
			if (liquidQuantity < 0.1f) {
				messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, parentEntity);
			}
		}
		return liquidAllocation;
	}

	public void cancelAllAllocations() {
		for (LiquidAllocation allocation : new HashSet<>(allocations)) {
			this.cancelAllocation(allocation);
		}
	}

	private void updateParentMaterial() {
		if (targetLiquidMaterial != null && parentEntity != null) {
			if (parentEntity.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes) {
				ItemEntityAttributes attributes = (ItemEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes();
				GameMaterial oldPrimaryMaterial = attributes.getPrimaryMaterial();
				attributes.setMaterial(targetLiquidMaterial);

				if (!oldPrimaryMaterial.equals(attributes.getPrimaryMaterial())) {
					messageDispatcher.dispatchMessage(MessageType.ITEM_PRIMARY_MATERIAL_CHANGED, new ItemPrimaryMaterialChangedMessage(parentEntity, oldPrimaryMaterial));
				}
			} else if (parentEntity.getPhysicalEntityComponent().getAttributes() instanceof FurnitureEntityAttributes) {
				((FurnitureEntityAttributes) parentEntity.getPhysicalEntityComponent().getAttributes()).setMaterial(targetLiquidMaterial);
			}
		}
	}

	public void setMaxLiquidCapacity(int maxLiquidCapacity) {
		this.maxLiquidCapacity = maxLiquidCapacity;
	}

	public Integer getMaxLiquidCapacity() {
		return maxLiquidCapacity;
	}

	public boolean isAlwaysInactive() {
		return alwaysInactive;
	}

	public void setAlwaysInactive(boolean alwaysInactive) {
		this.alwaysInactive = alwaysInactive;
	}

	public Entity getParentEntity() {
		return parentEntity;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (targetLiquidMaterial != null) {
			asJson.put("targetLiquidMaterial", targetLiquidMaterial.getMaterialName());
		}

		if (liquidQuantity != 0) {
			asJson.put("quantity", liquidQuantity);
		}

		JSONArray allocationsArray = new JSONArray();
		for (LiquidAllocation allocation : allocations) {
			allocation.writeTo(savedGameStateHolder);
			allocationsArray.add(allocation.getLiquidAllocationId());
		}
		asJson.put("allocations", allocationsArray);

		if (maxLiquidCapacity != 0) {
			asJson.put("maxCapacity", maxLiquidCapacity);
		}

		if (liquidContainerAccessZone != null) {
			liquidContainerAccessZone.writeTo(savedGameStateHolder);
			asJson.put("accessZone", liquidContainerAccessZone.getZoneId());
		}

		if (alwaysInactive) {
			asJson.put("alwaysInactive", true);
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		String targetLiquidMaterialName = asJson.getString("targetLiquidMaterial");
		if (targetLiquidMaterialName != null) {
			this.targetLiquidMaterial = relatedStores.gameMaterialDictionary.getByName(targetLiquidMaterialName);
			if (this.targetLiquidMaterial == null) {
				throw new InvalidSaveException("Could not find material with name " + targetLiquidMaterialName);
			}
		}

		this.liquidQuantity = asJson.getIntValue("quantity");

		JSONArray allocationsArray = asJson.getJSONArray("allocations");
		if (allocationsArray != null) {
			for (int cursor = 0; cursor < allocationsArray.size(); cursor++) {
				long allocationId = allocationsArray.getLongValue(cursor);
				LiquidAllocation liquidAllocation = savedGameStateHolder.liquidAllocations.get(allocationId);
				if (liquidAllocation == null) {
					throw new InvalidSaveException("Could not find liquid allocation with ID " + allocationId);
				} else {
					this.allocations.add(liquidAllocation);
				}
			}
		}

		this.maxLiquidCapacity = asJson.getIntValue("maxCapacity");

		Long zoneId = asJson.getLong("accessZone");
		if (zoneId != null) {
			this.liquidContainerAccessZone = savedGameStateHolder.zones.get(zoneId);
			if (this.liquidContainerAccessZone == null) {
				throw new InvalidSaveException("Could not find zone with ID " + zoneId);
			}
		}

		this.alwaysInactive = asJson.getBooleanValue("alwaysInactive");
	}

	public float getNumUnallocated() {
		return liquidQuantity - getNumAllocated();
	}

	public boolean isEmpty() {
		return liquidQuantity <= 0;
	}

	public Zone getAccessZone() {
		return liquidContainerAccessZone;
	}
}
