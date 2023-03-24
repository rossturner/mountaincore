package technology.rocketjump.mountaincore.assets.editor.model;


import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import technology.rocketjump.mountaincore.assets.entities.furniture.model.FurnitureEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetType;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;

import java.util.Arrays;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static technology.rocketjump.mountaincore.assets.editor.model.FurnitureNameBuildersTest.FurnitureEntityAssetBuilder.asset;

class FurnitureNameBuildersTest {

    private static Stream<Arguments> uniqueNameForAsset() {
        return Stream.of(
            Arguments.of(furniture("BAKERS_WORKTOP"), asset().assetType("ITEM_BASE_LAYER").layout("2x3BL").materials(GameMaterialType.METAL).build(), "Bakers_Worktop-2x3BL-Item_Base_Layer-Metal"),
            Arguments.of(furniture("DUG_GRAVE"), asset().assetType("BASE_LAYER").layout("1x1").materials(GameMaterialType.EARTH, GameMaterialType.BONE).build(), "Dug_Grave-1x1-Base_Layer-Earth-Bone")
        );
    }

    @ParameterizedTest
    @MethodSource("uniqueNameForAsset")
    void buildUniqueNameForAsset(FurnitureType itemType, FurnitureEntityAsset asset, String expectedName) {
        String actualName = FurnitureNameBuilders.buildUniqueNameForAsset(itemType, asset);
        assertThat(actualName).isEqualTo(expectedName);
    }

    private static FurnitureType furniture(String name) {
        FurnitureType furnitureType = new FurnitureType();
        furnitureType.setName(name);
        return furnitureType;
    }

    static class FurnitureEntityAssetBuilder {
        private final FurnitureEntityAsset asset = new FurnitureEntityAsset();
        public static FurnitureEntityAssetBuilder asset() {
            return new FurnitureEntityAssetBuilder();
        }

        private FurnitureEntityAssetBuilder() {}

        public FurnitureEntityAssetBuilder assetType(String entityAssetTypeName) {
            asset.setType(new EntityAssetType(entityAssetTypeName));
            return this;
        }

        public FurnitureEntityAssetBuilder layout(String layout) {
            asset.setFurnitureLayoutName(layout);
            return this;
        }

        public FurnitureEntityAssetBuilder materials(GameMaterialType... materialTypes) {
            asset.setValidMaterialTypes(Arrays.asList(materialTypes));
            return this;
        }


        public FurnitureEntityAsset build() {
            return asset;
        }

    }
}