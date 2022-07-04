package technology.rocketjump.saul.entities.model;

import java.util.List;

public enum EntityType {

	CREATURE("race.json"),
	PLANT("plantSpecies.json"),
	ITEM("itemType.json"),
	FURNITURE("furnitureType.json"),
	ONGOING_EFFECT("effectType.json"),
	MECHANISM("mechanismType.json");

	public static final List<EntityType> STATIC_ENTITY_TYPES = List.of(PLANT, ITEM, FURNITURE);
	public final String descriptorFilename;

	EntityType(String descriptorFilename) {
		this.descriptorFilename = descriptorFilename;
	}

}
