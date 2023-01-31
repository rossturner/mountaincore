package technology.rocketjump.saul.entities.factories;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.entities.model.physical.vehicle.VehicleEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.vehicle.VehicleType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;

@Singleton
public class VehicleEntityAttributesFactory implements GameContextAware {

	private GameContext gameContext;

	@Inject
	public VehicleEntityAttributesFactory() {
	}

	public VehicleEntityAttributes create(VehicleType vehicleType) {
		VehicleEntityAttributes attributes = new VehicleEntityAttributes(gameContext.getRandom().nextLong());
		attributes.setVehicleType(vehicleType);
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
