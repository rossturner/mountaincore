package technology.rocketjump.saul.mapping.factories;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.FloorTypeDictionary;
import technology.rocketjump.saul.assets.model.FloorType;
import technology.rocketjump.saul.cooking.CookingRecipeDictionary;
import technology.rocketjump.saul.cooking.model.CookingRecipe;
import technology.rocketjump.saul.entities.EntityStore;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.factories.ItemEntityAttributesFactory;
import technology.rocketjump.saul.entities.factories.ItemEntityFactory;
import technology.rocketjump.saul.entities.factories.SettlerFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.creature.HaulingComponent;
import technology.rocketjump.saul.entities.model.physical.item.*;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesDictionary;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.model.Profession;
import technology.rocketjump.saul.mapgen.model.output.GameMap;
import technology.rocketjump.saul.mapping.model.InvalidMapGenerationException;
import technology.rocketjump.saul.mapping.model.TiledMap;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.materials.model.GameMaterialType;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.RoomPlacementMessage;
import technology.rocketjump.saul.rendering.camera.GlobalSettings;
import technology.rocketjump.saul.rooms.*;
import technology.rocketjump.saul.rooms.components.StockpileComponent;

import java.util.*;
import java.util.stream.Collectors;

import static com.badlogic.gdx.math.MathUtils.random;
import static technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesType.TREE;
import static technology.rocketjump.saul.materials.model.GameMaterialType.METAL;

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

	@Inject
	public TiledMapFactory(MapGenWrapper mapGenWrapper, ItemTypeDictionary itemTypeDictionary,
						   GameMaterialDictionary gameMaterialDictionary, FloorTypeDictionary floorTypeDictionary,
						   GameMapConverter gameMapConverter, PlantSpeciesDictionary plantSpeciesDictionary,
						   RoomTypeDictionary roomTypeDictionary, EntityStore entityStore, SettlerFactory setterFactory,
						   ItemEntityFactory itemEntityFactory, ItemEntityAttributesFactory itemEntityAttributesFactory,
						   CookingRecipeDictionary cookingRecipeDictionary,
						   StockpileComponentUpdater stockpileComponentUpdater, CreaturePopulator creaturePopulator) {
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
	public void postSelectSpawnStep(GameContext gameContext, MessageDispatcher messageDispatcher, List<Profession> professionList) {
		TiledMap areaMap = gameContext.getAreaMap();
		GridPoint2 embarkPoint = areaMap.getEmbarkPoint();
		messageDispatcher.dispatchMessage(MessageType.FLOOD_FILL_EXPLORATION, embarkPoint);

		Deque<QuantifiedItemTypeWithMaterial> initiallyHauledStartingItems = initInitiallyHauledStartingItems();
		Deque<QuantifiedItemTypeWithMaterial> inventoryStartingItems = initInventoryStartingItems(professionList.size(), gameContext.getRandom());
		List<Entity> allSettlers = new ArrayList<>();

		for (Profession primaryProfession : professionList) {
			Profession secondaryProfession = professionList.get(gameContext.getRandom().nextInt(professionList.size()));

			Entity settler = createSettler(embarkPoint.x, embarkPoint.y, primaryProfession, secondaryProfession, gameContext, messageDispatcher);

			addAnyHauledItems(settler, initiallyHauledStartingItems, gameContext, messageDispatcher);
			allSettlers.add(settler);
		}

		addStartingInventory(inventoryStartingItems, allSettlers, gameContext, messageDispatcher);

		ItemType plankItemType = itemTypeDictionary.getByName("Resource-Planks");
		GameMaterial plankMaterialType = pickWoodMaterialType();
		ItemType stoneBlockItemType = itemTypeDictionary.getByName("Resource-Stone-Block");
		GameMaterial stoneBlockMaterialType = pickMaterialType(stoneBlockItemType);
		ItemType metalItemType = itemTypeDictionary.getByName("Resource-Metal-Ingot");
		ItemType plateItemType = itemTypeDictionary.getByName("Resource-Metal-Plate");
		ItemType hoopsItemType = itemTypeDictionary.getByName("Product-Barrel-Hoops");
		GameMaterial metalMaterial = pickMaterialType(metalItemType);
		List<ItemType> placedItems = Arrays.asList(plankItemType, stoneBlockItemType, metalItemType, plateItemType, hoopsItemType);

		Map<GridPoint2, RoomTile> roomTiles = new HashMap<>();
		Set<StockpileGroup> stockpileGroups = new HashSet<>();

		createResources(embarkPoint.x - 1, embarkPoint.y - 1, plankItemType, plankMaterialType, gameContext, messageDispatcher, roomTiles, stockpileGroups);
		createResources(embarkPoint.x - 1, embarkPoint.y, plankItemType, plankMaterialType, gameContext, messageDispatcher, roomTiles, stockpileGroups);
		createResources(embarkPoint.x - 1, embarkPoint.y + 1, plankItemType, plankMaterialType, gameContext, messageDispatcher, roomTiles, stockpileGroups);

		createResources(embarkPoint.x, embarkPoint.y - 1, stoneBlockItemType, stoneBlockMaterialType, gameContext, messageDispatcher, roomTiles, stockpileGroups);
		createResources(embarkPoint.x, embarkPoint.y, stoneBlockItemType, stoneBlockMaterialType, gameContext, messageDispatcher, roomTiles, stockpileGroups);
		createResources(embarkPoint.x, embarkPoint.y + 1, stoneBlockItemType, stoneBlockMaterialType, gameContext, messageDispatcher, roomTiles, stockpileGroups);

		createResources(embarkPoint.x + 1, embarkPoint.y - 1, hoopsItemType, pickMaterialType(METAL), gameContext, messageDispatcher, roomTiles, stockpileGroups);
		createResources(embarkPoint.x + 1, embarkPoint.y, metalItemType, metalMaterial, gameContext, messageDispatcher, roomTiles, stockpileGroups);
		createResources(embarkPoint.x + 1, embarkPoint.y + 1, plateItemType, pickMaterialType(METAL), gameContext, messageDispatcher, roomTiles, stockpileGroups);

		List<StockpileGroup> stockpileGroupList = new ArrayList<>(stockpileGroups);
		RoomType stockpileRoomType = roomTypeDictionary.getByName("STOCKPILE");

		messageDispatcher.dispatchMessage(MessageType.ROOM_PLACEMENT, new RoomPlacementMessage(roomTiles, stockpileRoomType, stockpileGroupList.get(0)));

		Room placedRoom = gameContext.getAreaMap().getTile(embarkPoint).getRoomTile().getRoom();
		StockpileComponent stockpileComponent = placedRoom.getComponent(StockpileComponent.class);
		for (StockpileGroup stockpileGroup : stockpileGroupList) {
			stockpileComponentUpdater.toggleGroup(stockpileComponent, stockpileGroup, false, true);
		}
		for (ItemType placedItemType : placedItems) {
			if (!stockpileComponent.isEnabled(placedItemType)) {
				stockpileComponentUpdater.toggleItem(stockpileComponent, placedItemType, true, true, true);
			}
		}

		creaturePopulator.initialiseMap(gameContext);

		messageDispatcher.dispatchMessage(MessageType.SETTLEMENT_SPAWNED);
	}

	private void addStartingInventory(Deque<QuantifiedItemTypeWithMaterial> inventoryStartingItems, List<Entity> allSettlers, GameContext gameContext, MessageDispatcher messageDispatcher) {
		List<Entity> shuffledSettlers = new ArrayList<>(allSettlers);
		Collections.shuffle(shuffledSettlers, gameContext.getRandom());
		for (QuantifiedItemTypeWithMaterial inventoryStartingItem : inventoryStartingItems) {
			Entity settler = shuffledSettlers.remove(0);

			ItemEntityAttributes itemAttributes = new ItemEntityAttributes(gameContext.getRandom().nextLong());
			itemAttributes.setItemType(inventoryStartingItem.getItemType());
			for (GameMaterialType requiredMaterialType : inventoryStartingItem.getItemType().getMaterialTypes()) {
				List<GameMaterial> materialsToPickFrom = gameMaterialDictionary.getByType(requiredMaterialType).stream()
						.filter(GameMaterial::isUseInRandomGeneration).collect(Collectors.toList());
				GameMaterial material = materialsToPickFrom.get(gameContext.getRandom().nextInt(materialsToPickFrom.size()));
				itemAttributes.setMaterial(material);
			}
			itemAttributes.setMaterial(inventoryStartingItem.getMaterial());
			itemAttributes.setQuantity(inventoryStartingItem.getQuantity() > 0 ? inventoryStartingItem.getQuantity() : 1);
			Entity item = itemEntityFactory.create(itemAttributes, null, true, gameContext);

			InventoryComponent inventoryComponent = settler.getOrCreateComponent(InventoryComponent.class);
			InventoryComponent.InventoryEntry entry = inventoryComponent.add(item, settler, messageDispatcher, gameContext.getGameClock());
			// Set items to be expired in inventory already
			entry.setLastUpdateGameTime(0 - inventoryStartingItem.getItemType().getHoursInInventoryUntilUnused());

			if (shuffledSettlers.isEmpty()) {
				shuffledSettlers.addAll(allSettlers);
				Collections.shuffle(shuffledSettlers, gameContext.getRandom());
			}
		}
	}

	private Entity createSettler(int tileX, int tileY, Profession primaryprofession, Profession secondaryProfession, GameContext gameContext, MessageDispatcher messageDispatcher) {
		Random random = new Random();
		Vector2 worldPosition = new Vector2(tileX + 0.5f + (0.1f - (random.nextFloat() * 0.2f)), tileY + 0.5f+ (0.1f - (random.nextFloat() * 0.2f)));
		Vector2 facing = new Vector2((random.nextFloat() * 2.0f) - 1.0f, (random.nextFloat() * 2.0f) - 1.0f);

		Entity settler = setterFactory.create(worldPosition, facing, primaryprofession, secondaryProfession, gameContext);

		if (GlobalSettings.DEV_MODE) {
			HaulingComponent haulingComponent = settler.getOrCreateComponent(HaulingComponent.class);

			ItemTypeWithMaterial itemTypeWithMaterial = new ItemTypeWithMaterial();
			if (gameContext.getRandom().nextBoolean()) {
				if (gameContext.getRandom().nextBoolean()) {
					CookingRecipe recipe = cookingRecipeDictionary.getByName("Create soup");
					itemTypeWithMaterial = recipe.getInputItemOptions().get(gameContext.getRandom().nextInt(recipe.getInputItemOptions().size()));
				} else {
					itemTypeWithMaterial.setItemType(itemTypeDictionary.getByName("Fuel-Sack"));
					itemTypeWithMaterial.setMaterial(materialDictionary.getByName("Charcoal"));
				}
			} else {
				itemTypeWithMaterial.setItemType(itemTypeDictionary.getByName("Resource-Grain-Sack"));
				itemTypeWithMaterial.setMaterial(materialDictionary.getByName("Barley"));
			}

			ItemEntityAttributes itemAttributes = new ItemEntityAttributes(gameContext.getRandom().nextLong());
			itemAttributes.setItemType(itemTypeWithMaterial.getItemType());
			itemAttributes.setMaterial(itemTypeWithMaterial.getMaterial());

			for (GameMaterialType otherRequiredMaterialTypes : itemAttributes.getItemType().getMaterialTypes()) {
				if (itemAttributes.getMaterial(otherRequiredMaterialTypes) == null) {
					itemAttributes.setMaterial(pickMaterialType(otherRequiredMaterialTypes));
				}
			}

			itemAttributes.setQuantity(1 + gameContext.getRandom().nextInt(10));
			if (itemAttributes.getQuantity() > itemAttributes.getItemType().getMaxStackSize()) {
				itemAttributes.setQuantity(itemAttributes.getItemType().getMaxStackSize());
			}

			Entity itemEntity = itemEntityFactory.create(itemAttributes, new GridPoint2(tileX, tileY), true, gameContext);
			haulingComponent.setHauledEntity(itemEntity, messageDispatcher, settler);
		}

		return settler;
	}

	private void createResources(int x, int y, ItemType itemType, GameMaterial materialToUse,
								 GameContext gameContext, MessageDispatcher messageDispatcher, Map<GridPoint2, RoomTile> roomTiles, Set<StockpileGroup> stockpileGroups) {
		TiledMap areaMap = gameContext.getAreaMap();

		MapTile tile = areaMap.getTile(x, y);
		if (tile == null) {
			return; // FIXME looks like we're embarking on edge of map
		}
		if (tile.hasWall()) {
			messageDispatcher.dispatchMessage(MessageType.REMOVE_WALL, tile.getTilePosition());
		}
		List<Long> entitiesToRemove = new LinkedList<>();
		for (Entity entity : tile.getEntities()) {
			if (!entity.getType().equals(EntityType.CREATURE)) {
				entitiesToRemove.add(entity.getId());
			}
		}
		// Separate loop to avoid ConcurrentModificationException
		for (Long entityId : entitiesToRemove) {
			entityStore.remove(entityId);
			tile.removeEntity(entityId);
		}

		ItemEntityAttributes itemEntityAttributes = new ItemEntityAttributes(gameContext.getRandom().nextLong());
		itemEntityAttributes.setItemType(itemType);
		itemEntityAttributes.setMaterial(materialToUse);
		itemEntityAttributes.setQuantity(itemType.getMaxStackSize());
		GridPoint2 location = new GridPoint2(x, y);
		entityStore.createResourceItem(itemEntityAttributes, location);
		stockpileGroups.add(itemType.getStockpileGroup());

		RoomTile roomTile = new RoomTile();
		roomTile.setTile(gameContext.getAreaMap().getTile(location));
		roomTile.setTilePosition(location);
		roomTiles.put(location, roomTile);
	}

	private GameMaterial pickWoodMaterialType() {
		List<PlantSpecies> treeSpecies = new ArrayList<>();
		for (PlantSpecies plantSpecies : plantSpeciesDictionary.getAll()) {
			if (plantSpecies.getPlantType().equals(TREE) && plantSpecies.getMaterial().isUseInRandomGeneration()) {
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
		items.addAll(item("Product-Anvil", 1));
		items.addAll(item("Product-Cauldron", 2));
		return items;
	}

	private Deque<QuantifiedItemTypeWithMaterial> initInventoryStartingItems(int numSettlers, Random random) {
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

	private void addAnyHauledItems(Entity settler, Deque<QuantifiedItemTypeWithMaterial> initiallyHauledItems, GameContext gameContext, MessageDispatcher messageDispatcher) {
		if (!initiallyHauledItems.isEmpty()) {
			QuantifiedItemTypeWithMaterial quantifiedItemType = initiallyHauledItems.peek();

			if (quantifiedItemType.getItemType() != null) {
				Entity startingItem = itemEntityFactory.createByItemType(quantifiedItemType.getItemType(), gameContext, true);

				HaulingComponent haulingComponent = settler.getOrCreateComponent(HaulingComponent.class);
				haulingComponent.setHauledEntity(startingItem, messageDispatcher, settler);
			}

			quantifiedItemType.setQuantity(quantifiedItemType.getQuantity() - 1);
			if (quantifiedItemType.getQuantity() <= 0) {
				initiallyHauledItems.pop();
			}
		}
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
