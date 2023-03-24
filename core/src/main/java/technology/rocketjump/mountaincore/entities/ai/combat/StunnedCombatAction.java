package technology.rocketjump.mountaincore.entities.ai.combat;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public class StunnedCombatAction extends CombatAction {

	public StunnedCombatAction(Entity parentEntity) {
		super(parentEntity);
		this.completed = true;
	}

	@Override
	public void update(float deltaTime, GameContext gameContext, MessageDispatcher messageDispatcher) {
	}

	@Override
	public void interrupted(MessageDispatcher messageDispatcher) {

	}

	@Override
	public boolean completesInOneRound() {
		return true;
	}

}
