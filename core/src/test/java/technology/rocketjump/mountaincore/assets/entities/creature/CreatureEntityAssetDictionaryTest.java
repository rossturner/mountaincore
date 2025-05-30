package technology.rocketjump.mountaincore.assets.entities.creature;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.mountaincore.assets.entities.creature.model.CreatureBodyShape;
import technology.rocketjump.mountaincore.assets.entities.creature.model.CreatureBodyShapeDescriptor;
import technology.rocketjump.mountaincore.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetType;
import technology.rocketjump.mountaincore.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Gender;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Race;
import technology.rocketjump.mountaincore.entities.model.physical.creature.RaceGenderDescriptor;
import technology.rocketjump.mountaincore.jobs.SkillDictionary;

import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class CreatureEntityAssetDictionaryTest {

    @Test
    public void getMatching_GivenDictionaryRebuilds_CanFindByTheUpdatedProperties() {
        CreatureBodyShapeDescriptor bodyShape = new CreatureBodyShapeDescriptor();
        bodyShape.setValue(CreatureBodyShape.AVERAGE);
        Race race = new Race();
        race.setName("MyRace");
        race.setBodyShapes(List.of(bodyShape));
        race.setGenders(Map.of(Gender.MALE, Mockito.mock(RaceGenderDescriptor.class), Gender.FEMALE, Mockito.mock(RaceGenderDescriptor.class)));
        EntityAssetType assetType = new EntityAssetType("MyAssetType");

        CreatureEntityAsset expectedAsset = new CreatureEntityAsset();
        expectedAsset.setUniqueName("MyAssetName");
        expectedAsset.setRace(race);
        expectedAsset.setGender(Gender.MALE);
        expectedAsset.setType(assetType);

        CreatureEntityAttributes attributes = new CreatureEntityAttributes();
        attributes.setRace(race);
        attributes.setGender(Gender.MALE);


        CreatureEntityAssetDictionary dictionary = new CreatureEntityAssetDictionary(List.of(expectedAsset));

        assertThat(dictionary.getMatching(assetType, attributes, SkillDictionary.NULL_PROFESSION)).isSameAs(expectedAsset);

        expectedAsset.setGender(Gender.FEMALE);
        dictionary.rebuild();

        assertThat(dictionary.getMatching(assetType, attributes, SkillDictionary.NULL_PROFESSION)).isNull();

        attributes.setGender(Gender.FEMALE);
        assertThat(dictionary.getMatching(assetType, attributes, SkillDictionary.NULL_PROFESSION)).isSameAs(expectedAsset);
    }

}