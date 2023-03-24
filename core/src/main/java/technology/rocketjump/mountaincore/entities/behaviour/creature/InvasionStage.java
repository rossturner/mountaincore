package technology.rocketjump.mountaincore.entities.behaviour.creature;

public enum InvasionStage {

	ARRIVING(0.2),
	PREPARING(24.0),
	RAIDING(18.0),
	RETREATING(100.0);

	public final double durationHours;

	InvasionStage(double durationHours) {
		this.durationHours = durationHours;
	}
}
