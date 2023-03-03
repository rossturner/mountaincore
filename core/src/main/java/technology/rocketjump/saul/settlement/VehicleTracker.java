package technology.rocketjump.saul.settlement;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.components.Faction;
import technology.rocketjump.saul.entities.components.FactionComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.vehicle.VehicleEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.vehicle.VehicleType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
public class VehicleTracker implements GameContextAware {

	private Map<VehicleType, List<Entity>> byVehicleType = new HashMap<>();

	@Inject
	public VehicleTracker() {

	}

	public List<Entity> byVehicleType(VehicleType vehicleType, Faction faction) {
		return byVehicleType.get(vehicleType).stream()
				.filter(e -> e.getOrCreateComponent(FactionComponent.class).getFaction().equals(faction))
				.toList();
	}

	public void vehicleAdded(Entity entity) {
		if (entity.getPhysicalEntityComponent().getAttributes() instanceof VehicleEntityAttributes attributes) {
			byVehicleType.computeIfAbsent(attributes.getVehicleType(), a -> new ArrayList<>()).add(entity);
		} else {
			Logger.error("Entity is not a vehicle");
		}
	}

	public void vehicleRemoved(Entity removedEntity) {
		if (removedEntity.getPhysicalEntityComponent().getAttributes() instanceof VehicleEntityAttributes attributes) {
			List<Entity> entities = byVehicleType.get(attributes.getVehicleType());
			if (entities != null) {
				entities.remove(removedEntity);
				if (entities.isEmpty()) {
					byVehicleType.remove(attributes.getVehicleType());
				}
			}
		} else {
			Logger.error("Entity is not a vehicle");
		}
	}

	@Override
	public void onContextChange(GameContext gameContext) {
	}

	@Override
	public void clearContextRelatedState() {
		byVehicleType.clear();
	}
}
