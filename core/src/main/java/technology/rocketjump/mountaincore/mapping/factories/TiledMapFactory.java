package technology.rocketjump.mountaincore.mapping.factories;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.assets.FloorTypeDictionary;
import technology.rocketjump.mountaincore.assets.model.FloorType;
import technology.rocketjump.mountaincore.cooking.CookingRecipeDictionary;
import technology.rocketjump.mountaincore.entities.EntityStore;
import technology.rocketjump.mountaincore.entities.components.creature.SkillsComponent;
import technology.rocketjump.mountaincore.entities.factories.ItemEntityAttributesFactory;
import technology.rocketjump.mountaincore.entities.factories.ItemEntityFactory;
import technology.rocketjump.mountaincore.entities.factories.SettlerFactory;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantSpeciesDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantSpeciesType;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapgen.model.output.GameMap;
import technology.rocketjump.mountaincore.mapping.model.InvalidMapGenerationException;
import technology.rocketjump.mountaincore.mapping.model.TiledMap;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.materials.GameMaterialDictionary;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.ReplaceRegionMessage;
import technology.rocketjump.mountaincore.production.StockpileComponentUpdater;
import technology.rocketjump.mountaincore.rooms.RoomTypeDictionary;

import java.util.*;
import java.util.stream.Collectors;

import static com.badlogic.gdx.math.MathUtils.random;
import static technology.rocketjump.mountaincore.materials.model.GameMaterialType.METAL;

public class TiledMapFactory {

	private final MapGenWrapper mapGenWrapper;
	private final GameMapConverter gameMapConverter;
	private final ItemTypeDictionary itemTypeDictionary;
	private final GameMaterialDictionary materialDictionary;
	private final FloorType baseFloor;
	private final GameMaterial baseFloorMaterial;
	private final PlantSpeciesDictionary plantSpeciesDictionary;
	private final RoomTypeDictionary roomTypeDictionary;
	private final EntityStore entityStore;
	private final SettlerFactory setterFactory;

	// TODO remove the following when not starting with crops
	private final ItemEntityFactory itemEntityFactory;
	private final ItemEntityAttributesFactory itemEntityAttributesFactory;
	private final CookingRecipeDictionary cookingRecipeDictionary;
	private final List<PlantSpecies> crops = new ArrayList<>();
	private final GameMaterialDictionary gameMaterialDictionary;
	private final StockpileComponentUpdater stockpileComponentUpdater;
	private final CreaturePopulator creaturePopulator;
	private final EmbarkResourcesFactory embarkResourcesFactory;

	@Inject
	public TiledMapFactory(MapGenWrapper mapGenWrapper, ItemTypeDictionary itemTypeDictionary,
						   GameMaterialDictionary gameMaterialDictionary, FloorTypeDictionary floorTypeDictionary,
						   GameMapConverter gameMapConverter, PlantSpeciesDictionary plantSpeciesDictionary,
						   RoomTypeDictionary roomTypeDictionary, EntityStore entityStore, SettlerFactory setterFactory,
						   ItemEntityFactory itemEntityFactory, ItemEntityAttributesFactory itemEntityAttributesFactory,
						   CookingRecipeDictionary cookingRecipeDictionary,
						   StockpileComponentUpdater stockpileComponentUpdater, CreaturePopulator creaturePopulator, EmbarkResourcesFactory embarkResourcesFactory) {
		this.mapGenWrapper = mapGenWrapper;
		this.itemTypeDictionary = itemTypeDictionary;
		this.materialDictionary = gameMaterialDictionary;

		this.baseFloor = floorTypeDictionary.getByFloorTypeName("rough_stone");
		this.gameMapConverter = gameMapConverter;
		this.plantSpeciesDictionary = plantSpeciesDictionary;
		this.roomTypeDictionary = roomTypeDictionary;
		this.entityStore = entityStore;
		this.setterFactory = setterFactory;
		this.itemEntityFactory = itemEntityFactory;
		this.itemEntityAttributesFactory = itemEntityAttributesFactory;
		this.cookingRecipeDictionary = cookingRecipeDictionary;
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.stockpileComponentUpdater = stockpileComponentUpdater;
		this.creaturePopulator = creaturePopulator;
		this.embarkResourcesFactory = embarkResourcesFactory;
		this.baseFloorMaterial = materialDictionary.getByName("Granite");

		for (PlantSpecies plantSpecies : plantSpeciesDictionary.getAll()) {
			if (plantSpecies.getPlantType().equals(PlantSpeciesType.CROP) && plantSpecies.getSeed() != null) {
				crops.add(plantSpecies);
			}
		}
	}

