package technology.rocketjump.mountaincore.mapping.factories;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.EntityAssetUpdater;
import technology.rocketjump.mountaincore.entities.EntityStore;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.FurnitureBehaviour;
import technology.rocketjump.mountaincore.entities.components.Faction;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.components.furniture.ConstructedEntityComponent;
import technology.rocketjump.mountaincore.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.mountaincore.entities.factories.*;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.mountaincore.entities.tags.StartingResourcesContainerTag;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.materials.GameMaterialDictionary;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.ItemPrimaryMaterialChangedMessage;
import technology.rocketjump.mountaincore.misc.VectorUtils;

import java.util.*;

@Singleton
public class EmbarkResourcesFactory {

	private static final int WAGON_CAPACITY = 16;
	public static final String DEFAULT_WAGON_DRAUGHT_ANIMAL = "Horse";

	private final GameMaterialDictionary gameMaterialDictionary;
	private final FurnitureEntityAttributesFactory furnitureEntityAttributesFactory;
	private final FurnitureEntityFactory furnitureEntityFactory;
	private final FurnitureTypeDictionary furnitureTypeDictionary;
	private final EntityStore entityStore;
	private final MessageDispatcher messageDispatcher;
	private final RaceDictionary raceDictionary;
	private final ItemEntityFactory itemEntityFactory;
	private final ItemEntityAttributesFactory itemEntityAttributesFactory;
	private final EntityAssetUpdater entityAssetUpdater;
	private final CreatureEntityAttributesFactory creatureEntityAttributesFactory;
	private final CreatureEntityFactory creatureEntityFactory;

	@Inject
	public EmbarkResourcesFactory(FurnitureTypeDictionary furnitureTypeDictionary, GameMaterialDictionary gameMaterialDictionary,
								  FurnitureEntityAttributesFactory furnitureEntityAttributesFactory, FurnitureEntityFactory furnitureEntityFactory,
								  EntityStore entityStore, MessageDispatcher messageDispatcher, RaceDictionary raceDictionary,
								  ItemEntityFactory itemEntityFactory,
								  ItemEntityAttributesFactory itemEntityAttributesFactory, EntityAssetUpdater entityAssetUpdater,
								  CreatureEntityAttributesFactory creatureEntityAttributesFactory, CreatureEntityFactory creatureEntityFactory) {
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.furnitureEntityAttributesFactory = furnitureEntityAttributesFactory;
		this.furnitureEntityFactory = furnitureEntityFactory;
		this.furnitureTypeDictionary = furnitureTypeDictionary;
		this.entityStore = entityStore;
		this.messageDispatcher = messageDispatcher;
		this.raceDictionary = raceDictionary;
		this.itemEntityFactory = itemEntityFactory;
		this.itemEntityAttributesFactory = itemEntityAttributesFactory;
		this.entityAssetUpdater = entityAssetUpdater;
		this.creatureEntityAttributesFactory = creatureEntityAttributesFactory;
		this.creatureEntityFactory = creatureEntityFactory;
	}

	public void spawnEmbarkResources(GridPoint2 embarkPoint, List<QuantifiedItemTypeWithMaterial> embarkResources, GameContext gameContext) {
		FurnitureType wagonFurnitureType = furnitureTypeDictionary.getAll()
				.stream().filter(ft -> ft.getProcessedTags().stream().anyMatch(t -> t instanceof StartingResourcesContainerTag))
				.findAny().orElseThrow();

		StartingResourcesContainerTag containerTag = (StartingResourcesContainerTag) wagonFurnitureType.getProcessedTags().stream().filter(t -> t instanceof StartingResourcesContainerTag)
				.findAny().orElseThrow();
		int wagonCapacity = containerTag.getCapacity();

		int embarkRegion = gameContext.getAreaMap().getTile(embarkPoint.x, embarkPoint.y).getRegionId();

		FurnitureLayout layoutToUse = wagonFurnitureType.getDefaultLayout();
		int timesToRotate = gameContext.getRandom().nextInt(3);
		for (int i = 0; i < timesToRotate; i++) {
			layoutToUse = layoutToUse.getRotatesTo();
		}

		int attempts = 0;
		while (!embarkResources.isEmpty()) {

			boolean xPositive = gameContext.getRandom().nextBoolean();
			boolean yPositive = gameContext.getRandom().nextBoolean();

			GridPoint2 attemptedLocation = embarkPoint.cpy().add(
					(xPositive ? 1 : -1) * (2 + gameContext.getRandom().nextInt(layoutToUse.getWidth() * 3)),
					(yPositive ? 1 : -1) * (2 + gameContext.getRandom().nextInt(layoutToUse.getHeight() * 3))
			);

			Set<GridPoint2> tilesToCheck = new HashSet<>();
			tilesToCheck.add(attemptedLocation);
			for (GridPoint2 extraTile : layoutToUse.getExtraTiles()) {
				tilesToCheck.add(attemptedLocation.cpy().add(extraTile));
			}

			if (canPlaceInto(tilesToCheck, embarkRegion, gameContext)) {
				Entity wagonEntity = createWagon(wagonFurnitureType, attemptedLocation, layoutToUse, tilesToCheck, gameContext);
				InventoryComponent wagonInventory = wagonEntity.getOrCreateComponent(InventoryComponent.class);
				int itemsAdded = 0;
				while (itemsAdded < wagonCapacity && !embarkResources.isEmpty()) {
					QuantifiedItemTypeWithMaterial requirement = embarkResources.remove(0);
					if (requirement.getItemType() != null) {
						GameMaterial material = requirement.getMaterial();
						if (material == null) {
							material = pickMaterial(requirement.getItemType().getPrimaryMaterialType(), gameContext.getRandom());
						}
						Entity item = itemEntityFactory.createByItemType(requirement.getItemType(), gameContext, true, Faction.SETTLEMENT);
						ItemEntityAttributes itemAttributes = (ItemEntityAttributes) item.getPhysicalEntityComponent().getAttributes();
						GameMaterial oldPrimaryMaterial = itemAttributes.getMaterial(requirement.getItemType().getPrimaryMaterialType());
						itemAttributes.setMaterial(material);
						itemAttributes.setQuantity(requirement.getQuantity());
						messageDispatcher.dispatchMessage(MessageType.ITEM_PRIMARY_MATERIAL_CHANGED, new ItemPrimaryMaterialChangedMessage(item, oldPrimaryMaterial));
						wagonInventory.add(item, wagonEntity, messageDispatcher, gameContext.getGameClock());
						itemsAdded++;
					}
				}

				entityAssetUpdater.updateEntityAssets(wagonEntity);
			}

			attempts++;
			if (attempts > 5000) {
				Logger.error("Could not find valid location to spawn starting wagon");
				break;
			}
		}
	}

