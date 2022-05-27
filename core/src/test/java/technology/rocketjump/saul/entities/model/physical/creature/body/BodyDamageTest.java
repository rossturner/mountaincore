package technology.rocketjump.saul.entities.model.physical.creature.body;

import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.junit.Before;
import org.junit.Test;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.TestModule;
import technology.rocketjump.saul.guice.SaulGuiceModule;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;

import java.util.Random;

public class BodyDamageTest {

	private BodyStructureDictionary bodyStructureDictionary;
	private BodyStructure humanoidBodyStructure;
	private I18nTranslator i18nTranslator;
	private Random random;

	@Before
	public void setUp() throws Exception {
		Injector injector = Guice.createInjector(new TestModule(), new SaulGuiceModule());
		bodyStructureDictionary = injector.getInstance(BodyStructureDictionary.class);
		i18nTranslator = injector.getInstance(I18nTranslator.class);
		humanoidBodyStructure = bodyStructureDictionary.getByName("Humanoid");
		random = new RandomXS128();
	}

	@Test
	public void testRandomLocation() {
		Body body = new Body(humanoidBodyStructure);
		for (int i = 0; i < 100; i++) {
			BodyPart bodyPart = body.randomlySelectPartBasedOnSize(random);
			Logger.info("Selected " + i18nTranslator.getDescription(bodyPart));
		}
	}
}