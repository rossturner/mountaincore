package technology.rocketjump.mountaincore.entities;

import com.badlogic.gdx.graphics.Color;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.mountaincore.assets.entities.creature.CreatureEntityAssetDictionary;
import technology.rocketjump.mountaincore.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.furniture.FurnitureEntityAssetDictionary;
import technology.rocketjump.mountaincore.assets.entities.furniture.model.FurnitureEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.item.ItemEntityAssetDictionary;
import technology.rocketjump.mountaincore.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.mechanism.MechanismEntityAssetDictionary;
import technology.rocketjump.mountaincore.assets.entities.mechanism.model.MechanismEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.model.*;
import technology.rocketjump.mountaincore.assets.entities.plant.PlantEntityAssetDictionary;
import technology.rocketjump.mountaincore.assets.entities.plant.model.PlantEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.vehicle.VehicleEntityAssetDictionary;
import technology.rocketjump.mountaincore.assets.entities.vehicle.model.VehicleEntityAsset;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.components.LiquidContainerComponent;
import technology.rocketjump.mountaincore.entities.components.creature.MilitaryComponent;
import technology.rocketjump.mountaincore.entities.components.creature.SkillsComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.EntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.mountaincore.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.mountaincore.entities.model.physical.effect.OngoingEffectAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.DoorwayEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.mechanism.MechanismEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantSpeciesGrowthStage;
import technology.rocketjump.mountaincore.entities.model.physical.vehicle.VehicleEntityAttributes;
import technology.rocketjump.mountaincore.entities.tags.AssetOverrideBySkillTag;
import technology.rocketjump.mountaincore.entities.tags.Tag;
import technology.rocketjump.mountaincore.entities.tags.TagProcessor;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.GameContextAware;
import technology.rocketjump.mountaincore.jobs.SkillDictionary;
import technology.rocketjump.mountaincore.jobs.model.Skill;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;

import java.util.*;
import java.util.function.Function;

import static technology.rocketjump.mountaincore.jobs.SkillDictionary.NULL_PROFESSION;

@Singleton
public class EntityAssetUpdater implements GameContextAware {


	private final CreatureEntityAssetDictionary creatureEntityAssetDictionary;
	private final ItemEntityAssetDictionary itemEntityAssetDictionary;
	private final FurnitureEntityAssetDictionary furnitureEntityAssetDictionary;
	private final VehicleEntityAssetDictionary vehicleEntityAssetDictionary;
	private final PlantEntityAssetDictionary plantEntityAssetDictionary;
	private final MechanismEntityAssetDictionary mechanismEntityAssetDictionary;
	private final TagProcessor tagProcessor;

	public final EntityAssetType branchAssetType;
	public final EntityAssetType leafAssetType;
	public final EntityAssetType fruitAssetType;
	public final EntityAssetType ITEM_BASE_LAYER;
	public final EntityAssetType ITEM_LIQUID_LAYER;
	public final EntityAssetType ITEM_COVER_LAYER;
	public final EntityAssetType FURNITURE_BASE_LAYER;
	public final EntityAssetType CREATURE_BODY;
	public final EntityAssetType BODY_OUTLINE;
	public final EntityAssetType CLOTHING_OUTLINE;
	public final EntityAssetType CREATURE_HAIR;
	public final EntityAssetType HAIR_OUTLINE;
	public final EntityAssetType CREATURE_EYEBROWS;
	public final EntityAssetType CREATURE_BEARD;
	public final EntityAssetType CREATURE_LEFT_HAND;
	public final EntityAssetType CREATURE_RIGHT_HAND;
	public final EntityAssetType CREATURE_HEAD;
	public final EntityAssetType FURNITURE_LIQUID_LAYER;
	public final EntityAssetType FURNITURE_COVER_LAYER;
	public final EntityAssetType MECHANISM_BASE_LAYER;
	public final EntityAssetType SHOW_WHEN_INVENTORY_PRESENT;
	public final EntityAssetType SHOW_WHEN_INVENTORY_PRESENT_2;
	public final EntityAssetType SHOW_WHEN_INVENTORY_PRESENT_3;
	public final EntityAssetType VEHICLE_SHOW_WHEN_INVENTORY_PRESENT;
	public final EntityAssetType VEHICLE_BASE_LAYER;
	private final SkillDictionary skillDictionary;
	private final List<EntityAssetType> SHOW_WHEN_INVENTORY_TYPES;
	private GameContext gameContext;
	private Skill UNARMORED_MILITARY_OVERRIDE;

