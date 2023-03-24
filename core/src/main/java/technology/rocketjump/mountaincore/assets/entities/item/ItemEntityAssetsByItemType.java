package technology.rocketjump.mountaincore.assets.entities.item;

import technology.rocketjump.mountaincore.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemEntityAssetsByItemType {

	private Map<ItemType, ItemEntityAssetsByQuality> typeMap = new HashMap<>();

	public void add(ItemType itemType, ItemEntityAsset asset) {
		typeMap.computeIfAbsent(itemType, a -> new ItemEntityAssetsByQuality()).add(asset);
	}

	public ItemEntityAsset get(ItemEntityAttributes attributes) {
		ItemEntityAssetsByQuality childMap = typeMap.get(attributes.getItemType());
		return childMap != null ? childMap.get(attributes) : null;
	}

	public List<ItemEntityAsset> getAll(ItemEntityAttributes attributes) {
		ItemEntityAssetsByQuality childMap = typeMap.get(attributes.getItemType());
		return childMap != null ? childMap.getAll(attributes) : List.of();
	}

}
