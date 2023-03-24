package technology.rocketjump.mountaincore.entities.factories.names;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SettlementNameGenerator {

	private final NameGenerationDescriptorDictionary nameGenerationDescriptorDictionary;
	private final NameGenerator nameGenerator;

	@Inject
	public SettlementNameGenerator(NameGenerationDescriptorDictionary nameGenerationDescriptorDictionary, NameGenerator nameGenerator) {
		this.nameGenerationDescriptorDictionary = nameGenerationDescriptorDictionary;
		this.nameGenerator = nameGenerator;
	}

	public String create(long seed) {
		return nameGenerator.createAdjectiveNounName(seed, null, nameGenerationDescriptorDictionary.getByDescriptorName("Settlement"));
	}


}
