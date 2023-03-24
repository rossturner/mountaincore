package technology.rocketjump.mountaincore.rooms.tags;

import technology.rocketjump.mountaincore.entities.tags.Tag;
import technology.rocketjump.mountaincore.entities.tags.TagProcessingUtils;
import technology.rocketjump.mountaincore.rooms.Room;
import technology.rocketjump.mountaincore.rooms.components.behaviour.GraveyardBehaviour;

public class GraveyardBehaviourTag extends Tag {
	@Override
	public String getTagName() {
		return "GRAVEYARD_BEHAVIOUR";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return true;
	}

	@Override
	public void apply(Room room, TagProcessingUtils tagProcessingUtils) {
		if (room.getBehaviourComponent() instanceof GraveyardBehaviour) {
			return;
		}

		GraveyardBehaviour behaviourComponent = room.createComponent(GraveyardBehaviour.class, tagProcessingUtils.messageDispatcher);

		behaviourComponent.setJobStore(tagProcessingUtils.jobStore);
		behaviourComponent.setFurnitureTypeDictionary(tagProcessingUtils.furnitureTypeDictionary);
		behaviourComponent.setJobTypes(
				tagProcessingUtils.jobTypeDictionary.getByName("DIGGING"),
				tagProcessingUtils.jobTypeDictionary.getByName("FILL_GRAVE")
		); // MODDING data-drive this
	}
}
