package technology.rocketjump.mountaincore.entities.ai.goap;

public class SwitchGoalException extends Exception {

	public final Goal target;

	public SwitchGoalException(SpecialGoal goalToSwitchTo) {
		this.target = goalToSwitchTo.goalInstance;
	}

}
