package technology.rocketjump.saul.entities.behaviour;

import technology.rocketjump.saul.entities.components.AttachedLightSourceComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;

public class AttachedLightSourceBehaviour {

	public static void infrequentUpdate(GameContext gameContext, Entity parentEntity) {
		AttachedLightSourceComponent attachedLightSourceComponent = parentEntity.getComponent(AttachedLightSourceComponent.class);
		if (attachedLightSourceComponent != null) {

		}
	}
}
