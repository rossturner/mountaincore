package technology.rocketjump.mountaincore.entities.factories.names;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Singleton
public class NameGenerationDescriptorDictionary {

	private final Map<String, NameGenerationDescriptor> byName = new HashMap<>();

	@Inject
	public NameGenerationDescriptorDictionary() throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		File jsonFile = new File("assets/text/name-descriptors.json");
		List<NameGenerationDescriptor> definitions = objectMapper.readValue(FileUtils.readFileToString(jsonFile, "UTF-8"),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, NameGenerationDescriptor.class));

		definitions.forEach(d -> byName.put(d.getDescriptorName(), d));
	}

	public NameGenerationDescriptor getByDescriptorName(String name) {
		return byName.get(name);
	}

	public Collection<NameGenerationDescriptor> getAll() {
		return byName.values();
	}

}
