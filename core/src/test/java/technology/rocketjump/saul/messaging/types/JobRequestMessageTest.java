package technology.rocketjump.saul.messaging.types;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.environment.GameClock;
import technology.rocketjump.saul.materials.model.PersistenceTestHarness;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JobRequestMessageTest extends PersistenceTestHarness {

	@Test
	public void testPersistence() throws InvalidSaveException {
		Entity mockEntity = Mockito.mock(Entity.class);

		GameClock mockGameClock = Mockito.mock(GameClock.class);
		when(mockGameClock.getCurrentGameTime()).thenReturn(4D);

		JobRequestMessage original = new JobRequestMessage(mockEntity, mockGameClock, null);

		original.writeTo(stateHolder);

		JobRequestMessage recreated = new JobRequestMessage();
		recreated.readFrom(stateHolder.jobRequestsJson.getJSONObject(0), stateHolder, dictionaries);

		assertThat(recreated.getRequestId()).isEqualTo(original.getRequestId());
		assertThat(recreated.getRequestedAtTime()).isEqualTo(original.getRequestedAtTime());
	}

}