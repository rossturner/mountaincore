package technology.rocketjump.mountaincore.entities.factories.names;

import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.model.physical.creature.Gender;
import technology.rocketjump.mountaincore.entities.model.physical.creature.HumanoidName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@Singleton
public class NameGenerator {

	private final NameGenerationDescriptorDictionary nameGenerationDescriptorDictionary;

	protected NameWordDictionary adjectiveDictionary;
	protected NameWordDictionary nounDictionary;

	protected final Map<String, GivenNameList> givenNamesMapping = new HashMap<>();

	@Inject
	public NameGenerator(NameGenerationDescriptorDictionary nameGenerationDescriptorDictionary) throws IOException {
		this.nameGenerationDescriptorDictionary = nameGenerationDescriptorDictionary;

		try (Stream<Path> fileList = Files.list(Path.of("assets/text/csv"))) {
			for (Path csvFile : fileList.toList()) {
				if (csvFile.toString().endsWith(".csv")) {
					if (csvFile.toString().endsWith("adjectives.csv")) {
						adjectiveDictionary = new NameWordDictionary(csvFile.toFile());
					} else if (csvFile.toString().endsWith("nouns.csv")) {
						nounDictionary = new NameWordDictionary(csvFile.toFile());
					} else if (csvFile.toString().endsWith("given_names.csv")) {
						GivenNameList givenNameList = new GivenNameList(csvFile.toFile());
						givenNamesMapping.put(FilenameUtils.removeExtension(csvFile.getFileName().toString()), givenNameList);
					} else {
						Logger.error(String.format("Unrecognised file of %s in %s", csvFile, csvFile.getParent().toAbsolutePath()));
					}
				}
			}
		}
	}

	public HumanoidName create(String descriptorName, long seed, Gender gender) {
		NameGenerationDescriptor descriptor = nameGenerationDescriptorDictionary.getByDescriptorName(descriptorName);
		if (descriptor == null) {
			Logger.error("Could not find name generation descriptor: " + descriptorName);
			return null;
		} else {
			HumanoidName name = new HumanoidName();
			name.setFirstName(generateUsingScheme(descriptor.getFirstName(), seed, gender, null, descriptor));
			name.setLastName(generateUsingScheme(descriptor.getFamilyName(), seed, gender, name.getFirstName().length() > 0 ? name.getFirstName().substring(0, 1) : null, descriptor));
			return name;
		}
	}

	public String generateUsingScheme(String scheme, long seed, Gender gender, String alliterationMatcher, NameGenerationDescriptor descriptor) {
		if (scheme.endsWith("given_names")) {
			return givenNamesMapping.get(scheme).createGivenName(seed, gender);
		} else if (scheme.equals("adjective_noun")) {
			return createAdjectiveNounName(seed, alliterationMatcher, descriptor);
		} else if (scheme.equals("none")) {
			return null;
		} else {
			Logger.error("Unrecognised name generation scheme: " + scheme);
			return "ERROR";
		}
	}

	protected String createAdjectiveNounName(long seed, String alliterationMatcher, NameGenerationDescriptor descriptor) {
		Random random = new RandomXS128(seed);

		String adjective = pickFrom(adjectiveDictionary, random, alliterationMatcher, descriptor.getGoodSpheres(), descriptor.getBadSpheres());
		String noun = pickFrom(nounDictionary, random, null, descriptor.getGoodSpheres(), descriptor.getBadSpheres());

		String lastOfAdjective = adjective.substring(adjective.length() - 1).toLowerCase(Locale.ROOT);
		String firstOfNoun = noun.substring(0, 1).toLowerCase(Locale.ROOT);
		if (lastOfAdjective.equals(firstOfNoun)) {
			return createAdjectiveNounName(seed + 1, alliterationMatcher, descriptor);
		}

		String combined = adjective + noun;
		return WordUtils.capitalize(combined.toLowerCase(Locale.ROOT));
	}

	protected String pickFrom(NameWordDictionary adjectiveDictionary, Random random, String alliterationMatcher, List<String> goodSpheres, List<String> badSpheres) {
		List<NameWord> toPickFrom = new ArrayList<>();

		for (String goodSphere : goodSpheres) {
			for (NameWord potentialWord : adjectiveDictionary.getBySphere(goodSphere)) {
				if (!Collections.disjoint(potentialWord.spheres, badSpheres)) {
					continue;
				}
				if (!toPickFrom.contains(potentialWord)) {
					toPickFrom.add(potentialWord);
				}
			}
		}

		if (toPickFrom.isEmpty()) {
			Logger.error("No valid names to pick from in " + this.getClass().getSimpleName());
			return "";
		} else {
			List<String> grabBag = new ArrayList<>();

			for (NameWord nameWord : toPickFrom) {
				int prevalence = nameWord.prevalence;
				if (alliterationMatcher != null && nameWord.word.startsWith(alliterationMatcher)) {
					prevalence *= 3; // Triple chance of word with alliteration (same starting letter)
				}
				for (int chances = 0; chances < prevalence; chances++) {
					grabBag.add(nameWord.word);
				}
			}

			return grabBag.get(random.nextInt(grabBag.size()));
		}
	}
}
