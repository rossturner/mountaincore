package technology.rocketjump.saul.assets.entities.item;

import technology.rocketjump.saul.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.saul.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;

import java.util.EnumMap;
import java.util.List;

public class ItemEntityAssetsByPlacement {

	private EnumMap<ItemPlacement, ItemEntityAssetsByMaterial> byItemPlacement = new EnumMap<>(ItemPlacement.class);

	public void add(ItemEntityAsset asset) {
		List<ItemPlacement> itemPlacements = asset.getItemPlacements();
		if (itemPlacements == null || itemPlacements.isEmpty()) {
			// Not specified, so add to all
			for (ItemPlacement itemPlacement : ItemPlacement.values()) {
				byItemPlacement.computeIfAbsent(itemPlacement, a -> new ItemEntityAssetsByMaterial()).add(asset);
			}
		} else {
			for (ItemPlacement itemPlacement : itemPlacements) {
				byItemPlacement.computeIfAbsent(itemPlacement, a -> new ItemEntityAssetsByMaterial()).add(asset);
			}
		}
	}

	public ItemEntityAsset get(ItemEntityAttributes attributes) {
		ItemPlacement itemPlacement = attributes.getItemPlacement();
		if (itemPlacement == null) {
			itemPlacement = ItemPlacement.ON_GROUND;
		}
		ItemEntityAssetsByMaterial childMap = byItemPlacement.get(itemPlacement);
		return childMap != null ? childMap.get(attributes) : null;
	}

	public List<ItemEntityAsset> getAll(ItemEntityAttributes attributes) {
		ItemPlacement itemPlacement = attributes.getItemPlacement();
		if (itemPlacement == null) {
			itemPlacement = ItemPlacement.ON_GROUND;
		}
		ItemEntityAssetsByMaterial childMap = byItemPlacement.get(itemPlacement);
		return childMap != null ? childMap.getAll(attributes) : List.of();
	}

}
