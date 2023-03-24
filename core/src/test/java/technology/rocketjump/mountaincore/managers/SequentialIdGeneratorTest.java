package technology.rocketjump.mountaincore.managers;

import org.junit.Test;
import technology.rocketjump.mountaincore.entities.SequentialIdGenerator;

import static org.fest.assertions.Assertions.assertThat;


public class SequentialIdGeneratorTest {

    @Test
    public void nextId_returnsAscendingIds() {
        assertThat(SequentialIdGenerator.nextId()).isEqualTo(SequentialIdGenerator.nextId() - 1);
    }
}