	@Inject
	public EntityAssetUpdater(ItemEntityAssetDictionary itemEntityAssetDictionary, FurnitureEntityAssetDictionary furnitureEntityAssetDictionary,
							  VehicleEntityAssetDictionary vehicleEntityAssetDictionary, PlantEntityAssetDictionary plantEntityAssetDictionary, MechanismEntityAssetDictionary mechanismEntityAssetDictionary,
							  EntityAssetTypeDictionary entityAssetTypeDictionary,
							  SkillDictionary skillDictionary, CreatureEntityAssetDictionary creatureEntityAssetDictionary,
							  TagProcessor tagProcessor, SkillDictionary skillDictionary1) {
		this.itemEntityAssetDictionary = itemEntityAssetDictionary;
		this.vehicleEntityAssetDictionary = vehicleEntityAssetDictionary;
		this.plantEntityAssetDictionary = plantEntityAssetDictionary;
		this.furnitureEntityAssetDictionary = furnitureEntityAssetDictionary;
		this.mechanismEntityAssetDictionary = mechanismEntityAssetDictionary;

		CREATURE_BODY = entityAssetTypeDictionary.getByName("CREATURE_BODY");

		branchAssetType = entityAssetTypeDictionary.getByName("PLANT_BRANCHES");
		leafAssetType = entityAssetTypeDictionary.getByName("PLANT_LEAVES");
		fruitAssetType = entityAssetTypeDictionary.getByName("PLANT_FRUIT");

		ITEM_BASE_LAYER = entityAssetTypeDictionary.getByName("ITEM_BASE_LAYER");
		ITEM_LIQUID_LAYER = entityAssetTypeDictionary.getByName("ITEM_LIQUID_LAYER");
		ITEM_COVER_LAYER = entityAssetTypeDictionary.getByName("ITEM_COVER_LAYER");

		FURNITURE_BASE_LAYER = entityAssetTypeDictionary.getByName("BASE_LAYER");
		FURNITURE_LIQUID_LAYER = entityAssetTypeDictionary.getByName("FURNITURE_LIQUID_LAYER");
		FURNITURE_COVER_LAYER = entityAssetTypeDictionary.getByName("FURNITURE_COVER_LAYER");

		CREATURE_HEAD = entityAssetTypeDictionary.getByName("CREATURE_HEAD");
		CREATURE_HAIR = entityAssetTypeDictionary.getByName("CREATURE_HAIR");
		BODY_OUTLINE = entityAssetTypeDictionary.getByName("BODY_OUTLINE");
		CLOTHING_OUTLINE = entityAssetTypeDictionary.getByName("CLOTHING_OUTLINE");
		HAIR_OUTLINE = entityAssetTypeDictionary.getByName("HAIR_OUTLINE");
		CREATURE_EYEBROWS = entityAssetTypeDictionary.getByName("CREATURE_EYEBROWS");
		CREATURE_BEARD = entityAssetTypeDictionary.getByName("CREATURE_BEARD");

		CREATURE_LEFT_HAND = entityAssetTypeDictionary.getByName("CREATURE_LEFT_HAND");
		CREATURE_RIGHT_HAND = entityAssetTypeDictionary.getByName("CREATURE_RIGHT_HAND");

		MECHANISM_BASE_LAYER = entityAssetTypeDictionary.getByName("MECHANISM_BASE_LAYER");
		SHOW_WHEN_INVENTORY_PRESENT = entityAssetTypeDictionary.getByName("SHOW_WHEN_INVENTORY_PRESENT");
		SHOW_WHEN_INVENTORY_PRESENT_2 = entityAssetTypeDictionary.getByName("SHOW_WHEN_INVENTORY_PRESENT_2");
		SHOW_WHEN_INVENTORY_PRESENT_3 = entityAssetTypeDictionary.getByName("SHOW_WHEN_INVENTORY_PRESENT_3");
		VEHICLE_SHOW_WHEN_INVENTORY_PRESENT = entityAssetTypeDictionary.getByName("VEHICLE_SHOW_WHEN_INVENTORY_PRESENT");

		VEHICLE_BASE_LAYER = entityAssetTypeDictionary.getByName("VEHICLE_BASE_LAYER");

		UNARMORED_MILITARY_OVERRIDE = skillDictionary.getByName("UNARMORED_MILITARY");


		SHOW_WHEN_INVENTORY_TYPES = Arrays.asList(
				SHOW_WHEN_INVENTORY_PRESENT, SHOW_WHEN_INVENTORY_PRESENT_2, SHOW_WHEN_INVENTORY_PRESENT_3,
				VEHICLE_SHOW_WHEN_INVENTORY_PRESENT
		);

		this.creatureEntityAssetDictionary = creatureEntityAssetDictionary;
		this.tagProcessor = tagProcessor;
		this.skillDictionary = skillDictionary1;
	}

