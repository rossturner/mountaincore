package technology.rocketjump.saul.entities.ai.combat;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.saul.entities.components.creature.CombatStateComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

public class DefensiveCombatAction extends CombatAction {

	public DefensiveCombatAction(Entity parentEntity) {
		super(parentEntity);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext, MessageDispatcher messageDispatcher) {
		CombatStateComponent combatStateComponent = parentEntity.getComponent(CombatStateComponent.class);
		Entity opponentEntity = gameContext.getEntities().get(combatStateComponent.getTargetedOpponentId());
		if (opponentEntity != null) {
			// Face towards opponent
			Vector2 parentPosition = parentEntity.getLocationComponent().getWorldOrParentPosition();
			Vector2 opponentPosition = opponentEntity.getLocationComponent().getWorldOrParentPosition();
			parentEntity.getLocationComponent().setFacing(opponentPosition.cpy().sub(parentPosition));
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
		CreatureCombatStats combatStats = new CreatureCombatStats(parentEntity);
		CombatStateComponent combatStateComponent = parentEntity.getComponent(CombatStateComponent.class);

		if (!combatStateComponent.isHasInitiative()) {
			combatStateComponent.setHasInitiative(true);

			int defensePool = combatStateComponent.getDefensePool();
			defensePool += combatStats.defensePoolRegainedPerDefensiveRound();
			defensePool = Math.min(defensePool, combatStats.maxDefensePool());
			combatStateComponent.setDefensePool(defensePool);
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {

	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {

	}
}
