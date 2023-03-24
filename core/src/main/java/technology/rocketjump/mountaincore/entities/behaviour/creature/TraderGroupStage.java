package technology.rocketjump.mountaincore.entities.behaviour.creature;

public enum TraderGroupStage {

		SPAWNED,
		ARRIVING,
		MOVING_TO_TRADE_DEPOT,
		ARRIVED_AT_TRADE_DEPOT,
		TRADING,
		PREPARING_TO_LEAVE,
		LEAVING;

		public TraderGroupStage nextStage() {
			return values()[(this.ordinal() + 1) % values().length];
		}

}
