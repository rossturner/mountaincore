package technology.rocketjump.saul.entities.behaviour.furniture;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

public class TradingImportFurnitureBehaviour extends ProductionExportFurnitureBehaviour {

	@Override
	public FurnitureBehaviour clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		TradingImportFurnitureBehaviour cloned = new TradingImportFurnitureBehaviour();
		cloned.maxNumItemStacks = this.maxNumItemStacks;
		cloned.selectedItemType = this.selectedItemType;
		cloned.selectedMaterial = this.selectedMaterial;
		cloned.pendingAssignments.addAll(this.pendingAssignments);
		return cloned;
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
