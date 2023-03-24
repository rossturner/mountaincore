package technology.rocketjump.mountaincore.rooms.components.behaviour;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rooms.Room;
import technology.rocketjump.mountaincore.rooms.components.RoomComponent;

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
