package technology.rocketjump.saul.entities.factories;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.entities.EntityAssetUpdater;
import technology.rocketjump.saul.entities.ai.goap.GoalDictionary;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.saul.entities.components.creature.CombatStateComponent;
import technology.rocketjump.saul.entities.components.creature.MemoryComponent;
import technology.rocketjump.saul.entities.components.creature.NeedsComponent;
import technology.rocketjump.saul.entities.components.creature.SkillsComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.LocationComponent;
import technology.rocketjump.saul.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.HaulingComponent;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.SkillDictionary;
import technology.rocketjump.saul.jobs.model.Skill;
import technology.rocketjump.saul.rooms.RoomStore;

import static technology.rocketjump.saul.jobs.SkillDictionary.NULL_PROFESSION;

@Singleton
// TODO Combine this with SettlerFactory
public class SettlerEntityFactory {

	private final MessageDispatcher messageDispatcher;
	private final EntityAssetUpdater entityAssetUpdater;
	private final SkillDictionary skillDictionary;
	private final GoalDictionary goalDictionary;
	private final RoomStore roomStore;

	@Inject
	public SettlerEntityFactory(MessageDispatcher messageDispatcher, SkillDictionary skillDictionary,
								EntityAssetUpdater entityAssetUpdater, GoalDictionary goalDictionary, RoomStore roomStore) {
		this.messageDispatcher = messageDispatcher;
		this.skillDictionary = skillDictionary;
		this.entityAssetUpdater = entityAssetUpdater;
		this.goalDictionary = goalDictionary;
		this.roomStore = roomStore;
	}

	public Entity create(CreatureEntityAttributes attributes, Vector2 worldPosition, Vector2 facing, Skill primaryProfession,
						 Skill secondaryProfession, GameContext gameContext) {
		PhysicalEntityComponent physicalComponent = new PhysicalEntityComponent();
		physicalComponent.setAttributes(attributes);

		CreatureBehaviour behaviourComponent = new CreatureBehaviour();
		behaviourComponent.constructWith(goalDictionary, roomStore);

		LocationComponent locationComponent = new LocationComponent();
		locationComponent.setWorldPosition(worldPosition, true);

		Entity entity = new Entity(EntityType.CREATURE, physicalComponent, behaviourComponent, locationComponent,
				messageDispatcher, gameContext);
		entity.addComponent(new HaulingComponent());
		locationComponent.setFacing(facing);

		SkillsComponent skillsComponent = new SkillsComponent();
		if (primaryProfession == null) {
			primaryProfession = NULL_PROFESSION;
		}
		skillsComponent.setSkillLevel(primaryProfession, 50);
		if (secondaryProfession != null && !secondaryProfession.equals(primaryProfession)) {
			skillsComponent.setSkillLevel(secondaryProfession, 30);
		}
		entity.addComponent(skillsComponent);

		NeedsComponent needsComponent = new NeedsComponent(attributes.getRace().getBehaviour().getNeeds(), gameContext.getRandom());
		entity.addComponent(needsComponent);
		entity.addComponent(new MemoryComponent());
		entity.getOrCreateComponent(CombatStateComponent.class).init(entity, messageDispatcher, gameContext);

		entityAssetUpdater.updateEntityAssets(entity);

		return entity;
	}

}