	public TiledMap create(long worldSeed, int worldWidth, int worldHeight, GameContext newGameContext) throws InvalidMapGenerationException {
		GameMap mapGenGameMap = mapGenWrapper.createUsingLibrary(worldSeed, worldWidth, worldHeight);

		final TiledMap areaMap = new TiledMap(worldSeed, worldWidth, worldHeight, baseFloor, baseFloorMaterial);
		newGameContext.setAreaMap(areaMap);
		gameMapConverter.apply(mapGenGameMap, areaMap, worldSeed);

		return areaMap;
	}

	// Note this passes in MessageDispatcher as a guard against using it in this class before the GameContext is set up
	public void preSelectSpawnStep(GameContext gameContext, MessageDispatcher messageDispatcher) {
		TiledMap areaMap = gameContext.getAreaMap();
		GridPoint2 embarkPoint = areaMap.getEmbarkPoint();
		messageDispatcher.dispatchMessage(MessageType.FLOOD_FILL_EXPLORATION, embarkPoint);
	}


	// Note this passes in MessageDispatcher as a guard against using it in this class before the GameContext is set up
	public void postSelectSpawnStep(GameContext gameContext, MessageDispatcher messageDispatcher, List<SkillsComponent> professionList) {
		TiledMap areaMap = gameContext.getAreaMap();
		GridPoint2 embarkPoint = areaMap.getEmbarkPoint();
		messageDispatcher.dispatchMessage(MessageType.FLOOD_FILL_EXPLORATION, embarkPoint);

		List<QuantifiedItemTypeWithMaterial> embarkResources = new ArrayList<>();
		embarkResources.addAll(initInitiallyHauledStartingItems());
		embarkResources.addAll(initInventoryStartingItems());

		int xOffset = -1;
		int yOffset = -1;

		for (SkillsComponent skillsComponent : professionList) {
			createSettler(embarkPoint.x + xOffset, embarkPoint.y + yOffset, skillsComponent, gameContext, messageDispatcher);

			xOffset++;
			if (xOffset > 1) {
				xOffset = -1;
				yOffset++;
			}
			if (yOffset > 1) {
				yOffset = -1;
			}
		}

		ItemType plankItemType = itemTypeDictionary.getByName("Resource-Planks");
		GameMaterial plankMaterialType = pickWoodMaterialType();
		ItemType stoneBlockItemType = itemTypeDictionary.getByName("Resource-Stone-Block");
		GameMaterial stoneBlockMaterialType = pickMaterialType(stoneBlockItemType);
		ItemType metalItemType = itemTypeDictionary.getByName("Resource-Metal-Ingot");
		ItemType plateItemType = itemTypeDictionary.getByName("Resource-Metal-Plate");
		ItemType hoopsItemType = itemTypeDictionary.getByName("Product-Barrel-Hoops");
		GameMaterial metalMaterial = pickMaterialType(metalItemType);

		embarkResources.add(describeStackOf(plankItemType, plankMaterialType));
		embarkResources.add(describeStackOf(plankItemType, plankMaterialType));
		embarkResources.add(describeStackOf(plankItemType, plankMaterialType));

		embarkResources.add(describeStackOf(stoneBlockItemType, stoneBlockMaterialType));
		embarkResources.add(describeStackOf(stoneBlockItemType, stoneBlockMaterialType));
		embarkResources.add(describeStackOf(stoneBlockItemType, stoneBlockMaterialType));

		embarkResources.add(describeStackOf(hoopsItemType, pickMaterialType(METAL)));
		embarkResources.add(describeStackOf(metalItemType, metalMaterial));
		embarkResources.add(describeStackOf(plateItemType, pickMaterialType(METAL)));

//		List<StockpileGroup> stockpileGroupList = new ArrayList<>(stockpileGroups);
//		RoomType stockpileRoomType = roomTypeDictionary.getByName("STOCKPILE");

//		messageDispatcher.dispatchMessage(MessageType.ROOM_PLACEMENT, new RoomPlacementMessage(roomTiles, stockpileRoomType, stockpileGroupList.get(0)));

//		Room placedRoom = gameContext.getAreaMap().getTile(embarkPoint).getRoomTile().getRoom();
//		StockpileRoomComponent stockpileRoomComponent = placedRoom.getComponent(StockpileRoomComponent.class);
//		for (StockpileGroup stockpileGroup : stockpileGroupList) {
//			stockpileComponentUpdater.toggleGroup(stockpileRoomComponent.getStockpileSettings(), stockpileGroup, false, true);
//		}
//		for (ItemType placedItemType : placedItems) {
//			if (!stockpileRoomComponent.getStockpileSettings().isEnabled(placedItemType)) {
//				stockpileComponentUpdater.toggleItem(stockpileRoomComponent.getStockpileSettings(), placedItemType, true, true, true);
//			}
//		}

		embarkResourcesFactory.spawnEmbarkResources(embarkPoint, embarkResources, gameContext);

		creaturePopulator.initialiseMap(gameContext);
	}

