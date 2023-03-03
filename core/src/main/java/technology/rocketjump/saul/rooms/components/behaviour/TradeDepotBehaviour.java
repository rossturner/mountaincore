package technology.rocketjump.saul.rooms.components.behaviour;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.rooms.Room;
import technology.rocketjump.saul.rooms.components.RoomComponent;

public class TradeDepotBehaviour extends RoomBehaviourComponent {


	public TradeDepotBehaviour(Room parent, MessageDispatcher messageDispatcher) {
		super(parent, messageDispatcher);
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
	}

	@Override
	public RoomComponent clone(Room newParent) {
		TradeDepotBehaviour cloned = new TradeDepotBehaviour(newParent, this.messageDispatcher);
		return cloned;
	}

	@Override
	public void mergeFrom(RoomComponent otherComponent) {
		TradeDepotBehaviour other = (TradeDepotBehaviour) otherComponent;
		// TODO merge any state info together
	}

	@Override
	public void infrequentUpdate(GameContext gameContext, MessageDispatcher messageDispatcher) {
	}

	@Override
	public void tileRemoved(GridPoint2 location) {
		// Don't need to do anything, list of furniture entities is updated every cycle
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
