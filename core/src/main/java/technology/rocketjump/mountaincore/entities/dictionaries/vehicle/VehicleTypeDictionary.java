package technology.rocketjump.mountaincore.entities.dictionaries.vehicle;

import com.badlogic.gdx.files.FileHandle;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.entities.model.physical.vehicle.VehicleType;

import java.io.IOException;
import java.util.*;

@Singleton
public class VehicleTypeDictionary {

	private final Map<String, VehicleType> byName = new HashMap<>();

	public static VehicleType NULL_TYPE = new VehicleType();
	static {
		NULL_TYPE.setName("Null vehicle type");
	}

	@Inject
	public VehicleTypeDictionary() throws IOException {
		this(new FileHandle("assets/definitions/types/vehicleTypes.json"));
	}

	public VehicleTypeDictionary(FileHandle jsonFile) throws IOException {
		ObjectMapper objectMapper = new ObjectMapper();
		List<VehicleType> vehicleTypes = objectMapper.readValue(jsonFile.readString(),
				objectMapper.getTypeFactory().constructParametrizedType(ArrayList.class, List.class, VehicleType.class));

		for (VehicleType vehicleType : vehicleTypes) {
			add(vehicleType);
		}
		byName.put(NULL_TYPE.getName(), NULL_TYPE);
	}

	public void add(VehicleType vehicleType) {
		initialiseVehicleType(vehicleType);

		byName.put(vehicleType.getName(), vehicleType);
	}

	private void initialiseVehicleType(VehicleType vehicleType) {

	}

	public VehicleType getByName(String name) {
		return byName.get(name);
	}

	public Collection<VehicleType> getAll() {
		return byName.values();
	}

}
