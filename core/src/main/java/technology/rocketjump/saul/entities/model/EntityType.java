package technology.rocketjump.saul.entities.model;

import technology.rocketjump.saul.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.saul.assets.entities.furniture.model.FurnitureEntityAsset;
import technology.rocketjump.saul.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.saul.assets.entities.mechanism.model.MechanismEntityAsset;
import technology.rocketjump.saul.assets.entities.model.EntityAsset;
import technology.rocketjump.saul.assets.entities.plant.model.PlantEntityAsset;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.entities.model.physical.effect.OngoingEffectType;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.mechanism.MechanismType;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpecies;

import java.util.List;

public enum EntityType {

	CREATURE("race.json", Race.class, CreatureEntityAsset.class, Race.class),
	PLANT("plantSpecies.json", PlantSpecies.class, PlantEntityAsset.class, PlantSpecies.class),
	ITEM("itemType.json", ItemType.class, ItemEntityAsset.class, ItemType.class),
	FURNITURE("furnitureType.json", FurnitureType.class, FurnitureEntityAsset.class, FurnitureType.class),
	ONGOING_EFFECT("effectType.json", OngoingEffectType.class, null, OngoingEffectType.class),
	MECHANISM("mechanismType.json", MechanismType.class, MechanismEntityAsset.class, MechanismType.class);

	public static final List<EntityType> STATIC_ENTITY_TYPES = List.of(PLANT, ITEM, FURNITURE);
	public final String descriptorFilename;
	public final Class<?> typeDescriptorClass;
	public final Class<? extends EntityAsset> entityAssetClass;
	public final Class<?> descriptorClass;

	EntityType(String descriptorFilename, Class<?> typeDescriptorClass, Class<? extends EntityAsset> entityAssetClass, Class<?> descriptorClass) {
		this.descriptorFilename = descriptorFilename;
		this.typeDescriptorClass = typeDescriptorClass;
		this.entityAssetClass = entityAssetClass;
		this.descriptorClass = descriptorClass;
	}

}
