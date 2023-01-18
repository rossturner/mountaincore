package technology.rocketjump.saul.mapping.factories;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.EntityStore;
import technology.rocketjump.saul.entities.behaviour.furniture.FurnitureBehaviour;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.saul.entities.factories.FurnitureEntityAttributesFactory;
import technology.rocketjump.saul.entities.factories.FurnitureEntityFactory;
import technology.rocketjump.saul.entities.factories.ItemEntityAttributesFactory;
import technology.rocketjump.saul.entities.factories.ItemEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.saul.entities.tags.StartingResourcesContainerTag;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.materials.model.GameMaterialType;
import technology.rocketjump.saul.messaging.MessageType;

import java.util.*;

@Singleton
public class EmbarkResourcesFactory {

	private static final int WAGON_CAPACITY = 16;

	private final GameMaterialDictionary gameMaterialDictionary;
	private final FurnitureEntityAttributesFactory furnitureEntityAttributesFactory;
	private final FurnitureEntityFactory furnitureEntityFactory;
	private final FurnitureTypeDictionary furnitureTypeDictionary;
	private final EntityStore entityStore;
	private final MessageDispatcher messageDispatcher;
	private final ItemEntityFactory itemEntityFactory;
	private final ItemEntityAttributesFactory itemEntityAttributesFactory;

	@Inject
	public EmbarkResourcesFactory(FurnitureTypeDictionary furnitureTypeDictionary, GameMaterialDictionary gameMaterialDictionary,
								  FurnitureEntityAttributesFactory furnitureEntityAttributesFactory, FurnitureEntityFactory furnitureEntityFactory,
								  EntityStore entityStore, MessageDispatcher messageDispatcher, ItemEntityFactory itemEntityFactory,
								  ItemEntityAttributesFactory itemEntityAttributesFactory) {
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.furnitureEntityAttributesFactory = furnitureEntityAttributesFactory;
		this.furnitureEntityFactory = furnitureEntityFactory;
		this.furnitureTypeDictionary = furnitureTypeDictionary;
		this.entityStore = entityStore;
		this.messageDispatcher = messageDispatcher;
		this.itemEntityFactory = itemEntityFactory;
		this.itemEntityAttributesFactory = itemEntityAttributesFactory;
	}

	public void spawnEmbarkResources(GridPoint2 embarkPoint, List<QuantifiedItemTypeWithMaterial> embarkResources, GameContext gameContext) {
		FurnitureType wagonFurnitureType = furnitureTypeDictionary.getAll()
				.stream().filter(ft -> ft.getProcessedTags().stream().anyMatch(t -> t instanceof StartingResourcesContainerTag))
				.findAny().orElseThrow();
		Collections.shuffle(embarkResources, gameContext.getRandom());

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
						ItemEntityAttributes itemAttributes = itemEntityAttributesFactory.createItemAttributes(requirement.getItemType(), requirement.getQuantity(), material);
						Entity startingItem = itemEntityFactory.create(itemAttributes, null, true, gameContext);
						wagonInventory.add(startingItem, wagonEntity, messageDispatcher, gameContext.getGameClock());
						itemsAdded++;
					}
				}
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

		entityStore.add(wagonEntity);
		messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, wagonEntity);
		return wagonEntity;
	}

}
