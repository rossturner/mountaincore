package technology.rocketjump.saul.assets.entities.tags;

import org.junit.Test;
import technology.rocketjump.saul.entities.tags.Tag;
import technology.rocketjump.saul.entities.tags.TagDictionary;
import technology.rocketjump.saul.rooms.tags.StockpileTag;

import static org.fest.assertions.Assertions.assertThat;

public class TagDictionaryTest {

	@Test
	public void can_create_new_instances_of_tags() throws ReflectiveOperationException {
		TagDictionary tagDictionary = new TagDictionary();

		Tag tagInstance = tagDictionary.newInstanceByName("STOCKPILE");

		assertThat(tagInstance).isInstanceOf(StockpileTag.class);
	}

}