package technology.rocketjump.saul.entities.components;

public interface InfrequentlyUpdatableComponent extends ParentDependentEntityComponent {

	void infrequentUpdate(double elapsedTime);

}
