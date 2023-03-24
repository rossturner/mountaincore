package technology.rocketjump.mountaincore.rooms.components;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.GridPoint2;
import com.google.common.collect.Lists;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.Prioritisable;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.SelectableDescription;
import technology.rocketjump.mountaincore.entities.components.ItemAllocation;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.jobs.model.JobPriority;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.persistence.EnumParser;
import technology.rocketjump.mountaincore.persistence.JSONUtils;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.production.AbstractStockpile;
import technology.rocketjump.mountaincore.production.StockpileAllocation;
import technology.rocketjump.mountaincore.production.StockpileGroup;
import technology.rocketjump.mountaincore.production.StockpileSettings;
import technology.rocketjump.mountaincore.rendering.utils.ColorMixer;
import technology.rocketjump.mountaincore.rooms.HaulingAllocation;
import technology.rocketjump.mountaincore.rooms.Room;
import technology.rocketjump.mountaincore.ui.i18n.I18nString;
import technology.rocketjump.mountaincore.ui.i18n.I18nText;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;
import technology.rocketjump.mountaincore.ui.i18n.I18nWord;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class StockpileRoomComponent extends RoomComponent implements SelectableDescription, Prioritisable {

	private final StockpileSettings stockpileSettings;
	private final RoomStockpile stockpile;
	private JobPriority priority = JobPriority.NORMAL;

	public StockpileRoomComponent(Room parent, MessageDispatcher messageDispatcher) {
		this(parent, messageDispatcher, new StockpileSettings());
	}

	private StockpileRoomComponent(Room parent, MessageDispatcher messageDispatcher, StockpileSettings stockpileSettings) {
		super(parent, messageDispatcher);
		this.stockpileSettings = stockpileSettings;
		this.stockpile = new RoomStockpile(parent);
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {

	}

	@Override
	public RoomComponent clone(Room newParent) {

		StockpileRoomComponent cloned = new StockpileRoomComponent(newParent, messageDispatcher, stockpileSettings.clone());

		// Copy over allocations, duplicates will be removed after
		for (Map.Entry<GridPoint2, StockpileAllocation> entry : this.stockpile.getAllocations().entrySet()) {
			cloned.stockpile.getAllocations().put(entry.getKey(), entry.getValue());
		}
		return cloned;
	}

	@Override
	public void mergeFrom(RoomComponent otherComponent) {
		StockpileRoomComponent other = (StockpileRoomComponent) otherComponent;
		for (Map.Entry<GridPoint2, StockpileAllocation> entry : other.stockpile.getAllocations().entrySet()) {
			this.stockpile.getAllocations().put(entry.getKey(), entry.getValue());
		}

		getStockpileSettings().getEnabledGroups().addAll(other.getEnabledGroups());
		getStockpileSettings().getEnabledItemTypes().addAll(other.getStockpileSettings().getEnabledItemTypes());

		for (Map.Entry<ItemType, Set<GameMaterial>> entry : other.getStockpileSettings().getEnabledMaterialsByItemType().entrySet()) {
			getStockpileSettings().getEnabledMaterialsByItemType().put(entry.getKey(), entry.getValue());
		}

		updateColor();
	}

	@Override
	public void tileRemoved(GridPoint2 location) {
		stockpile.getAllocations().remove(location);
		messageDispatcher.dispatchMessage(MessageType.REMOVE_HAULING_JOBS_TO_POSITION, location);
	}

	public AbstractStockpile getStockpile() {
		return stockpile;
	}

	public void itemOrCreaturePickedUp(MapTile targetTile) {
		StockpileAllocation allocationAtTile = getAllocationAt(targetTile.getTilePosition());
		if (allocationAtTile != null) {
			allocationAtTile.refreshQuantityInTile(targetTile);
			if (allocationAtTile.getTotalQuantity() <= 0 && allocationAtTile.getIncomingHaulingQuantity() <= 0) {
				stockpile.getAllocations().remove(targetTile.getTilePosition());
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

			this.stockpile.getAllocations().put(targetTile.getTilePosition(), replacementAllocation);
		}
	}

	//TODO: extract to abstract stockpile
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

			this.stockpile.getAllocations().put(targetTile.getTilePosition(), replacementAllocation);
		}
	}


	//TODO: extract to AbstractStockpile
	public void allocationCancelled(HaulingAllocation allocation, Entity itemEntity) {
		StockpileAllocation positionalAllocation = stockpile.getAllocations().get(allocation.getTargetPosition());
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
			stockpile.getAllocations().remove(allocation.getTargetPosition());
		}
	}

	@Override
	public List<I18nText> getDescription(I18nTranslator i18nTranslator, GameContext gameContext, MessageDispatcher messageDispatcher) {
		int parentSize = parent.getRoomTiles().size();
		int allocationSize = stockpile.getAllocations().size();

		Map<String, I18nString> replacements = new HashMap<>();
		replacements.put("allocated", new I18nWord(String.valueOf(allocationSize)));
		replacements.put("total", new I18nWord(String.valueOf(parentSize)));
		return Lists.newArrayList(i18nTranslator.getTranslatedWordWithReplacements("ROOMS.COMPONENT.STOCKPILE.DESCRIPTION", replacements));
	}

	public StockpileAllocation getAllocationAt(GridPoint2 position) {
		return stockpile.getAllocations().get(position);
	}

	public void updateColor() {
		List<Color> colors = getStockpileSettings().getEnabledGroups().stream().map(StockpileGroup::getColor).collect(Collectors.toList());
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
		JSONObject stockpileSettingsJson = new JSONObject();
		stockpileSettings.writeTo(stockpileSettingsJson, savedGameStateHolder);
		asJson.put("stockpileSettings", stockpileSettingsJson);


		if (!stockpile.getAllocations().isEmpty()) {
			JSONArray allocationsJson = new JSONArray();
			for (Map.Entry<GridPoint2, StockpileAllocation> entry : stockpile.getAllocations().entrySet()) {
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
		JSONObject stockpileSettingsJson = asJson.getJSONObject("stockpileSettings");
		if (stockpileSettingsJson != null) {
			stockpileSettings.readFrom(stockpileSettingsJson, savedGameStateHolder, relatedStores);
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

				stockpile.getAllocations().put(position, allocation);
			}
		}

		this.priority = EnumParser.getEnumValue(asJson, "priority", JobPriority.class, JobPriority.NORMAL);
	}


	public Set<StockpileGroup> getEnabledGroups() {
		return getStockpileSettings().getEnabledGroups();
	}

	public StockpileSettings getStockpileSettings() {
		return stockpileSettings;
	}
}
