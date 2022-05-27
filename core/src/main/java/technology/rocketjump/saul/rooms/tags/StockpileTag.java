package technology.rocketjump.saul.rooms.tags;

import technology.rocketjump.saul.entities.tags.Tag;
import technology.rocketjump.saul.entities.tags.TagProcessingUtils;
import technology.rocketjump.saul.rooms.Room;
import technology.rocketjump.saul.rooms.components.StockpileComponent;

public class StockpileTag extends Tag {
	@Override
	public String getTagName() {
		return "STOCKPILE";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return args.size() == 0;
	}

	@Override
	public void apply(Room room, TagProcessingUtils tagProcessingUtils) {
		room.createComponent(StockpileComponent.class, tagProcessingUtils.messageDispatcher);
	}

}