	private QuantifiedItemTypeWithMaterial describeStackOf(ItemType itemType, GameMaterial material) {
		QuantifiedItemTypeWithMaterial quantifiedItemTypeWithMaterial = new QuantifiedItemTypeWithMaterial();
		quantifiedItemTypeWithMaterial.setItemType(itemType);
		quantifiedItemTypeWithMaterial.setMaterial(material);
		quantifiedItemTypeWithMaterial.setQuantity(itemType.getMaxStackSize());
		return quantifiedItemTypeWithMaterial;
	}

	private Entity createSettler(int tileX, int tileY, SkillsComponent skillsComponent, GameContext gameContext, MessageDispatcher messageDispatcher) {
		Random random = new Random();
		Vector2 worldPosition = new Vector2(tileX + 0.5f + (0.1f - (random.nextFloat() * 0.2f)), tileY + 0.5f+ (0.1f - (random.nextFloat() * 0.2f)));

		MapTile tile = gameContext.getAreaMap().getTile(tileX, tileY);
		if (tile.hasWall()) {
			messageDispatcher.dispatchMessage(MessageType.REMOVE_WALL, tile.getTilePosition());
		}
		List<Entity> entitiesToRemove = new LinkedList<>();
		for (Entity entity : tile.getEntities()) {
			if (!entity.getType().equals(EntityType.CREATURE)) {
				entitiesToRemove.add(entity);
			}
		}
		// Separate loop to avoid ConcurrentModificationException
		for (Entity entity : entitiesToRemove) {
			entityStore.remove(entity.getId());
			tile.removeEntity(entity.getId());

			Integer neighbourRegionId = gameContext.getAreaMap().getNeighbours(tileX, tileY).values().stream()
					.filter(t -> t.getRegionType().equals(MapTile.RegionType.GENERIC))
					.findAny()
					.map(MapTile::getRegionId)
					.orElse(tile.getRegionId());
			if (tile.getRegionId() != neighbourRegionId) {
				messageDispatcher.dispatchMessage(MessageType.REPLACE_REGION, new ReplaceRegionMessage(tile, neighbourRegionId));
			}
		}

		Entity settler = setterFactory.create(worldPosition, skillsComponent, gameContext, true);

//		if (GlobalSettings.DEV_MODE) {
//			HaulingComponent haulingComponent = settler.getOrCreateComponent(HaulingComponent.class);
//
//			ItemTypeWithMaterial itemTypeWithMaterial = new ItemTypeWithMaterial();
//			if (gameContext.getRandom().nextBoolean()) {
//				if (gameContext.getRandom().nextBoolean()) {
//					CookingRecipe recipe = cookingRecipeDictionary.getByName("Create soup");
//					itemTypeWithMaterial = recipe.getInputItemOptions().get(gameContext.getRandom().nextInt(recipe.getInputItemOptions().size()));
//				} else {
//					itemTypeWithMaterial.setItemType(itemTypeDictionary.getByName("Fuel-Sack"));
//					itemTypeWithMaterial.setMaterial(materialDictionary.getByName("Charcoal"));
//				}
//			} else {
//				itemTypeWithMaterial.setItemType(itemTypeDictionary.getByName("Resource-Grain-Sack"));
//				itemTypeWithMaterial.setMaterial(materialDictionary.getByName("Barley"));
//			}
//
//			ItemEntityAttributes itemAttributes = new ItemEntityAttributes(gameContext.getRandom().nextLong());
//			itemAttributes.setItemType(itemTypeWithMaterial.getItemType());
//			itemAttributes.setMaterial(itemTypeWithMaterial.getMaterial());
//
//			for (GameMaterialType otherRequiredMaterialTypes : itemAttributes.getItemType().getMaterialTypes()) {
//				if (itemAttributes.getMaterial(otherRequiredMaterialTypes) == null) {
//					itemAttributes.setMaterial(pickMaterialType(otherRequiredMaterialTypes));
//				}
//			}
//
//			itemAttributes.setQuantity(1 + gameContext.getRandom().nextInt(10));
//			if (itemAttributes.getQuantity() > itemAttributes.getItemType().getMaxStackSize()) {
//				itemAttributes.setQuantity(itemAttributes.getItemType().getMaxStackSize());
//			}
//
//			Entity itemEntity = itemEntityFactory.create(itemAttributes, new GridPoint2(tileX, tileY), true, gameContext, Faction.SETTLEMENT);
//			haulingComponent.setHauledEntity(itemEntity, messageDispatcher, settler);
//		}

		return settler;
	}

