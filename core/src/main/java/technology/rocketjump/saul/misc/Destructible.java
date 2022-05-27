package technology.rocketjump.saul.misc;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;

public interface Destructible {

	void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext);

}
