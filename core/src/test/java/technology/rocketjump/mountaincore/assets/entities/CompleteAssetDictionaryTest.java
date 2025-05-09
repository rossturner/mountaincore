package technology.rocketjump.mountaincore.assets.entities;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.mountaincore.assets.entities.creature.CreatureEntityAssetDictionary;
import technology.rocketjump.mountaincore.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.furniture.FurnitureEntityAssetDictionary;
import technology.rocketjump.mountaincore.assets.entities.item.ItemEntityAssetDictionary;
import technology.rocketjump.mountaincore.assets.entities.mechanism.MechanismEntityAssetDictionary;
import technology.rocketjump.mountaincore.assets.entities.model.AnimationDictionary;
import technology.rocketjump.mountaincore.assets.entities.plant.PlantEntityAssetDictionary;
import technology.rocketjump.mountaincore.assets.entities.vehicle.VehicleEntityAssetDictionary;
import technology.rocketjump.mountaincore.assets.entities.wallcap.WallCapAssetDictionary;

import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CompleteAssetDictionaryTest {

    @Mock
    private CreatureEntityAssetDictionary creatureDictionary;
    @Mock
    private FurnitureEntityAssetDictionary furnitureDictionary;
    @Mock
    private PlantEntityAssetDictionary plantDictionary;
    @Mock
    private ItemEntityAssetDictionary itemDictionary;
    @Mock
    private WallCapAssetDictionary wallCapDictionary;
    @Mock
    private MechanismEntityAssetDictionary mechanismDictionary;
    @Mock
    private VehicleEntityAssetDictionary vehicleEntityAssetDictionary;
    @Mock
    private AnimationDictionary animationDictionary;

    @Test
    public void rebuild_GivenAssetHasChanged_ReturnsNewAsset() {
        String assetName = "MyCreatureName";
        CreatureEntityAsset firstAsset = Mockito.mock(CreatureEntityAsset.class);
        CreatureEntityAsset secondAsset = Mockito.mock(CreatureEntityAsset.class);
        Map<String, CreatureEntityAsset> firstMap = Map.of(assetName, firstAsset);
        Map<String, CreatureEntityAsset> secondMap = Map.of(assetName, secondAsset);
        when(creatureDictionary.getAll()).thenReturn(firstMap).thenReturn(secondMap);

        CompleteAssetDictionary dictionary = new CompleteAssetDictionary(creatureDictionary, furnitureDictionary, vehicleEntityAssetDictionary, plantDictionary, itemDictionary, wallCapDictionary, mechanismDictionary, animationDictionary);

        assertThat(dictionary.getByUniqueName(assetName)).isSameAs(firstAsset);

        dictionary.rebuild();

        assertThat(dictionary.getByUniqueName(assetName)).isSameAs(secondAsset);
    }
}