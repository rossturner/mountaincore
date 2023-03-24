package technology.rocketjump.mountaincore.rooms.tags;

import technology.rocketjump.mountaincore.entities.tags.Tag;
import technology.rocketjump.mountaincore.entities.tags.TagProcessingUtils;
import technology.rocketjump.mountaincore.rooms.Room;
import technology.rocketjump.mountaincore.rooms.components.behaviour.TradeDepotBehaviour;

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
