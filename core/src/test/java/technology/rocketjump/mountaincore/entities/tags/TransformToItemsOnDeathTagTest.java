package technology.rocketjump.mountaincore.entities.tags;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeDictionary;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class TransformToItemsOnDeathTagTest {
    @Mock
    private ItemTypeDictionary itemTypeDictionary;
    @InjectMocks
    private TagProcessingUtils tagProcessingUtils;


    @Test
    public void getTagName_ReturnsExpectedConstant() {
        assertThat(new TransformToItemsOnDeathTag().getTagName()).isEqualTo("TRANSFORM_TO_ITEMS_ON_DEATH");
    }

    @Test
    public void isValid_ExpectsItemQuantityPairs() {
        TransformToItemsOnDeathTag tag = new TransformToItemsOnDeathTag();
        tag.setArgs(Collections.emptyList());
        assertThat(tag.isValid(tagProcessingUtils)).isTrue();
        ItemType largeBoneType = Mockito.mock(ItemType.class);

        when(itemTypeDictionary.getByName("Resource-Bone-Large")).thenReturn(largeBoneType);

        tag.setArgs(List.of("Resource-Bone-Large"));
        assertThat(tag.isValid(tagProcessingUtils)).isFalse();

        tag.setArgs(List.of("Resource-Bone-Large", "3"));
        assertThat(tag.isValid(tagProcessingUtils)).isTrue();
    }

    @Test
    public void isValid_FailsOnUnrecognisedItemType() {
        TransformToItemsOnDeathTag tag = new TransformToItemsOnDeathTag();
        when(itemTypeDictionary.getByName("Doesnt-Exist")).thenReturn(null);

        tag.setArgs(List.of("Doesnt-Exist", "3"));

        assertThat(tag.isValid(tagProcessingUtils)).isFalse();
    }
}