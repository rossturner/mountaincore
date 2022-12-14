package technology.rocketjump.saul.entities.behaviour.furniture;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.model.JobPriority;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.rendering.utils.HexColors;

public class ProductionExportFurnitureBehaviour extends FurnitureBehaviour implements Prioritisable, DisplayGhostItemWhenInventoryEmpty {

	private int maxNumItemStacks = 0;
	private ItemType selectedItemType;
	private GameMaterial selectedMaterial; // null == ANY

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
