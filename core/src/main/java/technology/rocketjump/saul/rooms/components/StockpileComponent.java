package technology.rocketjump.saul.rooms.components;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.RandomXS128;
import com.google.common.collect.Lists;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.behaviour.creature.CorpseBehaviour;
import technology.rocketjump.saul.entities.behaviour.furniture.Prioritisable;
import technology.rocketjump.saul.entities.behaviour.furniture.SelectableDescription;
import technology.rocketjump.saul.entities.components.ItemAllocation;
import technology.rocketjump.saul.entities.components.ItemAllocationComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.model.JobPriority;
import technology.rocketjump.saul.mapping.model.TiledMap;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.persistence.EnumParser;
import technology.rocketjump.saul.persistence.JSONUtils;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.production.StockpileAllocation;
import technology.rocketjump.saul.production.StockpileAllocationResponse;
import technology.rocketjump.saul.production.StockpileGroup;
import technology.rocketjump.saul.rendering.utils.ColorMixer;
import technology.rocketjump.saul.rooms.HaulingAllocation;
import technology.rocketjump.saul.rooms.Room;
import technology.rocketjump.saul.ui.i18n.I18nString;
import technology.rocketjump.saul.ui.i18n.I18nText;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;
import technology.rocketjump.saul.ui.i18n.I18nWord;

import java.util.*;
import java.util.stream.Collectors;

public class StockpileComponent extends RoomComponent implements SelectableDescription, Prioritisable {

	private final Set<StockpileGroup> enabledGroups = new HashSet<>();
	private final Set<ItemType> enabledItemTypes = new HashSet<>();
	private final Map<ItemType, Set<GameMaterial>> enabledMaterialsByItemType = new HashMap<>();

	private boolean acceptingCorpses;
	private final Set<Race> enabledRaceCorpses = new HashSet<>();
	// This keeps track of allocations - null for empty spaces
	private final Map<GridPoint2, StockpileAllocation> allocations = new HashMap<>();
	private JobPriority priority = JobPriority.NORMAL;

