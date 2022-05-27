package technology.rocketjump.saul.entities.components;

import technology.rocketjump.saul.entities.components.humanoid.SteeringComponent;
import technology.rocketjump.saul.gamecontext.GameContext;

public interface BehaviourComponent extends ParentDependentEntityComponent {

	void update(float deltaTime, GameContext gameContext);

	void infrequentUpdate(GameContext gameContext);

	SteeringComponent getSteeringComponent();

	boolean isUpdateEveryFrame();

	boolean isUpdateInfrequently(); // Note this needs to be true if Entity is going to have any InfrequentUpdateComponents

	boolean isJobAssignable();

}
