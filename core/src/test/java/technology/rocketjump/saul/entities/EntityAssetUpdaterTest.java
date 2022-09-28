package technology.rocketjump.saul.entities;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.saul.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.entities.tags.Tag;
import technology.rocketjump.saul.entities.tags.TagProcessor;
import technology.rocketjump.saul.jobs.SkillDictionary;

import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EntityAssetUpdaterTest {
    @Mock
    private EntityAssetTypeDictionary entityAssetTypeDictionary;
    @Mock
    private SkillDictionary skillDictionary;
    @Mock
    private TagProcessor tagProcessor;

    @InjectMocks
    private EntityAssetUpdater entityAssetUpdater;


    @Test
    public void processTags_GivenCreature_CopiesTagsFromRace() {
        Tag tagFromRace = Mockito.mock(Tag.class);
        Entity myEntity = Mockito.mock(Entity.class);
        PhysicalEntityComponent physicalComponent = Mockito.mock(PhysicalEntityComponent.class);
        CreatureEntityAttributes creatureAttributes = Mockito.mock(CreatureEntityAttributes.class);
        Race race = Mockito.mock(Race.class);
        when(myEntity.getPhysicalEntityComponent()).thenReturn(physicalComponent);
        when(physicalComponent.getAttributes()).thenReturn(creatureAttributes);
        when(creatureAttributes.getRace()).thenReturn(race);
        when(race.getProcessedTags()).thenReturn(List.of(tagFromRace));

        entityAssetUpdater.processTags(myEntity);

        verify(myEntity).setTags(Set.of(tagFromRace));
    }
}