	private GameMaterial pickMaterial(GameMaterialType primaryMaterialType, Random random) {
		List<GameMaterial> materials = gameMaterialDictionary.getByType(primaryMaterialType).stream()
				.filter(GameMaterial::isUseInRandomGeneration)
				.toList();
		return materials.get(random.nextInt(materials.size()));
	}

	private boolean canPlaceInto(Set<GridPoint2> tilesToCheck, int embarkRegion, GameContext gameContext) {
		for (GridPoint2 gridPoint2 : tilesToCheck) {
			MapTile tile = gameContext.getAreaMap().getTile(gridPoint2);
			if (tile == null || tile.getRegionId() != embarkRegion || tile.hasWall() || tile.getFloor().isRiverTile()) {
				return false;
			}

			for (Entity entity : tile.getEntities()) {
				if (entity.getType().equals(EntityType.FURNITURE)) {
					return false;
				}
			}

		}
		return true;
	}

	private Entity createWagon(FurnitureType wagonFurnitureType, GridPoint2 primaryLocation, FurnitureLayout layoutToUse,
							   Set<GridPoint2> tilesCovered, GameContext gameContext) {
		// remove all other entities (checked that it doesn't contain furniture)
		tilesCovered.forEach(location -> {
			List<Long> entitiesToRemove = new LinkedList<>();
			MapTile tile = gameContext.getAreaMap().getTile(location);
			tile.getEntities().forEach(entity -> entitiesToRemove.add(entity.getId()));
			// Separate loop to avoid ConcurrentModificationException
			for (Long entityId : entitiesToRemove) {
				entityStore.remove(entityId);
				tile.removeEntity(entityId);
			}
		});

		FurnitureEntityAttributes wagonAttributes = furnitureEntityAttributesFactory.byType(wagonFurnitureType, gameMaterialDictionary.getExampleMaterial(wagonFurnitureType.getRequirements().keySet().iterator().next()));
		wagonAttributes.setCurrentLayout(layoutToUse);

		Entity wagonEntity = furnitureEntityFactory.create(wagonAttributes, primaryLocation, new FurnitureBehaviour(), gameContext);
		ConstructedEntityComponent constructedEntityComponent = new ConstructedEntityComponent();
		constructedEntityComponent.init(wagonEntity, messageDispatcher, gameContext);
		wagonEntity.addComponent(constructedEntityComponent);

		entityStore.add(wagonEntity);
		messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, wagonEntity);

		spawnWagonDraughtAnimal(gameContext, primaryLocation);

		return wagonEntity;
	}

	private void spawnWagonDraughtAnimal(GameContext gameContext, GridPoint2 primaryLocation) {
		GridPoint2 spawnLocation = null;
		int attempts = 0;
		MapTile primaryLocationTile = gameContext.getAreaMap().getTile(primaryLocation);

		while (attempts < 500 && spawnLocation == null) {
			GridPoint2 potentialLocation = primaryLocation.cpy().add(
				-3 + gameContext.getRandom().nextInt(7),
					-3 + gameContext.getRandom().nextInt(7)
			);
			MapTile tile = gameContext.getAreaMap().getTile(potentialLocation);
			if (tile != null && tile.isNavigable(null) && tile.getRegionId() == primaryLocationTile.getRegionId()) {
				spawnLocation = potentialLocation;
			}
			attempts++;
		}

		if (spawnLocation != null) {
			CreatureEntityAttributes attributes = creatureEntityAttributesFactory.create(raceDictionary.getByName(DEFAULT_WAGON_DRAUGHT_ANIMAL));
			creatureEntityFactory.create(attributes, VectorUtils.toVector(spawnLocation), new Vector2(), gameContext, Faction.SETTLEMENT);
		} else {
			Logger.warn("Could not find a valid location to spawn a draught animal for the wagon");
		}
	}

}
