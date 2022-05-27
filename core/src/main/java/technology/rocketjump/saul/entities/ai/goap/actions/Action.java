package technology.rocketjump.saul.entities.ai.goap.actions;

import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.particles.model.ParticleEffectInstance;
import technology.rocketjump.saul.persistence.model.ChildPersistable;

import java.util.ArrayList;
import java.util.List;

public abstract class Action implements ChildPersistable {

	protected final AssignedGoal parent;
	protected CompletionType completionType;
	protected List<ParticleEffectInstance> spawnedParticles = new ArrayList<>();

	public Action(AssignedGoal parent) {
		this.parent = parent;
	}

	/**
	 * Used to determine if this action should apply to the current parent
	 * @param gameContext
	 */
	public boolean isApplicable(GameContext gameContext) {
		return true;
	}

	public abstract void update(float deltaTime, GameContext gameContext) throws SwitchGoalException;

	public boolean isInterruptible() {
		return true;
	}

	public void actionInterrupted(GameContext gameContext) {
		completionType = CompletionType.FAILURE;
	}

	public String getDescriptionOverrideI18nKey() {
		return null;
	}

	public static Action newInstance(Class<? extends Action> classType, AssignedGoal assignedGoal) {
		try {
			return classType.getConstructor(assignedGoal.getClass()).newInstance(assignedGoal);
		} catch (ReflectiveOperationException e) {
			Logger.error("Could not find constructor for class " + classType.getSimpleName() + " with a parameter of " + assignedGoal.getClass().getSimpleName());
			throw new RuntimeException(e);
		}
	}

	public CompletionType isCompleted(GameContext gameContext) throws SwitchGoalException {
		if (completionType != null) {
			// This action is completed in some way so perform cleanup
			if (spawnedParticles != null) {
				for (ParticleEffectInstance spawnedParticle : spawnedParticles) {
					parent.messageDispatcher.dispatchMessage(MessageType.PARTICLE_RELEASE, spawnedParticle);
				}
				this.spawnedParticles = null;
			}
		}
		return completionType;
	}

	public String getSimpleName() {
		return getClass().getSimpleName().replace("Action", "");
	}

	public enum CompletionType {

		SUCCESS, FAILURE

	}

}
