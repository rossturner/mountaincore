package technology.rocketjump.saul.assets.entities.item;

import technology.rocketjump.saul.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemEntityAssetsByItemType {

	private Map<ItemType, ItemEntityAssetsByQuality> typeMap = new HashMap<>();

	public ItemEntityAssetsByItemType(ItemTypeDictionary itemTypeDictionary) {
		for (ItemType itemType : itemTypeDictionary.getAll()) {
			typeMap.put(itemType, new ItemEntityAssetsByQuality());
		}
	}

	public void add(ItemType itemType, ItemEntityAsset asset) {
		// Assuming all entities have a type specified
		typeMap.get(itemType).add(asset);
	}

	public ItemEntityAsset get(ItemEntityAttributes attributes) {
		return typeMap.get(attributes.getItemType()).get(attributes);
	}

	public List<ItemEntityAsset> getAll(ItemEntityAttributes attributes) {
		return typeMap.get(attributes.getItemType()).getAll(attributes);
	}

	public ItemEntityAssetsByQuality getQualityMapByItemType(ItemType itemType) {
		return typeMap.get(itemType);
	}
}
