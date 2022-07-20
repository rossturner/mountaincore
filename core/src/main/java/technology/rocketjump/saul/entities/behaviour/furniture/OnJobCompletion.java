package technology.rocketjump.saul.entities.behaviour.furniture;

import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;

public interface OnJobCompletion {

	void jobCompleted(GameContext gameContext, Entity completedByEntity);

}