	private GameMaterial pickWoodMaterialType() {
		List<PlantSpecies> treeSpecies = new ArrayList<>();
		for (PlantSpecies plantSpecies : plantSpeciesDictionary.getAll()) {
			if (plantSpecies.getPlantType().equals(PlantSpeciesType.TREE) && plantSpecies.getMaterial().isUseInRandomGeneration()) {
				treeSpecies.add(plantSpecies);
			}
		}

		return treeSpecies.get(random.nextInt(treeSpecies.size())).getMaterial();
	}

	private GameMaterial pickMaterialType(ItemType itemType) {
		return pickMaterialType(itemType.getPrimaryMaterialType());
	}

	private GameMaterial pickMaterialType(GameMaterialType materialType) {
		List<GameMaterial> materials = materialDictionary.getByType(materialType).stream()
				.filter(GameMaterial::isUseInRandomGeneration).collect(Collectors.toList());
		return materials.get(random.nextInt(materials.size()));
	}

	private Deque<QuantifiedItemTypeWithMaterial> initInitiallyHauledStartingItems() {
		Deque<QuantifiedItemTypeWithMaterial> items = new ArrayDeque<>();
		items.addAll(item("Product-Barrel", 2));
		items.addAll(item("Product-Bucket", 2));
		items.addAll(item("Product-Anvil", 2));
		items.addAll(item("Product-Cauldron", 2));
		return items;
	}

	private Deque<QuantifiedItemTypeWithMaterial> initInventoryStartingItems() {
		Deque<QuantifiedItemTypeWithMaterial> items = new ArrayDeque<>();
		items.addAll(item("Tool-Axe", 4));
		items.addAll(item("Tool-Chisel", 4));
		items.addAll(item("Tool-Large-Hammer", 2));
		items.addAll(item("Tool-Pickaxe", 4));
		items.addAll(item("Tool-Plane", 3));
		items.addAll(item("Tool-Rolling-Pin", 2));
		items.addAll(item("Tool-Kitchen-Knife", 2));
		items.addAll(item("Tool-Saw", 4));
		items.addAll(item("Tool-Small-Hammer", 3));
		items.addAll(item("Tool-Tongs", 3));

		items.addAll(item("Ingredient-Seeds", 20, "Carrot Seed"));
		items.addAll(item("Ingredient-Seeds", 18, "Corn Seed"));
		items.addAll(item("Ingredient-Seeds", 16, "Potato Seed"));
		items.addAll(item("Ingredient-Seeds", 18, "Tomato Seed"));
		items.addAll(item("Ingredient-Seeds", 12, "Wheat Seed"));

		items.addAll(item("Ingredient-Seeds", 16, "Barley Seed"));
		items.addAll(item("Ingredient-Seeds", 12, "Hops Seed"));
		items.addAll(item("Ingredient-Seeds", 8, "Hemp Seed"));

		return items;
	}

	private List<QuantifiedItemTypeWithMaterial> item(String itemTypeName, int quantity) {
		return item(itemTypeName, quantity, null);
	}

	private List<QuantifiedItemTypeWithMaterial> item(String itemTypeName, int quantity, String materialName) {
		ItemType itemType = itemTypeDictionary.getByName(itemTypeName);
		if (itemType == null) {
			Logger.error("Could not find item type with name " + itemTypeName + " to init settlement with");
		}

		List<QuantifiedItemTypeWithMaterial> result = new ArrayList<>();

		while (quantity > 0) {
			int amountInThisStack = Math.min(quantity, itemType.getMaxStackSize());

			QuantifiedItemTypeWithMaterial quantifiedItemType = new QuantifiedItemTypeWithMaterial();
			quantifiedItemType.setItemType(itemType);
			quantifiedItemType.setQuantity(amountInThisStack);
			if (materialName != null) {
				quantifiedItemType.setMaterial(materialDictionary.getByName(materialName));
			}
			if (materialName != null && quantifiedItemType.getMaterial() == null) {
				Logger.error("Could not find material with name " + materialName);
			}

			quantity -= amountInThisStack;
			result.add(quantifiedItemType);
		}

		return result;
	}
}
