package technology.rocketjump.saul.rooms.tags;

import technology.rocketjump.saul.entities.tags.Tag;
import technology.rocketjump.saul.entities.tags.TagProcessingUtils;
import technology.rocketjump.saul.rooms.Room;
import technology.rocketjump.saul.rooms.components.behaviour.TradeDepotBehaviour;

public class TradeDepotBehaviourTag extends Tag {
	@Override
	public String getTagName() {
		return "TRADE_DEPOT_BEHAVIOUR";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return true;
	}

	@Override
	public void apply(Room room, TagProcessingUtils tagProcessingUtils) {
		TradeDepotBehaviour behaviourComponent = room.createComponent(TradeDepotBehaviour.class, tagProcessingUtils.messageDispatcher);
	}
}