	public StockpileComponent(Room parent, MessageDispatcher messageDispatcher) {
		super(parent, messageDispatcher);
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {

	}

	@Override
	public RoomComponent clone(Room newParent) {
		StockpileComponent cloned = new StockpileComponent(newParent, messageDispatcher);
		cloned.enabledGroups.addAll(this.enabledGroups);
		cloned.enabledItemTypes.addAll(this.enabledItemTypes);
		for (Map.Entry<ItemType, Set<GameMaterial>> entry : this.enabledMaterialsByItemType.entrySet()) {
			cloned.enabledMaterialsByItemType.put(entry.getKey(), entry.getValue());
		}

		// Copy over allocations, duplicates will be removed after
		for (Map.Entry<GridPoint2, StockpileAllocation> entry : this.allocations.entrySet()) {
			cloned.allocations.put(entry.getKey(), entry.getValue());
		}
		return cloned;
	}

	@Override
	public void mergeFrom(RoomComponent otherComponent) {
		StockpileComponent other = (StockpileComponent) otherComponent;
		for (Map.Entry<GridPoint2, StockpileAllocation> entry : other.allocations.entrySet()) {
			this.allocations.put(entry.getKey(), entry.getValue());
		}

		this.enabledGroups.addAll(other.enabledGroups);
		this.enabledItemTypes.addAll(other.enabledItemTypes);

		for (Map.Entry<ItemType, Set<GameMaterial>> entry : other.enabledMaterialsByItemType.entrySet()) {
			this.enabledMaterialsByItemType.put(entry.getKey(), entry.getValue());
		}

		updateColor();
	}

	@Override
	public void tileRemoved(GridPoint2 location) {
		allocations.remove(location);
		messageDispatcher.dispatchMessage(MessageType.REMOVE_HAULING_JOBS_TO_POSITION, location);
	}

	public void itemOrCreaturePickedUp(MapTile targetTile) {
		StockpileAllocation allocationAtTile = getAllocationAt(targetTile.getTilePosition());
		if (allocationAtTile != null) {
			allocationAtTile.refreshQuantityInTile(targetTile);
			if (allocationAtTile.getTotalQuantity() <= 0 && allocationAtTile.getIncomingHaulingQuantity() <= 0) {
				allocations.remove(targetTile.getTilePosition());
			}
		}
	}

	public void itemPlaced(MapTile targetTile, ItemEntityAttributes placedItemAttributes, int quantityPlaced) {
		StockpileAllocation existingAllocation = getAllocationAt(targetTile.getTilePosition());
		if (existingAllocation != null && placedItemAttributes.getItemType().equals(existingAllocation.getItemType()) &&
				placedItemAttributes.getPrimaryMaterial().equals(existingAllocation.getGameMaterial())) {
			// Matches existing allocation, cancel incoming hauling and refresh
			existingAllocation.decrementIncomingHaulingQuantity(quantityPlaced);
			existingAllocation.refreshQuantityInTile(targetTile);
		} else {
			// Placed an item which does match the existing allocation
			StockpileAllocation replacementAllocation = new StockpileAllocation(targetTile.getTilePosition());
			replacementAllocation.setGameMaterial(placedItemAttributes.getMaterial(placedItemAttributes.getItemType().getPrimaryMaterialType()));
			replacementAllocation.setItemType(placedItemAttributes.getItemType());
			replacementAllocation.refreshQuantityInTile(targetTile);

			this.allocations.put(targetTile.getTilePosition(), replacementAllocation);
		}
	}

	public void corpsePlaced(MapTile targetTile, CreatureEntityAttributes attributes) {
		StockpileAllocation existingAllocation = getAllocationAt(targetTile.getTilePosition());
		if (existingAllocation != null && attributes.getRace().equals(existingAllocation.getRaceCorpse())) {
			// Matches existing allocation, cancel incoming hauling and refresh
			existingAllocation.decrementIncomingHaulingQuantity(1);
			existingAllocation.refreshQuantityInTile(targetTile);
		} else {
			// Placed an item which does not match the existing allocation
			StockpileAllocation replacementAllocation = new StockpileAllocation(targetTile.getTilePosition());
			replacementAllocation.setRaceCorpse(attributes.getRace());
			replacementAllocation.refreshQuantityInTile(targetTile);

			this.allocations.put(targetTile.getTilePosition(), replacementAllocation);
		}
	}

	// Picks and allocates a position for the item
	public StockpileAllocationResponse requestAllocation(Entity entity, TiledMap map) {
		boolean isItem = entity.getType().equals(EntityType.ITEM);
		boolean isCorpse = entity.getType().equals(EntityType.CREATURE) && entity.getBehaviourComponent() instanceof CorpseBehaviour;
		if (!isItem && !isCorpse) {
			return null;
		}

		ItemType itemType = isItem ? ((ItemEntityAttributes)entity.getPhysicalEntityComponent().getAttributes()).getItemType() : null;
		GameMaterial itemMaterial = isItem ? ((ItemEntityAttributes)entity.getPhysicalEntityComponent().getAttributes()).getPrimaryMaterial() : null;
		Race race = isCorpse ? ((CreatureEntityAttributes)entity.getPhysicalEntityComponent().getAttributes()).getRace() : null;
		final int maxStackSize = isCorpse ? 1 : itemType.getMaxStackSize();

		int numUnallocated = entity.getComponent(ItemAllocationComponent.class).getNumUnallocated();
		int quantityToAllocate = Math.min(numUnallocated, isCorpse ? 1 : itemType.getMaxHauledAtOnce());

		StockpileAllocation allocationToUse = null;

		List<GridPoint2> pointsToTraverse = new ArrayList<>(parent.getRoomTiles().keySet());
		// Randomly traverse to see if we can fit into existing
		Collections.shuffle(pointsToTraverse);
		// First try to find a matching allocation
		for (GridPoint2 position : pointsToTraverse) {
			MapTile tileAtPosition = map.getTile(position);
			StockpileAllocation allocationAtPosition = allocations.get(position);
			if (allocationAtPosition == null) {
				// No allocation here yet
				if (!tileAtPosition.isEmpty()) {
					Entity itemAlreadyInTile = tileAtPosition.getFirstItem();
					if (itemAlreadyInTile != null) {
						// There is already an item here but no existing allocation, so add a new allocation matching it
						// This is for pre-existing items where a stockpile is placed
						allocationAtPosition = new StockpileAllocation(position);
						ItemEntityAttributes attributesItemAlreadyInTile = (ItemEntityAttributes) itemAlreadyInTile.getPhysicalEntityComponent().getAttributes();

						allocationAtPosition.setGameMaterial(attributesItemAlreadyInTile.getPrimaryMaterial());
						allocationAtPosition.setItemType(attributesItemAlreadyInTile.getItemType());
						allocationAtPosition.refreshQuantityInTile(tileAtPosition);
						allocations.put(position, allocationAtPosition);
						continue;
					}

					Entity corpseEntity = tileAtPosition.getFirstCorpse();
					if (corpseEntity != null) {
						allocationAtPosition = new StockpileAllocation(position);
						allocationAtPosition.setRaceCorpse(((CreatureEntityAttributes)corpseEntity.getPhysicalEntityComponent().getAttributes()).getRace());
						allocationAtPosition.refreshQuantityInTile(tileAtPosition);
						allocations.put(position, allocationAtPosition);
						continue;
					}
				}
			} else if (matches(entity, allocationAtPosition) &&
					allocationAtPosition.getTotalQuantity() + quantityToAllocate <= maxStackSize &&
					allocationIsCorrectForTileContents(tileAtPosition, allocationAtPosition)) {
				allocationToUse = allocationAtPosition;
				break;
			}
		}
		if (allocationToUse == null) {
			// Not found one yet so use a new allocation

			// Deterministically go through points to traverse for a new location
			pointsToTraverse = new ArrayList<>(parent.getRoomTiles().keySet());
			Random random = new RandomXS128(parent.getRoomId());
			Collections.shuffle(pointsToTraverse, random);

			for (GridPoint2 position : pointsToTraverse) {
				MapTile tileAtPosition = map.getTile(position);
				StockpileAllocation allocationAtPosition = allocations.get(position);
				if (allocationAtPosition == null) {
					if (tileAtPosition.isEmpty()) {
						allocationToUse = new StockpileAllocation(position);
						allocationToUse.setItemType(itemType);
						allocationToUse.setGameMaterial(itemMaterial);
						allocationToUse.setRaceCorpse(race);
						allocations.put(position, allocationToUse);
						break;
					}
				}
			}
		}

		if (allocationToUse != null) {
			int spaceInAllocation = maxStackSize - allocationToUse.getTotalQuantity();
			if (quantityToAllocate == 0) {
				Logger.error("Quantity to requestAllocation in " + this.getClass().getSimpleName() + " is 0, investigate why");
				return null;
			}
			quantityToAllocate = Math.min(quantityToAllocate, spaceInAllocation);

			allocationToUse.incrementIncomingHaulingQuantity(quantityToAllocate);

			return new StockpileAllocationResponse(allocationToUse.getPosition(), quantityToAllocate);
		}

		return null;
	}

	private boolean matches(Entity entity, StockpileAllocation stockpileAllocation) {
		if (entity.getType().equals(EntityType.CREATURE)) {
			return ((CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes()).getRace().equals(stockpileAllocation.getRaceCorpse());
		} else if (entity.getType().equals(EntityType.ITEM)) {
			ItemEntityAttributes attributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
			return attributes.getItemType().equals(stockpileAllocation.getItemType()) &&
					attributes.getPrimaryMaterial().equals(stockpileAllocation.getGameMaterial());
		} else {
			return false;
		}
	}

	private boolean allocationIsCorrectForTileContents(MapTile tileAtPosition, StockpileAllocation allocationAtPosition) {
		Entity itemAtPosition = null;
		Entity corpseAtPosition = null;
		for (Entity entity : tileAtPosition.getEntities()) {
			if (entity.getType().equals(EntityType.PLANT)) {
				return false; // a plant has grown into the tile
			}
			if (entity.getType().equals(EntityType.ITEM)) {
				itemAtPosition = entity;
				break;
			}
			if (entity.getType().equals(EntityType.CREATURE) && entity.getBehaviourComponent() instanceof CorpseBehaviour) {
				corpseAtPosition = entity;
				break;
			}
		}

		if (itemAtPosition == null && corpseAtPosition == null) {
			return true; // nothing here so can place allocation
		} else if (corpseAtPosition != null) {
			return allocationAtPosition.getRaceCorpse() != null && allocationAtPosition.getRaceCorpse().equals(((CreatureEntityAttributes) corpseAtPosition.getPhysicalEntityComponent().getAttributes()).getRace());
		} else {
			ItemEntityAttributes attributes = (ItemEntityAttributes) itemAtPosition.getPhysicalEntityComponent().getAttributes();
			return attributes.getItemType().equals(allocationAtPosition.getItemType()) &&
					attributes.getPrimaryMaterial().equals(allocationAtPosition.getGameMaterial());
		}
	}

	public void allocationCancelled(HaulingAllocation allocation, Entity itemEntity) {
		StockpileAllocation positionalAllocation = allocations.get(allocation.getTargetPosition());
		if (positionalAllocation == null) {
			// Stockpile must have been removed
			return;
		}

		ItemEntityAttributes attributes = (ItemEntityAttributes) itemEntity.getPhysicalEntityComponent().getAttributes();
		if (!attributes.getPrimaryMaterial().equals(positionalAllocation.getGameMaterial()) ||
				!attributes.getItemType().equals(positionalAllocation.getItemType())) {
			// Allocation is not the correct item type or material
			return;
		}

		ItemAllocation itemAllocation = allocation.getItemAllocation();
		positionalAllocation.decrementIncomingHaulingQuantity(itemAllocation.getAllocationAmount());
		if (positionalAllocation.getTotalQuantity() <= 0) {
			allocations.remove(allocation.getTargetPosition());
		}
	}

	@Override
	public List<I18nText> getDescription(I18nTranslator i18nTranslator, GameContext gameContext, MessageDispatcher messageDispatcher) {
		int parentSize = parent.getRoomTiles().size();
		int allocationSize = allocations.size();

		Map<String, I18nString> replacements = new HashMap<>();
		replacements.put("allocated", new I18nWord(String.valueOf(allocationSize)));
		replacements.put("total", new I18nWord(String.valueOf(parentSize)));
		return Lists.newArrayList(i18nTranslator.getTranslatedWordWithReplacements("ROOMS.COMPONENT.STOCKPILE.DESCRIPTION", replacements));
	}

	public StockpileAllocation getAllocationAt(GridPoint2 position) {
		return allocations.get(position);
	}

	public void toggleGroup(StockpileGroup group, boolean enabled) {
		if (enabled) {
			enabledGroups.add(group);
		} else {
			enabledGroups.remove(group);
		}
		updateColor();
	}

	public void toggleItem(ItemType itemType, boolean enabled) {
		if (enabled) {
			enabledItemTypes.add(itemType);
		} else {
			enabledItemTypes.remove(itemType);
		}
	}

	public void toggleAcceptingCorpses(boolean enabled) {
		this.acceptingCorpses = enabled;
	}

	public boolean isAcceptingCorpses() {
		return acceptingCorpses;
	}

	public void toggleRaceCorpse(Race race, boolean enabled) {
		if (enabled) {
			enabledRaceCorpses.add(race);
		} else {
			enabledRaceCorpses.remove(race);
		}
	}

	public void toggleMaterial(ItemType itemType, GameMaterial gameMaterial, boolean enabled) {
		Set<GameMaterial> materials = enabledMaterialsByItemType.computeIfAbsent(itemType, a -> new LinkedHashSet<>());

		if (enabled) {
			materials.add(gameMaterial);
		} else {
			materials.remove(gameMaterial);

			if (materials.isEmpty()) {
				enabledMaterialsByItemType.remove(itemType);
			}
		}
	}

	public boolean isEnabled(StockpileGroup group) {
		return enabledGroups.contains(group);
	}

	public boolean isEnabled(ItemType itemType) {
		return enabledItemTypes.contains(itemType);
	}

	public boolean isEnabled(Race race) {
		return enabledRaceCorpses.contains(race);
	}

	public boolean isEnabled(GameMaterial material, ItemType itemType) {
		return enabledMaterialsByItemType.getOrDefault(itemType, Collections.emptySet()).contains(material);
	}

	public boolean canHold(Entity entity) {
		if (entity.getType().equals(EntityType.ITEM)) {
			ItemEntityAttributes itemAttributes = (ItemEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
			return enabledItemTypes.contains(itemAttributes.getItemType()) &&
					enabledMaterialsByItemType.getOrDefault(itemAttributes.getItemType(), Collections.emptySet()).contains(itemAttributes.getPrimaryMaterial());
		} else if (entity.getType().equals(EntityType.CREATURE) && entity.getBehaviourComponent() instanceof CorpseBehaviour) {
			return enabledRaceCorpses.contains(((CreatureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes()).getRace());
		} else {
			return false;
		}
	}

	private void updateColor() {
		List<Color> colors = enabledGroups.stream().map(StockpileGroup::getColor).collect(Collectors.toList());
		parent.setBorderColor(ColorMixer.averageBlend(colors));
	}

	@Override
	public JobPriority getPriority() {
		return this.priority;
	}

	@Override
	public void setPriority(JobPriority jobPriority) {
		this.priority = jobPriority;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		JSONArray enabledGroupsJson = new JSONArray();
		for (StockpileGroup enabledGroup : this.enabledGroups) {
			enabledGroupsJson.add(enabledGroup.getName());
		}
		if (acceptingCorpses) {
			enabledGroupsJson.add("CORPSES");
		}
		asJson.put("enabledGroups", enabledGroupsJson);

		JSONArray enabledItemTypesJson = new JSONArray();
		for (ItemType enabledItemType : this.enabledItemTypes) {
			enabledItemTypesJson.add(enabledItemType.getItemTypeName());
		}
		asJson.put("enabledItemTypes", enabledItemTypesJson);

		JSONArray enabledRacesJson = new JSONArray();
		for (Race race : this.enabledRaceCorpses) {
			enabledRacesJson.add(race.getName());
		}
		asJson.put("enabledRaces", enabledRacesJson);


		JSONObject materialMappingJson = new JSONObject(true);
		for (Map.Entry<ItemType, Set<GameMaterial>> entry : this.enabledMaterialsByItemType.entrySet()) {
			JSONArray materialNames = new JSONArray();
			for (GameMaterial material : entry.getValue()) {
				materialNames.add(material.getMaterialName());
			}
			materialMappingJson.put(entry.getKey().getItemTypeName(), materialNames);
		}
		asJson.put("enabledMaterials", materialMappingJson);


		if (!allocations.isEmpty()) {
			JSONArray allocationsJson = new JSONArray();
			for (Map.Entry<GridPoint2, StockpileAllocation> entry : allocations.entrySet()) {
				JSONObject entryJson = new JSONObject(true);
				entryJson.put("position", JSONUtils.toJSON(entry.getKey()));
				if (entry.getValue() != null) {
					JSONObject allocationJson = new JSONObject(true);
					entry.getValue().writeTo(allocationJson, savedGameStateHolder);
					entryJson.put("allocation", allocationJson);
				}
				allocationsJson.add(entryJson);
			}
			asJson.put("allocations", allocationsJson);
		}

		if (!priority.equals(JobPriority.NORMAL)) {
			asJson.put("priority", priority.name());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		JSONArray enabledGroupsJson = asJson.getJSONArray("enabledGroups");
		if (enabledGroupsJson != null) {
			for (Object item : enabledGroupsJson) {
				if (item.toString().equals("CORPSES")) {
					acceptingCorpses = true;
				} else {
					StockpileGroup group = relatedStores.stockpileGroupDictionary.getByName(item.toString());
					if (group == null) {
						throw new InvalidSaveException("Could not find stockpile group with name " + item.toString());
					} else {
						this.enabledGroups.add(group);
					}
				}
			}
		}

		JSONArray enabledItemTypesJson = asJson.getJSONArray("enabledItemTypes");
		if (enabledItemTypesJson != null) {
			for (Object item : enabledItemTypesJson) {
				ItemType itemType = relatedStores.itemTypeDictionary.getByName(item.toString());
				if (itemType == null) {
					throw new InvalidSaveException("Could not find itemType with name " + item.toString());
				} else {
					this.enabledItemTypes.add(itemType);
				}
			}
		}

		JSONArray enabledRacesJson = asJson.getJSONArray("enabledRaces");
		if (enabledRacesJson != null) {
			for (Object item : enabledRacesJson) {
				Race race = relatedStores.raceDictionary.getByName(item.toString());
				if (race == null) {
					throw new InvalidSaveException("Could not find race with name " + item.toString());
				} else {
					this.enabledRaceCorpses.add(race);
				}
			}
		}

		JSONObject materialMappingJson = asJson.getJSONObject("enabledMaterials");
		if (materialMappingJson != null) {
			for (String itemNameKey : materialMappingJson.keySet()) {
				JSONArray materialNames = materialMappingJson.getJSONArray(itemNameKey);
				ItemType itemType = relatedStores.itemTypeDictionary.getByName(itemNameKey);
				if (itemType == null) {
					throw new InvalidSaveException("Could not find itemType with name " + itemNameKey);
				}
				Set<GameMaterial> materials = new HashSet<>();
				for (Object materialName : materialNames) {
					GameMaterial material = relatedStores.gameMaterialDictionary.getByName(materialName.toString());
					if (material == null) {
						throw new InvalidSaveException("Could not find material with name " + materialName.toString());
					}
					materials.add(material);
				}
				this.enabledMaterialsByItemType.put(itemType, materials);
			}
		}

		JSONArray allocationsJson = asJson.getJSONArray("allocations");
		if (allocationsJson != null) {
			for (int cursor = 0; cursor < allocationsJson.size(); cursor++) {
				JSONObject entryJson = allocationsJson.getJSONObject(cursor);
				GridPoint2 position = JSONUtils.gridPoint2(entryJson.getJSONObject("position"));
				StockpileAllocation allocation = null;
				JSONObject allocationJson = entryJson.getJSONObject("allocation");
				if (allocationJson != null) {
					allocation = new StockpileAllocation(position);
					allocation.readFrom(allocationJson, savedGameStateHolder, relatedStores);
				}

				allocations.put(position, allocation);
			}
		}

		this.priority = EnumParser.getEnumValue(asJson, "priority", JobPriority.class, JobPriority.NORMAL);
	}

	public Room getParent() {
		return parent;
	}

	public Set<StockpileGroup> getEnabledGroups() {
		return enabledGroups;
	}
}
