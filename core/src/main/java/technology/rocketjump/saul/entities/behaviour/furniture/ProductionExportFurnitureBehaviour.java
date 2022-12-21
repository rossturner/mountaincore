package technology.rocketjump.saul.entities.behaviour.furniture;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.components.ItemAllocation;
import technology.rocketjump.saul.entities.components.ItemAllocationComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.model.JobPriority;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.RequestHaulingMessage;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.rendering.utils.HexColors;
import technology.rocketjump.saul.settlement.production.CraftingAssignment;

import java.util.ArrayList;
import java.util.List;

public class ProductionExportFurnitureBehaviour extends FurnitureBehaviour implements Prioritisable, DisplayGhostItemWhenInventoryEmpty {

	private int maxNumItemStacks = 0;
	private ItemType selectedItemType;
	private GameMaterial selectedMaterial; // null == ANY

	private List<CraftingAssignment> pendingAssignments = new ArrayList<>();

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
