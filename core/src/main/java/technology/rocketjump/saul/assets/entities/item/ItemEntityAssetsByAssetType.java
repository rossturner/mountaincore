package technology.rocketjump.saul.assets.entities.item;

import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static technology.rocketjump.saul.assets.entities.model.EntityAssetType.NULL_ENTITY_ASSET_TYPE;

public class ItemEntityAssetsByAssetType {

	private final ItemTypeDictionary itemTypeDictionary;
	private Map<EntityAssetType, ItemEntityAssetsByItemType> typeMap = new HashMap<>();

	public ItemEntityAssetsByAssetType(ItemTypeDictionary itemTypeDictionary) {
		this.itemTypeDictionary = itemTypeDictionary;
		typeMap.put(NULL_ENTITY_ASSET_TYPE, new ItemEntityAssetsByItemType());
	}

	public void add(ItemEntityAsset asset) {
		ItemType itemType = itemTypeDictionary.getByName(asset.getItemTypeName());
		if (itemType == null) {
			Logger.error(asset.getUniqueName() + " asset does not have a valid item type specified");
			return;
		}
		typeMap.computeIfAbsent(asset.getType(), a -> new ItemEntityAssetsByItemType())
				.add(itemType, asset);
		typeMap.get(NULL_ENTITY_ASSET_TYPE).add(itemTypeDictionary.getByName(asset.getItemTypeName()), asset);
	}

	public ItemEntityAsset get(EntityAssetType entityAssetType, ItemEntityAttributes attributes) {
		if (entityAssetType == null) {
			entityAssetType = NULL_ENTITY_ASSET_TYPE;
		}
		ItemEntityAssetsByItemType childMap = typeMap.get(entityAssetType);
		return childMap != null ? childMap.get(attributes) : null;
	}

	public List<ItemEntityAsset> getAll(EntityAssetType entityAssetType, ItemEntityAttributes attributes) {
		ItemEntityAssetsByItemType childMap = typeMap.get(entityAssetType);
		return childMap != null ? childMap.getAll(attributes) : List.of();
	}

}
