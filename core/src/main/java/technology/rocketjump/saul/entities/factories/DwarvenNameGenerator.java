package technology.rocketjump.saul.entities.factories;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.entities.factories.names.NameGenerator;
import technology.rocketjump.saul.entities.factories.names.NorseNameGenerator;

import java.io.File;
import java.io.IOException;

@Singleton
public class DwarvenNameGenerator extends NameGenerator {

	@Inject
	public DwarvenNameGenerator(NorseNameGenerator norseNameGenerator) throws IOException {
		super(new File("assets/text/dwarven/descriptor.json"), norseNameGenerator);
	}

}
