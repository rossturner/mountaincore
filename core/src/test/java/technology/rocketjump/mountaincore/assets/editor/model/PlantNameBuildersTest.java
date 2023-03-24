package technology.rocketjump.mountaincore.assets.editor.model;


import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetType;
import technology.rocketjump.mountaincore.assets.entities.plant.model.PlantEntityAsset;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantSpecies;

import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static technology.rocketjump.mountaincore.assets.editor.model.PlantNameBuildersTest.PlantEntityAssetBuilder.asset;

class PlantNameBuildersTest {

    private static Stream<Arguments> uniqueNameForAsset() {
        return Stream.of(
            Arguments.of(plant("OAK_TREE"), asset().assetType("PLANT_LEAVES").growthStages(0).build(), "Oak_Tree-Plant_Leaves-0"),
            Arguments.of(plant("BARLEY"), asset().assetType("PLANT_BRANCHES").growthStages(2, 3, 4, 5, 7, 9).build(), "Barley-Plant_Branches-2_3_4_5_7_9")
        );
    }

    @ParameterizedTest
    @MethodSource("uniqueNameForAsset")
    void buildUniqueNameForAsset(PlantSpecies plantSpecies, PlantEntityAsset asset, String expectedName) {
        String actualName = PlantNameBuilders.buildUniqueNameForAsset(plantSpecies, asset);
        assertThat(actualName).isEqualTo(expectedName);
    }

    private static PlantSpecies plant(String name) {
        PlantSpecies plantSpecies = new PlantSpecies();
        plantSpecies.setSpeciesName(name);
        return plantSpecies;
    }

    static class PlantEntityAssetBuilder {
        private final PlantEntityAsset asset = new PlantEntityAsset();
        public static PlantEntityAssetBuilder asset() {
            return new PlantEntityAssetBuilder();
        }

        private PlantEntityAssetBuilder() {}

        public PlantEntityAssetBuilder assetType(String entityAssetTypeName) {
            asset.setType(new EntityAssetType(entityAssetTypeName));
            return this;
        }

        public PlantEntityAssetBuilder growthStages(Integer... growthStages) {
            asset.setGrowthStages(Set.of(growthStages));
            return this;
        }


        public PlantEntityAsset build() {
            return asset;
        }

    }
}