package technology.rocketjump.saul.rooms.tags;

import technology.rocketjump.saul.entities.tags.Tag;
import technology.rocketjump.saul.entities.tags.TagProcessingUtils;
import technology.rocketjump.saul.rooms.Room;
import technology.rocketjump.saul.rooms.components.behaviour.GraveyardBehaviour;

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
