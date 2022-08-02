package technology.rocketjump.saul.entities.model.physical.creature.body;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.model.physical.creature.body.organs.OrganDefinitionDictionary;
import technology.rocketjump.saul.materials.GameMaterialDictionary;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Singleton
public class BodyStructureDictionary {

	private final OrganDefinitionDictionary organDefinitionDictionary;
	private final GameMaterialDictionary gameMaterialDictionary;
	private final Map<String, BodyStructure> byName = new HashMap<>();

	@Inject
	public BodyStructureDictionary(OrganDefinitionDictionary organDefinitionDictionary, GameMaterialDictionary gameMaterialDictionary) throws IOException {
		this.organDefinitionDictionary = organDefinitionDictionary;
		this.gameMaterialDictionary = gameMaterialDictionary;
		File jsonFile = new File("assets/definitions/bodyStructures.json");
		ObjectMapper objectMapper = new ObjectMapper();
		List<BodyStructure> bodyStructures = objectMapper.readValue(FileUtils.readFileToString(jsonFile),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, BodyStructure.class));

		for (BodyStructure bodyStructure : bodyStructures) {
			initialise(bodyStructure);
			byName.put(bodyStructure.getName(), bodyStructure);
		}
	}

	private void initialise(BodyStructure bodyStructure) {
		bodyStructure.getPartDefinitionByName(bodyStructure.getRootPartName()).ifPresent(bodyStructure::setRootPart);
		if (bodyStructure.getRootPart() == null) {
			throw new RuntimeException("Could not find root part with name " + bodyStructure.getRootPartName() + " for " + bodyStructure.getName());
		}

		for (BodyPartDefinition partDefinition : bodyStructure.getPartDefinitions()) {
			for (BodyPartOrgan organ : partDefinition.getOrgans()) {
				organ.setOrganDefinition(organDefinitionDictionary.getByName(organ.getType()));
				if (organ.getOrganDefinition() == null) {
					Logger.error("Can not find organ definition with name " + organ.getType() + " for " + bodyStructure.getName());
				}
			}
		}

	}

	public BodyStructure getByName(String organDefinitionName) {
		return byName.get(organDefinitionName);
	}

	public Collection<BodyStructure> getAll() {
		return byName.values();
	}
}
