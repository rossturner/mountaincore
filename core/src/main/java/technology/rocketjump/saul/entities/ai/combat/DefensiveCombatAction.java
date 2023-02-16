package technology.rocketjump.saul.entities.ai.combat;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.saul.entities.components.creature.CombatStateComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;

public class DefensiveCombatAction extends CombatAction {

	public DefensiveCombatAction(Entity parentEntity) {
		super(parentEntity);
		this.completed = true;
	}

	@Override
	public void update(float deltaTime, GameContext gameContext, MessageDispatcher messageDispatcher) {
		CombatStateComponent combatStateComponent = parentEntity.getComponent(CombatStateComponent.class);
		if (combatStateComponent.getTargetedOpponentId() != null) {
			Entity opponentEntity = gameContext.getEntities().get(combatStateComponent.getTargetedOpponentId());
			if (opponentEntity != null) {
				// Face towards opponent
				Vector2 parentPosition = parentEntity.getLocationComponent().getWorldOrParentPosition();
				Vector2 opponentPosition = opponentEntity.getLocationComponent().getWorldOrParentPosition();
				parentEntity.getLocationComponent().setFacing(opponentPosition.cpy().sub(parentPosition));
			}
		}
	}

	@Override
	public void interrupted(MessageDispatcher messageDispatcher) {

	}

	@Override
	public boolean completesInOneRound() {
		return true;
	}

	@Override
	public void onRoundCompletion() {
		super.onRoundCompletion();
		CreatureCombat combatStats = new CreatureCombat(parentEntity);
		CombatStateComponent combatStateComponent = parentEntity.getComponent(CombatStateComponent.class);

		if (!combatStateComponent.isHasInitiative()) {
			int defensePool = combatStateComponent.getDefensePool();
			defensePool += combatStats.defensePoolRegainedPerDefensiveRound();
			defensePool = Math.min(defensePool, combatStats.maxDefensePool());
			combatStateComponent.setDefensePool(defensePool);
		}

		combatStateComponent.setHasInitiative(true);
	}

}
