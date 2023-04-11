package technology.rocketjump.mountaincore.entities.behaviour.creature;

public enum TraderGroupStage {

		SPAWNED(8.0),
		ARRIVING(12.0),
		MOVING_TO_TRADE_DEPOT(20.0),
		ARRIVED_AT_TRADE_DEPOT(8.0),
		TRADING(32.0),
		PREPARING_TO_LEAVE(8.0),
		LEAVING(8.0);

	public final double maxHoursInStage;

	TraderGroupStage(double maxHoursInStage) {
		this.maxHoursInStage = maxHoursInStage;
	}

	public TraderGroupStage nextStage() {
			return values()[(this.ordinal() + 1) % values().length];
		}

}
