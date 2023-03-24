package technology.rocketjump.mountaincore.entities.factories;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.mountaincore.entities.EntityAssetUpdater;
import technology.rocketjump.mountaincore.entities.behaviour.vehicle.VehicleBehaviour;
import technology.rocketjump.mountaincore.entities.components.Faction;
import technology.rocketjump.mountaincore.entities.components.FactionComponent;
import technology.rocketjump.mountaincore.entities.components.OxidisationComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.EntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.LocationComponent;
import technology.rocketjump.mountaincore.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.mountaincore.entities.model.physical.vehicle.VehicleEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.messaging.MessageType;

@Singleton
public class VehicleEntityFactory {

	private static final float VEHICLE_RADIUS = 1f;
	private final MessageDispatcher messageDispatcher;
	private final EntityAssetUpdater entityAssetUpdater;

	@Inject
	public VehicleEntityFactory(MessageDispatcher messageDispatcher, EntityAssetUpdater entityAssetUpdater) {
		this.messageDispatcher = messageDispatcher;
		this.entityAssetUpdater = entityAssetUpdater;
	}

	public Entity create(VehicleEntityAttributes attributes, GridPoint2 tilePosition, GameContext gameContext, Faction faction) {
		PhysicalEntityComponent physicalComponent = createPhysicalComponent(attributes);
		LocationComponent locationComponent = createLocationComponent(tilePosition);
		VehicleBehaviour vehicleBehaviour = new VehicleBehaviour();

		Entity entity = new Entity(EntityType.VEHICLE, physicalComponent, vehicleBehaviour, locationComponent, messageDispatcher, gameContext);

		attributes.getMaterials().values().stream().filter(m -> m.getOxidisation() != null)
				.findAny()
				.ifPresent((a) -> {
					OxidisationComponent oxidisationComponent = new OxidisationComponent();
					oxidisationComponent.init(entity, messageDispatcher, gameContext);
					entity.addComponent(oxidisationComponent);
				});

		FactionComponent factionComponent = new FactionComponent();
		factionComponent.init(entity, messageDispatcher, gameContext);
		entity.addComponent(factionComponent);
		factionComponent.setFaction(faction);

		entityAssetUpdater.updateEntityAssets(entity);
		messageDispatcher.dispatchMessage(MessageType.ENTITY_CREATED, entity);
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
