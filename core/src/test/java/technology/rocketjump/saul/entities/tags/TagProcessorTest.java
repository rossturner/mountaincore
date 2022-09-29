package technology.rocketjump.saul.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.saul.entities.model.physical.effect.OngoingEffectTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesDictionary;
import technology.rocketjump.saul.rooms.RoomTypeDictionary;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TagProcessorTest {
    @Mock
    private ItemTypeDictionary itemTypeDictionary;
    @Mock
    private PlantSpeciesDictionary plantSpeciesDictionary;
    @InjectMocks
    private TagProcessingUtils tagProcessingUtils;


    @Test
    public void init_GivenRaceWithRawTags_ProcessesTags() {

        TagDictionary tagDictionary = Mockito.mock(TagDictionary.class);
        MessageDispatcher messageDispatcher = Mockito.mock(MessageDispatcher.class);
        FurnitureTypeDictionary furnitureTypeDictionary = Mockito.mock(FurnitureTypeDictionary.class);
        RoomTypeDictionary roomTypeDictionary = Mockito.mock(RoomTypeDictionary.class);
        OngoingEffectTypeDictionary ongoingEffectTypeDictionary = Mockito.mock(OngoingEffectTypeDictionary.class);
        RaceDictionary raceDictionary = Mockito.mock(RaceDictionary.class);

        TagProcessor tagProcessor = new TagProcessor(tagDictionary, tagProcessingUtils, messageDispatcher, furnitureTypeDictionary, roomTypeDictionary, ongoingEffectTypeDictionary, raceDictionary);

        Race race = new Race();
        List<String> expectedArguments = List.of("MyValue1", "MyValue2");
        race.setTags(Map.of("MY_TAG", expectedArguments));
        when(raceDictionary.getAll()).thenReturn(List.of(race));


        Tag expectedProcessTag = Mockito.mock(Tag.class);
        when(tagDictionary.newInstanceByName("MY_TAG")).thenReturn(expectedProcessTag);
        when(expectedProcessTag.isValid(tagProcessingUtils)).thenReturn(true);


        tagProcessor.init();

        assertThat(race.getProcessedTags())
                .hasSize(1)
                .first()
                .isEqualTo(expectedProcessTag);

        verify(expectedProcessTag).setArgs(expectedArguments);
    }
}