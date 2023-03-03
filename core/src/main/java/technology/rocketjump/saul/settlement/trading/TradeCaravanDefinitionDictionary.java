package technology.rocketjump.saul.settlement.trading;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.io.FileUtils;
import technology.rocketjump.saul.entities.dictionaries.vehicle.VehicleTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.jobs.SkillDictionary;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.modding.exception.ModLoadingException;
import technology.rocketjump.saul.settlement.trading.model.TradeCaravanDefinition;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class TradeCaravanDefinitionDictionary {

	private final VehicleTypeDictionary vehicleTypeDictionary;
	private final RaceDictionary raceDictionary;
	private final SkillDictionary skillDictionary;
	private final ItemTypeDictionary itemTypeDictionary;
	private final GameMaterialDictionary gameMaterialDictionary;

	private List<TradeCaravanDefinition> allDefinitions = new ArrayList<>();

	@Inject
	public TradeCaravanDefinitionDictionary(VehicleTypeDictionary vehicleTypeDictionary, RaceDictionary raceDictionary,
											SkillDictionary skillDictionary, ItemTypeDictionary itemTypeDictionary,
											GameMaterialDictionary gameMaterialDictionary) throws IOException, ModLoadingException {
		this.vehicleTypeDictionary = vehicleTypeDictionary;
		this.raceDictionary = raceDictionary;
		this.skillDictionary = skillDictionary;
		this.itemTypeDictionary = itemTypeDictionary;
		this.gameMaterialDictionary = gameMaterialDictionary;

		ObjectMapper objectMapper = new ObjectMapper();
		File invasionDefinitionsFile = new File("assets/definitions/tradeCaravans.json");
		List<TradeCaravanDefinition> definitions = objectMapper.readValue(FileUtils.readFileToString(invasionDefinitionsFile, "UTF-8"),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, TradeCaravanDefinition.class));

		for (TradeCaravanDefinition definition : definitions) {
			init(definition);
			allDefinitions.add(definition);
		}

		if (allDefinitions.size() != 1) {
			// remove this when supporting a different number of definitions
			throw new ModLoadingException("Expected exactly one TradeCaravanDefinition, found " + allDefinitions.size());
		}
	}

	public TradeCaravanDefinition get() {
		return allDefinitions.get(0);
	}

	private void init(TradeCaravanDefinition definition) throws ModLoadingException {
		init(definition.getVehicles());
		init(definition.getTraders());
		init(definition.getGuards());
	}

	private void init(TradeCaravanDefinition.TradeCaravanVehiclesDescriptor vehicles) throws ModLoadingException {
		vehicles.setVehicleType(vehicleTypeDictionary.getByName(vehicles.getVehicleTypeName()));
		if (vehicles.getVehicleType() == null) {
			throw new ModLoadingException("Could not find vehicle type with name " + vehicles.getVehicleTypeName());
		}
		vehicles.setDraughtAnimalRace(raceDictionary.getByName(vehicles.getDraughtAnimal()));
		if (vehicles.getDraughtAnimalRace() == null) {
			throw new ModLoadingException("Could not find animal race with name " + vehicles.getDraughtAnimal());
		}
	}

	private void init(TradeCaravanDefinition.TradeCaravanCreatureDescriptor creatures) throws ModLoadingException {
		creatures.setRace(raceDictionary.getByName(creatures.getRaceName()));
		if (creatures.getRace() == null) {
			throw new ModLoadingException("Could not find race with name " + creatures.getRaceName());
		}
		if (creatures.getProfessionName() != null) {
			creatures.setProfession(skillDictionary.getByName(creatures.getProfessionName()));
			if (creatures.getProfession() == null) {
				throw new ModLoadingException("Could not find profession with name " + creatures.getProfessionName());
			}
		}

		creatures.getInventoryItems().forEach(i -> i.initialise(itemTypeDictionary, gameMaterialDictionary));

		validateItemTypes(creatures.getWeaponItemTypes());
		validateItemTypes(creatures.getShieldItemTypes());
		validateItemTypes(creatures.getArmorItemTypes());
	}

	private void validateItemTypes(List<String> itemTypeNames) throws ModLoadingException {
		for (String itemTypeName : itemTypeNames) {
			if (itemTypeName != null) {
				ItemType itemType = itemTypeDictionary.getByName(itemTypeName);
				if (itemType == null) {
					throw new ModLoadingException("Could not find item type with name " + itemTypeName);
				}
			}
		}
	}

}
