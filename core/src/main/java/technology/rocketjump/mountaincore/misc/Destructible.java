package technology.rocketjump.mountaincore.misc;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public interface Destructible {

	void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext);

}
