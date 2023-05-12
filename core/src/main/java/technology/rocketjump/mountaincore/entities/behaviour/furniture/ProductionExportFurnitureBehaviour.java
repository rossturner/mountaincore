package technology.rocketjump.mountaincore.entities.behaviour.furniture;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.components.ItemAllocation;
import technology.rocketjump.mountaincore.entities.components.ItemAllocationComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.jobs.model.JobPriority;
import technology.rocketjump.mountaincore.jobs.model.JobState;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.RequestHaulingMessage;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rendering.utils.HexColors;
import technology.rocketjump.mountaincore.settlement.production.CraftingAssignment;

import java.util.ArrayList;
import java.util.List;

public class ProductionExportFurnitureBehaviour extends FurnitureBehaviour implements Prioritisable, DisplayGhostItemWhenInventoryEmpty {

	protected int maxNumItemStacks = 0;
	protected ItemType selectedItemType;
	protected GameMaterial selectedMaterial; // null == ANY

	protected List<CraftingAssignment> pendingAssignments = new ArrayList<>();

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		super.init(parentEntity, messageDispatcher, gameContext);
		parentEntity.getOrCreateComponent(InventoryComponent.class).setAddAsAllocationPurpose(null);
	}

	@Override
	public FurnitureBehaviour clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		ProductionExportFurnitureBehaviour cloned = new ProductionExportFurnitureBehaviour();
		cloned.maxNumItemStacks = this.maxNumItemStacks;
		cloned.selectedItemType = this.selectedItemType;
		cloned.selectedMaterial = this.selectedMaterial;
		cloned.pendingAssignments.addAll(this.pendingAssignments);
		return cloned;
	}

	@Override
	public void setPriority(JobPriority jobPriority) {
		super.setPriority(jobPriority);
	}

	public void setMaxNumItemStacks(int maxNumItemStacks) {
		this.maxNumItemStacks = maxNumItemStacks;
	}

	public void setSelectedItemType(ItemType selectedItemType) {
		this.selectedItemType = selectedItemType;
//		cancelIncomingHaulingJobs();
	}

	public void setSelectedMaterial(GameMaterial selectedMaterial) {
		if (selectedMaterial == GameMaterial.NULL_MATERIAL) {
			selectedMaterial = null;
		}
		this.selectedMaterial = selectedMaterial;
		if (selectedMaterial != null) {
//			cancelIncomingHaulingJobs();
		}
	}

	@Override
	public ItemType getSelectedItemType() {
		return selectedItemType;
	}

	@Override
	public Color getOverrideColor() {
//		if (incomingHaulingJobs.isEmpty()) {
//			return HexColors.GHOST_NEGATIVE_COLOR_MORE_OPAQUE;
//		} else {
		return HexColors.GHOST_PLAIN_COLOR;
//		}
	}

	public GameMaterial getSelectedMaterial() {
		return selectedMaterial;
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		super.infrequentUpdate(gameContext);

		if (parentEntity.isOnFire()) {
//			cancelIncomingHaulingJobs();
			return;
		}
		pendingAssignments.removeIf(p -> p.getCraftingJob().getJobState().equals(JobState.REMOVED));

		InventoryComponent inventoryComponent = parentEntity.getOrCreateComponent(InventoryComponent.class);

		List<Entity> unwantedInventoryItems = getInventoryItemsNotMatchingSelection(inventoryComponent);
		if (unwantedInventoryItems.size() > 0) {
			for (Entity inventoryItem : unwantedInventoryItems) {
				ItemAllocationComponent allocationComponent = inventoryItem.getComponent(ItemAllocationComponent.class);
				allocationComponent.cancelAll(ItemAllocation.Purpose.PRODUCTION_IMPORT);

				if (allocationComponent.getNumUnallocated() > 0) {
					messageDispatcher.dispatchMessage(MessageType.REQUEST_ENTITY_HAULING, new RequestHaulingMessage(
							inventoryItem, parentEntity, true, priority, null)
					);
				}
			}
		}
	}

	private List<Entity> getInventoryItemsNotMatchingSelection(InventoryComponent inventoryComponent) {
		if (selectedItemType == null) {
			return inventoryComponent.getInventoryEntries().stream().map(e -> e.entity).toList();
		}
		return inventoryComponent.getInventoryEntries().stream()
				.map(e -> e.entity)
				.filter(e -> e.getType().equals(EntityType.ITEM))
				.filter(e -> !matchesCurrentSelection((ItemEntityAttributes) e.getPhysicalEntityComponent().getAttributes()))
				.toList();
	}

	private boolean matchesCurrentSelection(ItemEntityAttributes attributes) {
		return attributes.getItemType().equals(selectedItemType) && (selectedMaterial == null || attributes.getPrimaryMaterial().equals(selectedMaterial));
	}

	public List<CraftingAssignment> getPendingAssignments() {
		return pendingAssignments;
	}

	public Entity getParentEntity() {
		return parentEntity;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);

		asJson.put("maxNumItemStacks", maxNumItemStacks);
		asJson.put("selectedItemType", selectedItemType != null ? selectedItemType.getItemTypeName() : null);
		asJson.put("selectedMaterial", selectedMaterial != null ? selectedMaterial.getMaterialName() : null);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		this.maxNumItemStacks = asJson.getIntValue("maxNumItemStacks");
		String itemTypeName = asJson.getString("selectedItemType");
		if (itemTypeName != null) {
			selectedItemType = relatedStores.itemTypeDictionary.getByName(itemTypeName);
			if (selectedItemType == null) {
				throw new InvalidSaveException("Could not find item type " + itemTypeName);
			}
		}
		String materialName = asJson.getString("selectedMaterial");
		if (materialName != null) {
			selectedMaterial = relatedStores.gameMaterialDictionary.getByName(materialName);
			if (selectedMaterial == null) {
				throw new InvalidSaveException("Could not find material " + materialName);
			}
		}
	}
}
