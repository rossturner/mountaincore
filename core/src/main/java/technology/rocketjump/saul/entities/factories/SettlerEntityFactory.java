package technology.rocketjump.saul.entities.factories;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.entities.EntityAssetUpdater;
import technology.rocketjump.saul.entities.ai.goap.GoalDictionary;
import technology.rocketjump.saul.entities.behaviour.creature.SettlerBehaviour;
import technology.rocketjump.saul.entities.components.humanoid.MemoryComponent;
import technology.rocketjump.saul.entities.components.humanoid.NeedsComponent;
import technology.rocketjump.saul.entities.components.humanoid.ProfessionsComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.LocationComponent;
import technology.rocketjump.saul.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.HaulingComponent;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.ProfessionDictionary;
import technology.rocketjump.saul.jobs.model.Profession;
import technology.rocketjump.saul.rooms.RoomStore;

import static technology.rocketjump.saul.jobs.ProfessionDictionary.NULL_PROFESSION;

@Singleton
public class SettlerEntityFactory {

	private final MessageDispatcher messageDispatcher;
	private final EntityAssetUpdater entityAssetUpdater;
	private final ProfessionDictionary professionDictionary;
	private final GoalDictionary goalDictionary;
	private final RoomStore roomStore;

	@Inject
	public SettlerEntityFactory(MessageDispatcher messageDispatcher, ProfessionDictionary professionDictionary,
								EntityAssetUpdater entityAssetUpdater, GoalDictionary goalDictionary, RoomStore roomStore) {
		this.messageDispatcher = messageDispatcher;
		this.professionDictionary = professionDictionary;
		this.entityAssetUpdater = entityAssetUpdater;
		this.goalDictionary = goalDictionary;
		this.roomStore = roomStore;
	}

	public Entity create(CreatureEntityAttributes attributes, Vector2 worldPosition, Vector2 facing, Profession primaryProfession,
						 Profession secondaryProfession, GameContext gameContext) {
		PhysicalEntityComponent physicalComponent = new PhysicalEntityComponent();
		physicalComponent.setAttributes(attributes);

		SettlerBehaviour behaviourComponent = new SettlerBehaviour();
		behaviourComponent.constructWith(goalDictionary, roomStore);

		LocationComponent locationComponent = new LocationComponent();
		locationComponent.setWorldPosition(worldPosition, true);

		Entity entity = new Entity(EntityType.CREATURE, physicalComponent, behaviourComponent, locationComponent,
				messageDispatcher, gameContext);
		entity.addComponent(new HaulingComponent());
		locationComponent.setFacing(facing);

		ProfessionsComponent professionsComponent = new ProfessionsComponent();
		if (primaryProfession == null) {
			primaryProfession = NULL_PROFESSION;
		}
		professionsComponent.setSkillLevel(primaryProfession, 50);
		if (secondaryProfession != null && !secondaryProfession.equals(primaryProfession)) {
			professionsComponent.setSkillLevel(secondaryProfession, 30);
		}
		entity.addComponent(professionsComponent);

		NeedsComponent needsComponent = new NeedsComponent(attributes.getRace().getBehaviour().getNeeds(), gameContext.getRandom());
		entity.addComponent(needsComponent);
		entity.addComponent(new MemoryComponent());

		entityAssetUpdater.updateEntityAssets(entity);

		return entity;
	}

}
