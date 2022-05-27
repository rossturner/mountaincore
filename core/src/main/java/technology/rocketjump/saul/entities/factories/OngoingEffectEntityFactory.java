package technology.rocketjump.saul.entities.factories;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.saul.entities.behaviour.effects.FireEffectBehaviour;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.LocationComponent;
import technology.rocketjump.saul.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.saul.entities.model.physical.effect.OngoingEffectAttributes;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;

import javax.inject.Inject;
import javax.inject.Singleton;

import static technology.rocketjump.saul.assets.entities.model.NullEntityAsset.NULL_ASSET;
import static technology.rocketjump.saul.entities.factories.PlantEntityFactory.createPhysicalComponent;

@Singleton
public class OngoingEffectEntityFactory {

	private final MessageDispatcher messageDispatcher;

	@Inject
	public OngoingEffectEntityFactory(MessageDispatcher messageDispatcher) {
		this.messageDispatcher = messageDispatcher;
	}

	public Entity create(OngoingEffectAttributes attributes, Vector2 worldPosition, GameContext gameContext) {
		PhysicalEntityComponent physicalComponent = createPhysicalComponent(attributes);
		physicalComponent.setBaseAsset(NULL_ASSET);
		FireEffectBehaviour behaviorComponent = new FireEffectBehaviour(); // TODO add this to type definition
		LocationComponent locationComponent = this.createLocationComponent(worldPosition, attributes);

		Entity entity = new Entity(EntityType.ONGOING_EFFECT, physicalComponent, behaviorComponent, locationComponent, messageDispatcher, gameContext);
		messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, entity); // to process tags
		messageDispatcher.dispatchMessage(MessageType.ENTITY_CREATED, entity);
		return entity;
	}

	private LocationComponent createLocationComponent(Vector2 worldPosition, OngoingEffectAttributes attributes) {
		LocationComponent locationComponent = new LocationComponent();
		if (worldPosition != null) {
			locationComponent.setWorldPosition(worldPosition, false);
		}
		locationComponent.setFacing(EntityAssetOrientation.DOWN.toVector2().cpy());
		locationComponent.setRadius(attributes.getEffectRadius());
		return locationComponent;
	}
}
