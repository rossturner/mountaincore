package technology.rocketjump.saul.entities.factories;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.saul.entities.EntityAssetUpdater;
import technology.rocketjump.saul.entities.behaviour.vehicle.VehicleBehaviour;
import technology.rocketjump.saul.entities.components.OxidisationComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.EntityAttributes;
import technology.rocketjump.saul.entities.model.physical.LocationComponent;
import technology.rocketjump.saul.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.saul.entities.model.physical.vehicle.VehicleEntityAttributes;
import technology.rocketjump.saul.gamecontext.GameContext;

import java.util.Random;

@Singleton
public class VehicleEntityFactory {

	private static final float VEHICLE_RADIUS = 1f;
	private final MessageDispatcher messageDispatcher;
	private final EntityAssetUpdater entityAssetUpdater;
	private final Random random = new RandomXS128();

	@Inject
	public VehicleEntityFactory(MessageDispatcher messageDispatcher, EntityAssetUpdater entityAssetUpdater) {
		this.messageDispatcher = messageDispatcher;
		this.entityAssetUpdater = entityAssetUpdater;
	}

	public Entity create(VehicleEntityAttributes attributes, GridPoint2 tilePosition, GameContext gameContext) {
		PhysicalEntityComponent physicalComponent = createPhysicalComponent(attributes);
		LocationComponent locationComponent = createLocationComponent(tilePosition);

		Entity entity = new Entity(EntityType.VEHICLE, physicalComponent, new VehicleBehaviour(), locationComponent, messageDispatcher, gameContext);

		attributes.getMaterials().values().stream().filter(m -> m.getOxidisation() != null)
				.findAny()
				.ifPresent((a) -> {
					OxidisationComponent oxidisationComponent = new OxidisationComponent();
					oxidisationComponent.init(entity, messageDispatcher, gameContext);
					entity.addComponent(oxidisationComponent);
				});


		entityAssetUpdater.updateEntityAssets(entity);
		return entity;
	}

	private LocationComponent createLocationComponent(GridPoint2 tilePosition) {
		LocationComponent locationComponent = new LocationComponent();
		Vector2 worldPosition = new Vector2(tilePosition.x + 0.5f, tilePosition.y + 0.5f);

		locationComponent.setWorldPosition(worldPosition, false);
		locationComponent.setFacing(EntityAssetOrientation.DOWN.toVector2().cpy());
		locationComponent.setRadius(VEHICLE_RADIUS);
		return locationComponent;
	}

	private PhysicalEntityComponent createPhysicalComponent(EntityAttributes attributes) {
		PhysicalEntityComponent physicalComponent = new PhysicalEntityComponent();
		physicalComponent.setAttributes(attributes);
		return physicalComponent;
	}

}
