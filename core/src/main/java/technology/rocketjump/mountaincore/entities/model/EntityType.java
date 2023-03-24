package technology.rocketjump.mountaincore.entities.model;

import technology.rocketjump.mountaincore.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.furniture.model.FurnitureEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.mechanism.model.MechanismEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAsset;
import technology.rocketjump.mountaincore.assets.entities.plant.model.PlantEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.vehicle.model.VehicleEntityAsset;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Race;
import technology.rocketjump.mountaincore.entities.model.physical.effect.OngoingEffectType;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.entities.model.physical.mechanism.MechanismType;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.mountaincore.entities.model.physical.vehicle.VehicleType;

import java.util.List;

public enum EntityType {

	CREATURE("race.json", Race.class, CreatureEntityAsset.class),
	PLANT("plantSpecies.json", PlantSpecies.class, PlantEntityAsset.class),
	ITEM("itemType.json", ItemType.class, ItemEntityAsset.class),
	FURNITURE("furnitureType.json", FurnitureType.class, FurnitureEntityAsset.class),
	VEHICLE("vehicleType.json", VehicleType.class, VehicleEntityAsset.class),
	ONGOING_EFFECT("effectType.json", OngoingEffectType.class, null),
	MECHANISM("mechanismType.json", MechanismType.class, MechanismEntityAsset.class);

	public static final List<EntityType> STATIC_ENTITY_TYPES = List.of(PLANT, ITEM, FURNITURE);
	public final String descriptorFilename;
	public final Class<?> typeDescriptorClass;
	public final Class<? extends EntityAsset> entityAssetClass;

	EntityType(String descriptorFilename, Class<?> typeDescriptorClass, Class<? extends EntityAsset> entityAssetClass) {
		this.descriptorFilename = descriptorFilename;
		this.typeDescriptorClass = typeDescriptorClass;
		this.entityAssetClass = entityAssetClass;
	}

}
