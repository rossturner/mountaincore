package technology.rocketjump.saul.assets.entities;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.assets.entities.creature.CreatureEntityAssetDictionary;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.saul.assets.entities.furniture.FurnitureEntityAssetDictionary;
import technology.rocketjump.saul.assets.entities.furniture.model.FurnitureEntityAsset;
import technology.rocketjump.saul.assets.entities.item.ItemEntityAssetDictionary;
import technology.rocketjump.saul.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.saul.assets.entities.mechanism.MechanismEntityAssetDictionary;
import technology.rocketjump.saul.assets.entities.model.*;
import technology.rocketjump.saul.assets.entities.plant.PlantEntityAssetDictionary;
import technology.rocketjump.saul.assets.entities.plant.model.PlantEntityAsset;
import technology.rocketjump.saul.assets.entities.vehicle.VehicleEntityAssetDictionary;
import technology.rocketjump.saul.assets.entities.vehicle.model.VehicleEntityAsset;
import technology.rocketjump.saul.assets.entities.wallcap.WallCapAssetDictionary;

import java.util.HashMap;
import java.util.Map;

import static technology.rocketjump.saul.assets.entities.model.NullEntityAsset.NULL_ASSET;

@Singleton
public class CompleteAssetDictionary {

	private final Map<String, EntityAsset> allAssetsByName = new HashMap<>();
	private final CreatureEntityAssetDictionary creatureEntityAssetDictionary;
	private final FurnitureEntityAssetDictionary furnitureEntityAssetDictionary;
	private final VehicleEntityAssetDictionary vehicleEntityAssetDictionary;
	private final PlantEntityAssetDictionary plantEntityAssetDictionary;
	private final ItemEntityAssetDictionary itemEntityAssetDictionary;
	private final WallCapAssetDictionary wallCapAssetDictionary;
	private final MechanismEntityAssetDictionary mechanismEntityAssetDictionary;
	private final AnimationDictionary animationDictionary;

	@Inject
	public CompleteAssetDictionary(CreatureEntityAssetDictionary creatureEntityAssetDictionary, FurnitureEntityAssetDictionary furnitureEntityAssetDictionary,
								   VehicleEntityAssetDictionary vehicleEntityAssetDictionary, PlantEntityAssetDictionary plantEntityAssetDictionary, ItemEntityAssetDictionary itemEntityAssetDictionary,
								   WallCapAssetDictionary wallCapAssetDictionary, MechanismEntityAssetDictionary mechanismEntityAssetDictionary, AnimationDictionary animationDictionary) {
		this.creatureEntityAssetDictionary = creatureEntityAssetDictionary;
		this.furnitureEntityAssetDictionary = furnitureEntityAssetDictionary;
		this.vehicleEntityAssetDictionary = vehicleEntityAssetDictionary;
		this.plantEntityAssetDictionary = plantEntityAssetDictionary;
		this.itemEntityAssetDictionary = itemEntityAssetDictionary;
		this.wallCapAssetDictionary = wallCapAssetDictionary;
		this.mechanismEntityAssetDictionary = mechanismEntityAssetDictionary;
		this.animationDictionary = animationDictionary;

		rebuild();
	}

	public void rebuild() {
		allAssetsByName.clear();
		allAssetsByName.putAll(creatureEntityAssetDictionary.getAll());
		allAssetsByName.putAll(plantEntityAssetDictionary.getAll());
		allAssetsByName.putAll(itemEntityAssetDictionary.getAll());
		allAssetsByName.putAll(furnitureEntityAssetDictionary.getAll());
		allAssetsByName.putAll(vehicleEntityAssetDictionary.getAll());
		allAssetsByName.putAll(wallCapAssetDictionary.getAll());
		allAssetsByName.putAll(mechanismEntityAssetDictionary.getAll());
		allAssetsByName.put(NULL_ASSET.getUniqueName(), NULL_ASSET);
		creatureEntityAssetDictionary.rebuild();
		itemEntityAssetDictionary.rebuild();
		furnitureEntityAssetDictionary.rebuild();
		plantEntityAssetDictionary.rebuild();

		populateAnimationsFromTemplates();
	}

	public void add(CreatureEntityAsset asset) {
		creatureEntityAssetDictionary.add(asset);
		rebuild();
	}

	public void add(ItemEntityAsset itemEntityAsset) {
		itemEntityAssetDictionary.add(itemEntityAsset);
		rebuild();
	}

	public void add(FurnitureEntityAsset furnitureEntityAsset) {
		furnitureEntityAssetDictionary.add(furnitureEntityAsset);
		rebuild();
	}

	public void add(VehicleEntityAsset vehicleEntityAsset) {
		vehicleEntityAssetDictionary.add(vehicleEntityAsset);
		rebuild();
	}

	public void add(PlantEntityAsset asset) {
		plantEntityAssetDictionary.add(asset);
		rebuild();
	}

	public EntityAsset getByUniqueName(String uniqueAssetName) {
		return allAssetsByName.get(uniqueAssetName);
	}


	private void populateAnimationsFromTemplates() {
		for (EntityAsset entityAsset : allAssetsByName.values()) {
			if (entityAsset.getSpriteDescriptors() != null) {
				for (SpriteDescriptor spriteDescriptor : entityAsset.getSpriteDescriptors().values()) {
					Map<String, TemplateAnimationScript.Variables> templates = spriteDescriptor.getTemplatedAnimationScripts();
					for (String animationName : templates.keySet()) {
						TemplateAnimationScript.Variables templateVariables = templates.get(animationName);
						AnimationScript templateInstance = animationDictionary.newInstance(templateVariables);
						spriteDescriptor.getInheritedAnimationScripts().put(animationName, templateInstance);
					}
				}
			}
		}
	}
}
