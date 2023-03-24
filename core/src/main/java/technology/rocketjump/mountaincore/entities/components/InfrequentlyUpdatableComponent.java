package technology.rocketjump.mountaincore.entities.components;

public interface InfrequentlyUpdatableComponent extends ParentDependentEntityComponent {

	void infrequentUpdate(double elapsedTime);

}
