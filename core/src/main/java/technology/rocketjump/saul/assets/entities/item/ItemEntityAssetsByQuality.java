package technology.rocketjump.saul.assets.entities.item;

import technology.rocketjump.saul.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemQuality;

import java.util.EnumMap;
import java.util.List;

public class ItemEntityAssetsByQuality {

	private EnumMap<ItemQuality, ItemEntityAssetsByPlacement> byItemQuality = new EnumMap<>(ItemQuality.class);

	public void add(ItemEntityAsset asset) {
		List<ItemQuality> itemQualities = asset.getItemQualities();
		if (itemQualities == null || itemQualities.isEmpty()) {
			// Add to all
			for (ItemQuality itemQuality : ItemQuality.values()) {
				byItemQuality.computeIfAbsent(itemQuality, a -> new ItemEntityAssetsByPlacement()).add(asset);
			}
		} else {
			itemQualities.forEach(iq -> byItemQuality.computeIfAbsent(iq, a -> new ItemEntityAssetsByPlacement()).add(asset));
		}
	}

	public ItemEntityAsset get(ItemEntityAttributes attributes) {
		ItemQuality itemQuality = attributes.getItemQuality();
		if (itemQuality == null) {
			itemQuality = ItemQuality.STANDARD;
		}
		ItemEntityAssetsByPlacement childMap = byItemQuality.get(itemQuality);
		return childMap != null ? childMap.get(attributes) : null;
	}

	public List<ItemEntityAsset> getAll(ItemEntityAttributes attributes) {
		ItemQuality itemQuality = attributes.getItemQuality();
		if (itemQuality == null) {
			itemQuality = ItemQuality.STANDARD;
		}
		ItemEntityAssetsByPlacement childMap = byItemQuality.get(itemQuality);
		return childMap != null ? childMap.getAll(attributes) : List.of();
	}

}
