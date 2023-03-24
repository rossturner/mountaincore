package technology.rocketjump.mountaincore.entities.factories;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.mountaincore.entities.EntityAssetUpdater;
import technology.rocketjump.mountaincore.entities.components.BehaviourComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.EntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.LocationComponent;
import technology.rocketjump.mountaincore.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.mountaincore.entities.model.physical.mechanism.MechanismEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

@Singleton
public class MechanismEntityFactory {

	private static final float MECHANISM_RADIUS = 0.5f;
	private final MessageDispatcher messageDispatcher;
	private final EntityAssetUpdater entityAssetUpdater;

	@Inject
	public MechanismEntityFactory(MessageDispatcher messageDispatcher, EntityAssetUpdater entityAssetUpdater) {
		this.messageDispatcher = messageDispatcher;
		this.entityAssetUpdater = entityAssetUpdater;
	}

	public Entity create(MechanismEntityAttributes attributes, GridPoint2 tilePosition, BehaviourComponent behaviour, GameContext gameContext) {
		PhysicalEntityComponent physicalComponent = createPhysicalComponent(attributes);
		LocationComponent locationComponent = createLocationComponent(tilePosition);

		Entity entity = new Entity(EntityType.MECHANISM, physicalComponent, behaviour, locationComponent, messageDispatcher, gameContext);

//		attributes.getMaterials().values().stream().filter(m -> m.getOxidisation() != null)
//				.findAny()
//				.ifPresent((a) -> {
//					OxidisationComponent oxidisationComponent = new OxidisationComponent();
//					oxidisationComponent.init(entity, messageDispatcher, gameContext);
//					entity.addComponent(oxidisationComponent);
//				});

		entityAssetUpdater.updateEntityAssets(entity);
		return entity;
	}

	private LocationComponent createLocationComponent(GridPoint2 tilePosition) {
		LocationComponent locationComponent = new LocationComponent();
		Vector2 worldPosition = new Vector2(tilePosition.x + 0.5f, tilePosition.y + 0.5f);

		locationComponent.setWorldPosition(worldPosition, false);
		locationComponent.setFacing(EntityAssetOrientation.DOWN.toVector2().cpy());
		locationComponent.setRadius(MECHANISM_RADIUS);
		return locationComponent;
	}

	private PhysicalEntityComponent createPhysicalComponent(EntityAttributes attributes) {
		PhysicalEntityComponent physicalComponent = new PhysicalEntityComponent();
		physicalComponent.setAttributes(attributes);
		return physicalComponent;
	}

}
