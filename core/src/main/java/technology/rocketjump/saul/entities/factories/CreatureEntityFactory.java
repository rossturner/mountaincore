package technology.rocketjump.saul.entities.factories;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.EntityAssetUpdater;
import technology.rocketjump.saul.entities.ai.goap.GoalDictionary;
import technology.rocketjump.saul.entities.behaviour.DoNothingBehaviour;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.saul.entities.behaviour.creature.SettlerBehaviour;
import technology.rocketjump.saul.entities.components.BehaviourComponent;
import technology.rocketjump.saul.entities.components.humanoid.MemoryComponent;
import technology.rocketjump.saul.entities.components.humanoid.NeedsComponent;
import technology.rocketjump.saul.entities.components.humanoid.StatusComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.LocationComponent;
import technology.rocketjump.saul.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rooms.RoomStore;

@Singleton
public class CreatureEntityFactory  {

	private final MessageDispatcher messageDispatcher;
	private final EntityAssetUpdater entityAssetUpdater;
	private final GoalDictionary goalDictionary;
	private final RoomStore roomStore;

	@Inject
	public CreatureEntityFactory(MessageDispatcher messageDispatcher, EntityAssetUpdater entityAssetUpdater, GoalDictionary goalDictionary,
								 RoomStore roomStore) {
		this.messageDispatcher = messageDispatcher;
		this.entityAssetUpdater = entityAssetUpdater;
		this.goalDictionary = goalDictionary;
		this.roomStore = roomStore;
	}

	public Entity create(CreatureEntityAttributes attributes, Vector2 worldPosition, Vector2 facing, GameContext gameContext) {
		PhysicalEntityComponent physicalComponent = new PhysicalEntityComponent();
		physicalComponent.setAttributes(attributes);

		BehaviourComponent behaviourComponent = new DoNothingBehaviour();
		if (attributes.getRace().getBehaviour().getBehaviourClass() != null) {
			try {
				behaviourComponent = attributes.getRace().getBehaviour().getBehaviourClass().getConstructor().newInstance();

				if (behaviourComponent instanceof SettlerBehaviour) {
					SettlerBehaviour settlerBehaviour = (SettlerBehaviour) behaviourComponent;
					settlerBehaviour.constructWith(goalDictionary, roomStore);
				} else if (behaviourComponent instanceof CreatureBehaviour) {
					((CreatureBehaviour) behaviourComponent).constructWith(goalDictionary);
				}

			} catch (ReflectiveOperationException e) {
				Logger.error("Could not initialise behaviour class " + attributes.getRace().getBehaviour().getBehaviourClass().getSimpleName());
			}
		}

		LocationComponent locationComponent = new LocationComponent();
		locationComponent.setWorldPosition(worldPosition, true);
		locationComponent.setFacing(facing);

		Entity entity = new Entity(EntityType.CREATURE, physicalComponent, behaviourComponent, locationComponent,
				messageDispatcher, gameContext);

		entity.addComponent(new NeedsComponent(attributes.getRace().getBehaviour().getNeeds(), gameContext.getRandom()));
		entity.addComponent(new MemoryComponent());
		entity.getOrCreateComponent(StatusComponent.class).init(entity, messageDispatcher, gameContext);

		entityAssetUpdater.updateEntityAssets(entity);
		messageDispatcher.dispatchMessage(MessageType.ENTITY_CREATED, entity);
		return entity;
	}

}
