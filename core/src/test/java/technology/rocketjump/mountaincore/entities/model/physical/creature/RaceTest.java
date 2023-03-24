package technology.rocketjump.mountaincore.entities.model.physical.creature;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RaceTest {

    @Test
    void deserialise_GivenRawTags_PopulatesObjectCorrectly() throws IOException {
        ObjectMapper objectMapper = new ObjectMapper();
        String json = """
                {
                  "name" : "Undead Dwarf",
                  "i18nKey" : "RACE.UNDEAD_DWARF",
                  "minStrength" : 2.0,
                  "maxStrength" : 17.0,
                  "tags": {
                    "MY_TAG": ["Value1", "Value2"]
                  }
                }
                """;

        Race race = objectMapper.readValue(json, Race.class);

        assertThat(race)
                .extracting(Race::getName, Race::getMinStrength)
                .contains("Undead Dwarf", 2.0f);

        assertThat(race.getTags()).containsEntry("MY_TAG", List.of("Value1", "Value2"));
    }
}