package technology.rocketjump.mountaincore.entities.factories;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.mountaincore.assets.entities.model.NullEntityAsset;
import technology.rocketjump.mountaincore.entities.behaviour.effects.FireEffectBehaviour;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.LocationComponent;
import technology.rocketjump.mountaincore.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.mountaincore.entities.model.physical.effect.OngoingEffectAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.messaging.MessageType;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class OngoingEffectEntityFactory {

	private final MessageDispatcher messageDispatcher;

	@Inject
	public OngoingEffectEntityFactory(MessageDispatcher messageDispatcher) {
		this.messageDispatcher = messageDispatcher;
	}

	public Entity create(OngoingEffectAttributes attributes, Vector2 worldPosition, GameContext gameContext) {
		PhysicalEntityComponent physicalComponent = PlantEntityFactory.createPhysicalComponent(attributes);
		physicalComponent.setBaseAsset(NullEntityAsset.NULL_ASSET);
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
