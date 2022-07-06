package technology.rocketjump.saul.assets.entities;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.mechanism.MechanismTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesDictionary;

@Singleton
public class CompleteEntityTypeDictionary {

	private final RaceDictionary raceDictionary;
	private final PlantSpeciesDictionary plantSpeciesDictionary;
	private final ItemTypeDictionary itemTypeDictionary;
	private final FurnitureTypeDictionary furnitureTypeDictionary;
	private final MechanismTypeDictionary mechanismTypeDictionary;

	@Inject
	public CompleteEntityTypeDictionary(RaceDictionary raceDictionary, PlantSpeciesDictionary plantSpeciesDictionary,
										ItemTypeDictionary itemTypeDictionary, FurnitureTypeDictionary furnitureTypeDictionary,
										MechanismTypeDictionary mechanismTypeDictionary) {
		this.raceDictionary = raceDictionary;
		this.plantSpeciesDictionary = plantSpeciesDictionary;
		this.itemTypeDictionary = itemTypeDictionary;
		this.furnitureTypeDictionary = furnitureTypeDictionary;
		this.mechanismTypeDictionary = mechanismTypeDictionary;
	}

	public Object get(EntityType entityType, String name) {
		switch (entityType) {
			case CREATURE:
				return raceDictionary.getByName(name);
			case PLANT:
				return plantSpeciesDictionary.getByName(name);
			case ITEM:
				return itemTypeDictionary.getByName(name);
			case FURNITURE:
				return furnitureTypeDictionary.getByName(name);
			case MECHANISM:
				return mechanismTypeDictionary.getByName(name);
			default:
				Logger.error("Not yet implemented: " + entityType.name() + " in " + getClass().getSimpleName());
				return null;
		}
	}

}
