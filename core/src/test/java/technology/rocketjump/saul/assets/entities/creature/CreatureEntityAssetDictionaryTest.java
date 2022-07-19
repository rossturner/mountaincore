package technology.rocketjump.saul.assets.entities.creature;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.saul.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShape;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShapeDescriptor;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.entities.model.physical.creature.*;
import technology.rocketjump.saul.jobs.ProfessionDictionary;

import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CreatureEntityAssetDictionaryTest {

    @Mock
    private EntityAssetTypeDictionary entityAssetTypeDictionary;
    @Mock
    private RaceDictionary raceDictionary;

    @Test
    public void getMatching_GivenDictionaryRebuilds_CanFindByTheUpdatedProperties() {
        CreatureBodyShapeDescriptor bodyShape = new CreatureBodyShapeDescriptor();
        bodyShape.setValue(CreatureBodyShape.AVERAGE);
        Race race = new Race();
        race.setName("MyRace");
        race.setBodyShapes(List.of(bodyShape));
        race.setGenders(Map.of(Gender.MALE, Mockito.mock(RaceGenderDescriptor.class), Gender.FEMALE, Mockito.mock(RaceGenderDescriptor.class)));
        when(raceDictionary.getAll()).thenReturn(List.of(race));
        EntityAssetType assetType = new EntityAssetType("MyAssetType");
        when(entityAssetTypeDictionary.getAll()).thenReturn(List.of(assetType));

        CreatureEntityAsset expectedAsset = new CreatureEntityAsset();
        expectedAsset.setUniqueName("MyAssetName");
        expectedAsset.setRace(race);
        expectedAsset.setGender(Gender.MALE);
        expectedAsset.setType(assetType);

        CreatureEntityAttributes attributes = new CreatureEntityAttributes();
        attributes.setRace(race);
        attributes.setGender(Gender.MALE);


        CreatureEntityAssetDictionary dictionary = new CreatureEntityAssetDictionary(List.of(expectedAsset), entityAssetTypeDictionary, raceDictionary);

        assertThat(dictionary.getMatching(assetType, attributes, ProfessionDictionary.NULL_PROFESSION)).isSameAs(expectedAsset);

        expectedAsset.setGender(Gender.FEMALE);
        dictionary.rebuild();

        assertThat(dictionary.getMatching(assetType, attributes, ProfessionDictionary.NULL_PROFESSION)).isNull();

        attributes.setGender(Gender.FEMALE);
        assertThat(dictionary.getMatching(assetType, attributes, ProfessionDictionary.NULL_PROFESSION)).isSameAs(expectedAsset);
    }

}