package technology.rocketjump.mountaincore.entities.factories;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.mountaincore.entities.EntityAssetUpdater;
import technology.rocketjump.mountaincore.entities.components.BehaviourComponent;
import technology.rocketjump.mountaincore.entities.components.OxidisationComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.EntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.LocationComponent;
import technology.rocketjump.mountaincore.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

import java.util.Random;

@Singleton
public class FurnitureEntityFactory {

	private static final float ITEM_RADIUS = 0.4f;
	private static final float OFFSET_POSITION_EPSILON = 0.01f;
	private final MessageDispatcher messageDispatcher;
	private final EntityAssetUpdater entityAssetUpdater;
	private final Random random = new RandomXS128();

	@Inject
	public FurnitureEntityFactory(MessageDispatcher messageDispatcher, EntityAssetUpdater entityAssetUpdater) {
		this.messageDispatcher = messageDispatcher;
		this.entityAssetUpdater = entityAssetUpdater;
	}

	public Entity create(FurnitureEntityAttributes attributes, GridPoint2 tilePosition, BehaviourComponent behaviour, GameContext gameContext) {
		PhysicalEntityComponent physicalComponent = createPhysicalComponent(attributes);
		LocationComponent locationComponent = createLocationComponent(tilePosition);

		Entity entity = new Entity(EntityType.FURNITURE, physicalComponent, behaviour, locationComponent, messageDispatcher, gameContext);

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
		Vector2 worldPosition = new Vector2(
				tilePosition.x + 0.5f + (-OFFSET_POSITION_EPSILON + (2 * OFFSET_POSITION_EPSILON * random.nextFloat())),
				tilePosition.y + 0.5f + (-OFFSET_POSITION_EPSILON + (2 * OFFSET_POSITION_EPSILON * random.nextFloat())));

		locationComponent.setWorldPosition(worldPosition, false);
		locationComponent.setFacing(EntityAssetOrientation.DOWN.toVector2().cpy());
		locationComponent.setRadius(ITEM_RADIUS);
		return locationComponent;
	}

	private PhysicalEntityComponent createPhysicalComponent(EntityAttributes attributes) {
		PhysicalEntityComponent physicalComponent = new PhysicalEntityComponent();
		physicalComponent.setAttributes(attributes);
		return physicalComponent;
	}

}
