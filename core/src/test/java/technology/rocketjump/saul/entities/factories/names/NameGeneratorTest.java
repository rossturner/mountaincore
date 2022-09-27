package technology.rocketjump.saul.entities.factories.names;

import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;
import technology.rocketjump.saul.entities.model.physical.creature.HumanoidName;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import static org.fest.assertions.Assertions.assertThat;
import static technology.rocketjump.saul.entities.model.physical.creature.Gender.FEMALE;
import static technology.rocketjump.saul.entities.model.physical.creature.Gender.MALE;

public class NameGeneratorTest {

	private NameGenerator nameGenerator;

	@Before
	public void setup() {
		Injector injector = Guice.createInjector();
		nameGenerator = injector.getInstance(NameGenerator.class);
	}

	@Test
	public void test_createsNonEmptyName() {
		HumanoidName name = nameGenerator.create("Dwarven", 1L, MALE);

		assertThat(name.getFirstName()).isNotEmpty();
		assertThat(name.getLastName()).isNotEmpty();
	}

	@Test
	public void bigNamePrintoutTest() {
		Random random = new RandomXS128();
		for (int i = 0; i < 100; i++) {
			System.out.println(nameGenerator.create("Orcish", random.nextLong(), MALE));
		}
	}

	@Test
	public void alliterationTest() {
		List<HumanoidName> allNames = new LinkedList<>();
		int alliterative = 0;

		Random random = new RandomXS128();
		int total = 10000;
		for (int i = 0; i < total; i++) {
			HumanoidName name = nameGenerator.create("Dwarven", random.nextLong(), random.nextBoolean() ? MALE : FEMALE);

			String firstLetter = name.getFirstName().substring(0, 1);
			if (name.getLastName().startsWith(firstLetter)) {
				alliterative++;
			}
		}

		float percentage = Math.round((float)alliterative) / ((float)total) * 100;

		System.out.println(alliterative + " of " + total + " are alliterative - " + percentage + "%");
	}
}