	public void updateEntityAssets(Entity entity) {
		switch (entity.getType()) {
			case CREATURE:
				updateCreatureAssets(entity);
				break;
			case ITEM:
				updateItemAssets(entity);
				break;
			case FURNITURE:
				updateFurnitureAssets(entity);
				break;
			case VEHICLE:
				updateVehicleAssets(entity);
				break;
			case PLANT:
				updatePlantAssets(entity);
				break;
			case MECHANISM:
				updateMechanismAssets(entity);
				break;
			case ONGOING_EFFECT:
				processTags(entity);
				break;
			default:
				throw new RuntimeException("Unhandled entity type " + entity.getType() + " in " + this.getClass().getSimpleName());
		}
	}

	private void updateCreatureAssets(Entity entity) {
		CreatureEntityAttributes attributes = (CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
		SkillsComponent skillsComponent = entity.getComponent(SkillsComponent.class);
		Skill primaryProfession = NULL_PROFESSION;
		if (skillsComponent != null) {
			primaryProfession = skillsComponent.getPrimaryProfession();
		}
		MilitaryComponent militaryComponent = entity.getComponent(MilitaryComponent.class);
		if (militaryComponent != null && militaryComponent.isInMilitary()) {
			primaryProfession = UNARMORED_MILITARY_OVERRIDE;
		}

		EquippedItemComponent equippedItemComponent = entity.getComponent(EquippedItemComponent.class);
		if (equippedItemComponent != null && equippedItemComponent.getEquippedClothing() != null) {
			Entity equippedClothing = equippedItemComponent.getEquippedClothing();
			AssetOverrideBySkillTag assetOverrideBySkillTag = equippedClothing.getTag(AssetOverrideBySkillTag.class);
			if (assetOverrideBySkillTag != null && equippedClothing.getPhysicalEntityComponent().getAttributes() instanceof ItemEntityAttributes clothingAttributes) {
				primaryProfession = assetOverrideBySkillTag.getSkill(skillDictionary);
				attributes.setColor(assetOverrideBySkillTag.getColoringLayer(), clothingAttributes.getPrimaryMaterial().getColor());
			}
		}


		CreatureEntityAsset baseAsset;
		if (gameContext != null && entity.getLocationComponent().getContainerEntity() != null && entity.isSettler()) {
			// Only show head and above when inside a container
			baseAsset = creatureEntityAssetDictionary.getMatching(CREATURE_HEAD, attributes, primaryProfession);
		} else {
			baseAsset = creatureEntityAssetDictionary.getMatching(CREATURE_BODY, attributes, primaryProfession);
		}

		entity.getPhysicalEntityComponent().getTypeMap().clear();
		entity.getPhysicalEntityComponent().setBaseAsset(baseAsset);
		if (baseAsset == null) {
			Logger.error("Base asset is null for " + attributes.toString());
		} else {
			entity.getPhysicalEntityComponent().getTypeMap().put(baseAsset.getType(), baseAsset);
			addOtherCreatureAssetTypes(baseAsset.getType(), entity, attributes, primaryProfession);
		}


		// Some special cases that should(?) be refactored away
		for (EntityAssetType hiddenAssetType : attributes.getHiddenAssetTypes()) {
			entity.getPhysicalEntityComponent().getTypeMap().remove(hiddenAssetType);

			// Special case to remove by merging hair and hair outlines
			if (hiddenAssetType.equals(CREATURE_HAIR)) {
				entity.getPhysicalEntityComponent().getTypeMap().remove(HAIR_OUTLINE);
			}
		}

		if (entity.getPhysicalEntityComponent().getTypeMap().containsKey(CLOTHING_OUTLINE)) {
			entity.getPhysicalEntityComponent().getTypeMap().remove(BODY_OUTLINE);
		}

		if (entity.getLocationComponent().getContainerEntity() == null) {
			addOtherCreatureAssetTypes(CREATURE_LEFT_HAND, entity, attributes, primaryProfession);
			addOtherCreatureAssetTypes(CREATURE_RIGHT_HAND, entity, attributes, primaryProfession);
		}

		// Tag processing
		processTags(entity);
	}

	private void updatePlantAssets(Entity entity) {
		PhysicalEntityComponent physicalComponent = entity.getPhysicalEntityComponent();
		PlantEntityAttributes attributes = (PlantEntityAttributes) physicalComponent.getAttributes();

		PlantEntityAsset baseAsset = plantEntityAssetDictionary.getPlantEntityAsset(branchAssetType, attributes);
		physicalComponent.setBaseAsset(baseAsset);
		physicalComponent.getTypeMap().clear();
		physicalComponent.getTypeMap().put(branchAssetType, baseAsset);

		PlantSpeciesGrowthStage growthStage = attributes.getSpecies().getGrowthStages().get(attributes.getGrowthStageCursor());

		Color leafColor = attributes.getColor(ColoringLayer.LEAF_COLOR);
		if (leafColor != null && !Color.CLEAR.equals(leafColor)) {
			PlantEntityAsset leafAsset = plantEntityAssetDictionary.getPlantEntityAsset(leafAssetType, attributes);
			physicalComponent.getTypeMap().put(leafAssetType, leafAsset);
		}

		if (growthStage.isShowFruit()) {
			PlantEntityAsset fruitAsset = plantEntityAssetDictionary.getPlantEntityAsset(fruitAssetType, attributes);
			physicalComponent.getTypeMap().put(fruitAssetType, fruitAsset);
		}

		processTags(entity);
	}

	private void updateItemAssets(Entity entity) {
		ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();

		ItemEntityAsset baseAsset = itemEntityAssetDictionary.getItemEntityAsset(ITEM_BASE_LAYER, attributes);
		entity.getPhysicalEntityComponent().setBaseAsset(baseAsset);
		if (baseAsset != null) {
			addOtherItemAssetTypes(baseAsset.getType(), entity, attributes);
		}

		// Tag processing
		processTags(entity);
	}

	private void updateMechanismAssets(Entity entity) {
		MechanismEntityAttributes attributes = (MechanismEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();

		MechanismEntityAsset baseAsset = mechanismEntityAssetDictionary.getMechanismEntityAsset(MECHANISM_BASE_LAYER, attributes);
		entity.getPhysicalEntityComponent().setBaseAsset(baseAsset);
		if (baseAsset != null) {
			addOtherMechanismAssetTypes(baseAsset.getType(), entity, attributes);
		}

		// Tag processing
		processTags(entity);
	}

	public void processTags(Entity entity) {
		Set<Tag> attachedTags = findAttachedTags(entity);
		entity.setTags(attachedTags);
		tagProcessor.apply(attachedTags, entity);
	}


	private void updateVehicleAssets(Entity entity) {
		VehicleEntityAttributes attributes = (VehicleEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();

		VehicleEntityAsset baseAsset = vehicleEntityAssetDictionary.getVehicleEntityAsset(VEHICLE_BASE_LAYER, attributes);
		entity.getPhysicalEntityComponent().setBaseAsset(baseAsset);
		if (baseAsset != null) {
			addOtherVehicleAssetTypes(baseAsset.getType(), entity, attributes);
		}

		Set<Tag> attachedTags = findAttachedTags(entity);
		attachedTags.addAll(attributes.getVehicleType().getProcessedTags());
		entity.setTags(attachedTags);
		tagProcessor.apply(attachedTags, entity);
	}

	private void addOtherVehicleAssetTypes(EntityAssetType assetType, Entity entity, VehicleEntityAttributes attributes) {
		VehicleEntityAsset asset = vehicleEntityAssetDictionary.getVehicleEntityAsset(assetType, attributes);

		if (asset != null) {
			entity.getPhysicalEntityComponent().getTypeMap().put(asset.getType(), asset);

			Set<EntityAssetType> attachedTypes = new HashSet<>();
			for (SpriteDescriptor spriteDescriptor : asset.getSpriteDescriptors().values()) {
				for (EntityChildAssetDescriptor childAssetDescriptor : spriteDescriptor.getChildAssets()) {
					if (childAssetDescriptor.getSpecificAssetName() == null) {
						// FIXME Specific assets should be found at setup time

						attachedTypes.add(childAssetDescriptor.getType());
					}
				}
			}

			for (EntityAssetType attachedType : attachedTypes) {
				if (shouldAssetTypeApply(attachedType, entity)) {
					addOtherVehicleAssetTypes(attachedType, entity, attributes);
				}
			}
		}
	}

	private void updateFurnitureAssets(Entity entity) {
		FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();

		if (attributes instanceof DoorwayEntityAttributes) {
			return;
		}
		FurnitureEntityAsset baseAsset = furnitureEntityAssetDictionary.getFurnitureEntityAsset(FURNITURE_BASE_LAYER, attributes);
		entity.getPhysicalEntityComponent().setBaseAsset(baseAsset);
		if (baseAsset != null) {
			addOtherFurnitureAssetTypes(baseAsset.getType(), entity, attributes);
		}

		Set<Tag> attachedTags = findAttachedTags(entity);
		attachedTags.addAll(attributes.getFurnitureType().getProcessedTags());
		entity.setTags(attachedTags);
		tagProcessor.apply(attachedTags, entity);
	}


	private void addOtherCreatureAssetTypes(EntityAssetType assetType, Entity entity, CreatureEntityAttributes attributes, Skill primaryProfession) {
		addOtherAssetTypes(assetType, entity, at -> creatureEntityAssetDictionary.getMatching(at, attributes, primaryProfession));
	}

	private void addOtherMechanismAssetTypes(EntityAssetType assetType, Entity entity, MechanismEntityAttributes attributes) {
		addOtherAssetTypes(assetType, entity, at -> mechanismEntityAssetDictionary.getMechanismEntityAsset(at, attributes));
	}

	private void addOtherFurnitureAssetTypes(EntityAssetType assetType, Entity entity, FurnitureEntityAttributes attributes) {
		addOtherAssetTypes(assetType, entity, at -> furnitureEntityAssetDictionary.getFurnitureEntityAsset(at, attributes));
	}

	private void addOtherItemAssetTypes(EntityAssetType assetType, Entity entity, ItemEntityAttributes attributes) {
		addOtherAssetTypes(assetType, entity, at -> itemEntityAssetDictionary.getItemEntityAsset(at, attributes));
	}

	private void addOtherAssetTypes(EntityAssetType assetType, Entity entity, Function<EntityAssetType, EntityAsset> getAsset) {
		EntityAsset asset = getAsset.apply(assetType);

		if (asset != null && asset.getType() != null) {
			entity.getPhysicalEntityComponent().getTypeMap().put(asset.getType(), asset);


			Set<EntityAssetType> attachedTypes = new HashSet<>();
			for (EntityAssetOrientation orientation : asset.getSpriteDescriptors().keySet()) {
				SpriteDescriptor spriteDescriptor = asset.getSpriteDescriptors().get(orientation);
				for (EntityChildAssetDescriptor childAssetDescriptor : spriteDescriptor.getChildAssets()) {

					if (childAssetDescriptor.getSpecificAssetName() == null) {
						// FIXME Specific assets should be found at setup time

						attachedTypes.add(childAssetDescriptor.getType());

						//bit of duplication from recursion
						for (String animationName : childAssetDescriptor.getInheritAnimations()) {
							if (spriteDescriptor.getEffectiveAnimationScripts().containsKey(animationName)) {
								EntityAsset childAsset = getAsset.apply(childAssetDescriptor.getType());
								if (childAsset != null) {
									SpriteDescriptor childSpriteDescriptor = childAsset.getSpriteDescriptors().get(orientation);
									if (childSpriteDescriptor != null) {
										AnimationScript parentScript = spriteDescriptor.getEffectiveAnimationScripts().get(animationName);
										AnimationScript inheritedScript = new AnimationScript();
										inheritedScript.setDuration(parentScript.getDuration());
										inheritedScript.setRotations(parentScript.getRotations());
										inheritedScript.setTranslations(parentScript.getTranslations());
										inheritedScript.setScalings(parentScript.getScalings());
										childSpriteDescriptor.getInheritedAnimationScripts().put(animationName, inheritedScript);
									}
								}
							}
						}
					}
				}
			}

			for (EntityAssetType attachedType : attachedTypes) {
				if (shouldAssetTypeApply(attachedType, entity)) {
					addOtherAssetTypes(attachedType, entity, getAsset);
				}
			}
		}
	}


	private Set<Tag> findAttachedTags(Entity entity) {
		Set<Tag> attachedTags = new LinkedHashSet<>();
		for (EntityAsset entityAsset : entity.getPhysicalEntityComponent().getTypeMap().values()) {
			attachedTags.addAll(tagProcessor.processRawTags(entityAsset.getTags()));
		}

		EntityAttributes entityAttributes = entity.getPhysicalEntityComponent().getAttributes();
		if (entityAttributes instanceof FurnitureEntityAttributes furnitureEntityAttributes) {
			attachedTags.addAll(furnitureEntityAttributes.getFurnitureType().getProcessedTags());
		} else if (entityAttributes instanceof ItemEntityAttributes itemEntityAttributes) {
			attachedTags.addAll(itemEntityAttributes.getItemType().getProcessedTags());
		} else if (entityAttributes instanceof PlantEntityAttributes plantEntityAttributes) {
			attachedTags.addAll(plantEntityAttributes.getSpecies().getProcessedTags());
		} else if (entityAttributes instanceof OngoingEffectAttributes ongoingEffectAttributes) {
			attachedTags.addAll(ongoingEffectAttributes.getType().getProcessedTags());
		} else if (entityAttributes instanceof CreatureEntityAttributes creatureAttributes) {
			attachedTags.addAll(creatureAttributes.getRace().getProcessedTags());
		} else if (entityAttributes instanceof VehicleEntityAttributes vehicleEntityAttributes) {
			attachedTags.addAll(vehicleEntityAttributes.getVehicleType().getProcessedTags());
		}

		return attachedTags;
	}

	private boolean shouldAssetTypeApply(EntityAssetType attachedType, Entity entity) {
		if (SHOW_WHEN_INVENTORY_TYPES.contains(attachedType)) {
			InventoryComponent inventoryComponent = entity.getComponent(InventoryComponent.class);
			return  inventoryComponent != null && !inventoryComponent.getInventoryEntries().isEmpty();
		}

		if (attachedType.equals(ITEM_COVER_LAYER) || attachedType.equals(FURNITURE_COVER_LAYER)) {
			LiquidContainerComponent liquidContainerComponent = entity.getComponent(LiquidContainerComponent.class);
			return liquidContainerComponent != null && liquidContainerComponent.getTargetLiquidMaterial() != null &&
					!shouldShowLiquidLayer(liquidContainerComponent.getTargetLiquidMaterial()) && liquidContainerComponent.getLiquidQuantity() > 0.1;
		} else if (attachedType.equals(ITEM_LIQUID_LAYER) || attachedType.equals(FURNITURE_LIQUID_LAYER)) {
			LiquidContainerComponent liquidContainerComponent = entity.getComponent(LiquidContainerComponent.class);
			return liquidContainerComponent != null && liquidContainerComponent.getTargetLiquidMaterial() != null &&
					shouldShowLiquidLayer(liquidContainerComponent.getTargetLiquidMaterial()) && liquidContainerComponent.getLiquidQuantity() > 0.1;
		} else {
			return true;
		}
	}

	private boolean shouldShowLiquidLayer(GameMaterial material) {
		return !material.isAlcoholic() && (material.isEdible() || material.isQuenchesThirst());
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
