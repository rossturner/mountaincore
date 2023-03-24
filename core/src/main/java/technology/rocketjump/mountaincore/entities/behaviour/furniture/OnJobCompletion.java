package technology.rocketjump.mountaincore.entities.behaviour.furniture;

import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public interface OnJobCompletion {

	void jobCompleted(GameContext gameContext, Entity completedByEntity);

}
