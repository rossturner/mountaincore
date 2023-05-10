package technology.rocketjump.mountaincore.entities.ai.goap;

public enum SpecialGoal {

	IDLE("Idle goal"),
	DO_NOTHING("Do nothing goal"),
	HAUL_ITEM("Haul item goal"),
	DUMP_ITEM("Dump item goal"),
	TRANSFER_LIQUID("Transfer liquid goal"),
	TRANSFER_LIQUID_FOR_CRAFTING("Transfer liquid for crafting goal"),
	MOVE_LIQUID_IN_ITEM("Move liquid in item goal"),
	REMOVE_LIQUID("Remove liquid goal"),
	DUMP_LIQUID("Dump liquid goal"),
	PLACE_ITEM("Place item goal"),
	ROLL_ON_FLOOR("Roll on floor goal"),
	DOUSE_SELF("Douse self goal"),
	EXTINGUISH_FIRE("Extinguish fire goal"),
	CREATE_CAMPFIRE("Create camp fire"),
	MOVE_GROUP_TOWARDS_SETTLEMENT("Move group towards settlement"),
	CRAFTING_JOB("Crafting job goal"),
	ABANDON_JOB("Abandon job goal"),
	GO_TO_SETTLEMENT_GOAL("Go to settlement goal");

	public final String goalName;
	Goal goalInstance;

	SpecialGoal(String goalName) {
		this.goalName = goalName;
	}

	public Goal getInstance() {
		return goalInstance;
	}
}
