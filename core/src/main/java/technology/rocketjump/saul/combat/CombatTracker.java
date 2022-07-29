package technology.rocketjump.saul.combat;

import com.google.inject.Singleton;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.Updatable;

@Singleton
public class CombatTracker implements Updatable {

	private GameContext gameContext;

	public void onCombatRoundStart() {
		// Clear list of actions to be resolved in previous round

		// For every combatant
			// Get them to set a new action to resolve in this round (triggers message which this class accepts)
				// combatants with initiative can switch to targeting a different opponent

			// then ask combatants with initiative if they want to switch to defending

			// defending combatants can replenish defense pool

			// organise timing of attacks to be made, spread out from now to 70% of the way through the round
				// combatants attacking each other are set to the same time


	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}

	@Override
	public void update(float deltaTime) {

	}

	@Override
	public boolean runWhilePaused() {
		return false;
	}
}
