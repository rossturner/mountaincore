package technology.rocketjump.saul.entities.behaviour.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

public class TraderCreatureGroup extends CreatureGroup {


	@Override
	public void infrequentUpdate(GameContext gameContext, MessageDispatcher messageDispatcher) {
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(savedGameStateHolder);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);
	}
}
