package technology.rocketjump.mountaincore.assets.editor.model;


import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import technology.rocketjump.mountaincore.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static technology.rocketjump.mountaincore.assets.editor.model.ItemNameBuildersTest.ItemEntityAssetBuilder.asset;

class ItemNameBuildersTest {

    private static Stream<Arguments> uniqueNameForAsset() {
        return Stream.of(
            Arguments.of(itemType("Fuel-Sack"), asset().assetType("ITEM_BASE_LAYER").build(), "Fuel-Sack-Item_Base_Layer"),
            Arguments.of(itemType("Tool-Chisel"), asset().assetType("ABOVE_LAYER_1").build(), "Tool-Chisel-Above_Layer_1"),
            Arguments.of(itemType("Weapon-Bow"), asset().itemPlacements(ItemPlacement.BEING_CARRIED).assetType("ABOVE_LAYER_1").build(), "Weapon-Bow-Above_Layer_1-Being_Carried"),
            Arguments.of(itemType("Product-Linen"), asset().itemPlacements(ItemPlacement.BEING_CARRIED, ItemPlacement.PROJECTILE).assetType("ABOVE_LAYER_1").build(), "Product-Linen-Above_Layer_1-Not_On_Ground")
        );
    }

    @ParameterizedTest
    @MethodSource("uniqueNameForAsset")
    void buildUniqueNameForAsset(ItemType itemType, ItemEntityAsset asset, String expectedName) {
        String actualName = ItemNameBuilders.buildUniqueNameForAsset(itemType, asset);
        assertThat(actualName).isEqualTo(expectedName);
    }

    private static ItemType itemType(String name) {
        ItemType itemType = new ItemType();
        itemType.setItemTypeName(name);
        return itemType;
    }

    static class ItemEntityAssetBuilder {
        private final ItemEntityAsset asset = new ItemEntityAsset();
        public static ItemEntityAssetBuilder asset() {
            return new ItemEntityAssetBuilder();
        }

        private ItemEntityAssetBuilder() {}

        public ItemEntityAssetBuilder assetType(String entityAssetTypeName) {
            asset.setType(new EntityAssetType(entityAssetTypeName));
            return this;
        }

        public ItemEntityAssetBuilder itemPlacements(ItemPlacement... placements) {
            asset.setItemPlacements(List.of(placements));
            return this;
        }

        public ItemEntityAsset build() {
            return asset;
        }

    }
}