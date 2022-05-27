package technology.rocketjump.saul.rooms.tags;

import technology.rocketjump.saul.entities.tags.Tag;
import technology.rocketjump.saul.entities.tags.TagProcessingUtils;
import technology.rocketjump.saul.rooms.Room;
import technology.rocketjump.saul.rooms.components.behaviour.MushroomFarmBehaviour;

public class MushroomFarmBehaviourTag extends Tag {
	@Override
	public String getTagName() {
		return "MUSHROOM_FARM_BEHAVIOUR";
	}

	@Override
	public boolean isValid(TagProcessingUtils tagProcessingUtils) {
		return true;
	}

	@Override
	public void apply(Room room, TagProcessingUtils tagProcessingUtils) {
		MushroomFarmBehaviour behaviourComponent = room.createComponent(MushroomFarmBehaviour.class, tagProcessingUtils.messageDispatcher);
		behaviourComponent.setMushroomFarmingProfession(tagProcessingUtils.professionDictionary.getByName("FARMER"));
		behaviourComponent.setFurnitureTypes(
				tagProcessingUtils.furnitureTypeDictionary.getByName("INNOCULATED_LOG"),
				tagProcessingUtils.furnitureTypeDictionary.getByName("SHOCKED_LOG")
		); // MODDING data-drive this
		behaviourComponent.setJobTypes( // MODDING data-drive this
				tagProcessingUtils.jobTypeDictionary.getByName("HAULING")
		);
	}
}
