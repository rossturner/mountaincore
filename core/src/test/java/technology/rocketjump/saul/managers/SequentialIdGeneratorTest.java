package technology.rocketjump.saul.managers;

import org.junit.Test;
import technology.rocketjump.saul.entities.SequentialIdGenerator;

import static org.fest.assertions.Assertions.assertThat;


public class SequentialIdGeneratorTest {

    @Test
    public void nextId_returnsAscendingIds() {
        assertThat(SequentialIdGenerator.nextId()).isEqualTo(SequentialIdGenerator.nextId() - 1);
    }
}