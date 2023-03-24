package technology.rocketjump.mountaincore.entities.factories;

import com.google.inject.Guice;
import org.fest.assertions.Assertions;
import org.junit.Before;
import org.junit.Test;
import technology.rocketjump.mountaincore.entities.factories.names.SettlementNameGenerator;

import java.io.IOException;


public class SettlementNameGeneratorTest {

	private SettlementNameGenerator settlementNameGenerator;

	@Before
	public void setup() throws IOException {
		settlementNameGenerator = Guice.createInjector().getInstance(SettlementNameGenerator.class);
	}

	@Test
	public void simpleTest() {
		Assertions.assertThat(settlementNameGenerator.create(1L)).isEqualTo("Vulgarden");

//		Random random = new RandomXS128();
//		for (int i = 0; i < 100; i++) {
//			System.out.println(settlementNameGenerator.create(random.nextLong()));
//		}

	}

}