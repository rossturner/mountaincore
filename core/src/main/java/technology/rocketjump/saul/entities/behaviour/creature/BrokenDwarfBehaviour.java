package technology.rocketjump.saul.entities.behaviour.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.apache.commons.lang3.NotImplementedException;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.components.EntityComponent;
import technology.rocketjump.saul.entities.components.creature.SkillsComponent;
import technology.rocketjump.saul.entities.components.creature.SteeringComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.Sanity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.ParticleRequestMessage;
import technology.rocketjump.saul.particles.model.ParticleEffectInstance;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.Optional;

import static technology.rocketjump.saul.entities.ai.goap.SpecialGoal.IDLE;

public class BrokenDwarfBehaviour extends CreatureBehaviour implements ParticleRequestMessage.ParticleCreationCallback {

	private transient ParticleEffectInstance brokenDwarfEffect;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		super.init(parentEntity, messageDispatcher, gameContext);

		((CreatureEntityAttributes)parentEntity.getPhysicalEntityComponent().getAttributes()).setSanity(Sanity.BROKEN);
		parentEntity.removeComponent(SkillsComponent.class);
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		super.destroy(parentEntity, messageDispatcher, gameContext);

		if (brokenDwarfEffect != null) {
			brokenDwarfEffect.getWrappedInstance().allowCompletion();
		}
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		throw new NotImplementedException("Not yet implemented " + this.getClass().getSimpleName() + ".clone()");
	}

	@Override
	public void update(float deltaTime) {
		super.update(deltaTime);

		if (brokenDwarfEffect != null && !brokenDwarfEffect.isActive()) {
			brokenDwarfEffect = null;
		}

		if (brokenDwarfEffect == null) {
			messageDispatcher.dispatchMessage(MessageType.PARTICLE_REQUEST, new ParticleRequestMessage(
					"Broken dwarf cloud",
					Optional.of(parentEntity),
					Optional.empty(),
					this
			));
		}
	}

	@Override
	public void particleCreated(ParticleEffectInstance instance) {
		this.brokenDwarfEffect = instance;
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		super.infrequentUpdate(gameContext);
	}

	@Override
	public boolean isUpdateEveryFrame() {
		return true;
	}

	@Override
	public boolean isUpdateInfrequently() {
		return true;
	}

	@Override
	public boolean isJobAssignable() {
		return false;
	}

	@Override
	public SteeringComponent getSteeringComponent() {
		return steeringComponent;
	}

	@Override
	protected AssignedGoal pickNextGoalFromQueue() {
		return new AssignedGoal(IDLE.getInstance(), parentEntity, messageDispatcher);
	}

	@Override
	protected void addGoalsToQueue(GameContext gameContext) {
		// Do nothing
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);
	}
}
