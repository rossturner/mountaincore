package technology.rocketjump.saul.entities.behaviour.furniture;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.components.ItemAllocation;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.rendering.utils.HexColors;

public class TradingExportFurnitureBehaviour extends ProductionImportFurnitureBehaviour {

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		super.init(parentEntity, messageDispatcher, gameContext);
		parentEntity.getOrCreateComponent(InventoryComponent.class).setAddAsAllocationPurpose(ItemAllocation.Purpose.TRADING_EXPORT);
	}

	@Override
	public FurnitureBehaviour clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		TradingExportFurnitureBehaviour cloned = new TradingExportFurnitureBehaviour();
		cloned.maxNumItemStacks = this.maxNumItemStacks;
		cloned.selectedItemType = this.selectedItemType;
		cloned.selectedMaterial = this.selectedMaterial;

		cloned.incomingHaulingJobs.addAll(this.incomingHaulingJobs);
		cloned.haulingJobType = this.haulingJobType;

		return cloned;
	}

	@Override
	public Color getOverrideColor() {
		if (incomingHaulingJobs.isEmpty()) {
			return HexColors.GHOST_NEGATIVE_COLOR_MORE_OPAQUE;
		} else {
			return HexColors.GHOST_PLAIN_COLOR;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);
	}
}
