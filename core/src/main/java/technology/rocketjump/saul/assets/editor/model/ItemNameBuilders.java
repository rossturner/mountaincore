package technology.rocketjump.saul.assets.editor.model;

import org.apache.commons.lang3.text.WordUtils;
import technology.rocketjump.saul.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.saul.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class ItemNameBuilders {
    public static String buildUniqueNameForAsset(ItemType itemType, ItemEntityAsset asset) {
        EntityAssetType assetType = asset.getType();
        List<ItemPlacement> itemPlacements = asset.getItemPlacements();

        StringJoiner uniqueNameJoiner = new StringJoiner("-");
        uniqueNameJoiner.add(itemType.getItemTypeName());
        if (assetType != null) {
            uniqueNameJoiner.add(assetType.getName());
        }
        Set<ItemPlacement> allItemPlacements = new HashSet<>(List.of(ItemPlacement.values()));
        allItemPlacements.removeAll(itemPlacements);
        if (allItemPlacements.size() == 1) {
            allItemPlacements.stream().findFirst().ifPresent(unusedValue -> {
                uniqueNameJoiner.add("Not_"+unusedValue.name());
            });
        } else if(!itemPlacements.isEmpty() && !allItemPlacements.isEmpty()) {
            String itemPlacementTerm = itemPlacements.stream().map(ItemPlacement::name).collect(Collectors.joining("_"));
            uniqueNameJoiner.add(itemPlacementTerm);
        }

        return WordUtils.capitalizeFully(uniqueNameJoiner.toString(), '_', '-');
    }
}
