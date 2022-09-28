package technology.rocketjump.saul.entities.behaviour.creature;

public enum InvasionStage {

	ARRIVING(0.5),
	PREPARING(48.0),
	RAIDING(12.0),
	RETREATING(100.0);

	public final double durationHours;

	InvasionStage(double durationHours) {
		this.durationHours = durationHours;
	}
}
