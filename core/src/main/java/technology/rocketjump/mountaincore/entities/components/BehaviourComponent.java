package technology.rocketjump.mountaincore.entities.components;

import technology.rocketjump.mountaincore.entities.components.creature.SteeringComponent;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public interface BehaviourComponent extends ParentDependentEntityComponent {

	void update(float deltaTime);

	void updateWhenPaused();

	void infrequentUpdate(GameContext gameContext);

	SteeringComponent getSteeringComponent();

	boolean isUpdateEveryFrame();

	boolean isUpdateInfrequently(); // Note this needs to be true if Entity is going to have any InfrequentUpdateComponents

	boolean isJobAssignable();

}
