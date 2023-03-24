package technology.rocketjump.mountaincore.entities.factories;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.entities.model.physical.vehicle.VehicleEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.vehicle.VehicleType;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.GameContextAware;
import technology.rocketjump.mountaincore.materials.GameMaterialDictionary;

@Singleton
public class VehicleEntityAttributesFactory implements GameContextAware {

	private final GameMaterialDictionary gameMaterialDictionary;
	private GameContext gameContext;

	@Inject
	public VehicleEntityAttributesFactory(GameMaterialDictionary gameMaterialDictionary) {
		this.gameMaterialDictionary = gameMaterialDictionary;
	}

	public VehicleEntityAttributes create(VehicleType vehicleType) {
		VehicleEntityAttributes attributes = new VehicleEntityAttributes(gameContext.getRandom().nextLong());
		attributes.setVehicleType(vehicleType);

		// Material selection not important right now
		attributes.setMaterial(gameMaterialDictionary.getExampleMaterial(vehicleType.getMaterialType()));

		return attributes;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
