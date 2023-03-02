package technology.rocketjump.saul.invasions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.invasions.model.InvasionDefinition;
import technology.rocketjump.saul.invasions.model.InvasionEquipmentDescriptor;
import technology.rocketjump.saul.invasions.model.InvasionParticipant;
import technology.rocketjump.saul.materials.GameMaterialDictionary;

import java.io.File;
import java.io.IOException;
import java.util.*;

@Singleton
public class InvasionDefinitionDictionary {

	private final RaceDictionary raceDictionary;
	private final ItemTypeDictionary itemTypeDictionary;
	private final GameMaterialDictionary materialDictionary;

	private Map<String, InvasionDefinition> byName = new HashMap<>();

	@Inject
	public InvasionDefinitionDictionary(RaceDictionary raceDictionary, ItemTypeDictionary itemTypeDictionary,
										GameMaterialDictionary materialDictionary) throws IOException {
		this.raceDictionary = raceDictionary;
		this.itemTypeDictionary = itemTypeDictionary;
		this.materialDictionary = materialDictionary;

		ObjectMapper objectMapper = new ObjectMapper();
		File invasionDefinitionsFile = new File("assets/definitions/invasions.json");
		List<InvasionDefinition> definitions = objectMapper.readValue(FileUtils.readFileToString(invasionDefinitionsFile, "UTF-8"),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, InvasionDefinition.class));

		for (InvasionDefinition definition : definitions) {
			init(definition);
			byName.put(definition.getName(), definition);
		}
	}

	public Collection<InvasionDefinition> getAll() {
		return byName.values();
	}

	public InvasionDefinition getByName(String name) {
		return byName.get(name);
	}

	private void init(InvasionDefinition definition) {
		for (InvasionParticipant participant : definition.getParticipants()) {
			participant.setRace(raceDictionary.getByName(participant.getRaceName()));

			if (participant.getRace() == null) {
				Logger.error(String.format("Could not find race with name %s for participant in invasion %s", participant.getRaceName(), definition.getName()));
			}

			participant.getFixedInventory().forEach(item -> item.initialise(itemTypeDictionary, materialDictionary));

			for (List<InvasionEquipmentDescriptor> equipmentOption : List.of(
					participant.getEquipmentOptions().getWeapons(),
					participant.getEquipmentOptions().getShield(),
					participant.getEquipmentOptions().getArmor())
			) {
				for (InvasionEquipmentDescriptor equipmentDescriptor : equipmentOption) {
					if (equipmentDescriptor.getItemTypeName() != null) {
						equipmentDescriptor.setItemType(itemTypeDictionary.getByName(equipmentDescriptor.getItemTypeName()));
						if (equipmentDescriptor.getItemType() == null) {
							Logger.error(String.format("Could not find item type with name %s for participant %s of invasion %s", equipmentDescriptor.getItemTypeName(), participant.getRaceName(), definition.getName()));
						}
					} else if (!equipmentDescriptor.isNone()) {
						Logger.error("Equipment descriptor on " + definition.getName() + " must have an itemTypeName or have \"none\": true");
					}
				}
			}
		}
	}

}
