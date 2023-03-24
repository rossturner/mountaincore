package technology.rocketjump.mountaincore.entities.ai.goap;

import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;
import technology.rocketjump.mountaincore.guice.MountaincoreGuiceModule;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

public class GoalDictionaryTest {

	private GoalDictionary goalDictionary;
	@Before
	public void setUp() throws Exception {
		Injector injector = Guice.createInjector(new MountaincoreGuiceModule());
		goalDictionary = injector.getInstance(GoalDictionary.class);
	}

	@Test
	public void getAllGoals() throws Exception {
		List<Goal> goals = goalDictionary.getAllGoals();

		assertThat(goals.size()).isGreaterThan(1);

		assertThat(goalDictionary.getByName("Sleep in bed goal").getSelectors()).hasSize(2);
	}

}