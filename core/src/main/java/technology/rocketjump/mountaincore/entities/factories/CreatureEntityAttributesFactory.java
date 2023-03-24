package technology.rocketjump.mountaincore.entities.factories;

import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.entities.factories.names.NameGenerator;
import technology.rocketjump.mountaincore.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Race;

import java.util.Random;

@Singleton
public class CreatureEntityAttributesFactory {

	private final Random random = new RandomXS128();
	private final NameGenerator nameGenerator;

	@Inject
	public CreatureEntityAttributesFactory(NameGenerator nameGenerator) {
		this.nameGenerator = nameGenerator;
	}

	public CreatureEntityAttributes create(Race race) {
		CreatureEntityAttributes attributes = new CreatureEntityAttributes(race, random.nextLong());

		if (race.getNameGeneration() != null) {
			attributes.setName(nameGenerator.create(race.getNameGeneration(), attributes.getSeed(), attributes.getGender()));
		}

		return attributes;
	}

}
