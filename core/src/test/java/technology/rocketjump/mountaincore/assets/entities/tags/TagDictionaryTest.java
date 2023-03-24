package technology.rocketjump.mountaincore.assets.entities.tags;

import org.junit.Test;
import technology.rocketjump.mountaincore.entities.tags.Tag;
import technology.rocketjump.mountaincore.entities.tags.TagDictionary;
import technology.rocketjump.mountaincore.misc.ReflectionsService;
import technology.rocketjump.mountaincore.rooms.tags.StockpileTag;

import static org.fest.assertions.Assertions.assertThat;

public class TagDictionaryTest {

	@Test
	public void can_create_new_instances_of_tags() throws ReflectiveOperationException {
		TagDictionary tagDictionary = new TagDictionary(new ReflectionsService(null, null));

		Tag tagInstance = tagDictionary.newInstanceByName("STOCKPILE");

		assertThat(tagInstance).isInstanceOf(StockpileTag.class);
	}

}