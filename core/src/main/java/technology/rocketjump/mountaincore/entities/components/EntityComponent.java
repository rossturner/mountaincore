package technology.rocketjump.mountaincore.entities.components;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.persistence.model.ChildPersistable;

public interface EntityComponent extends ChildPersistable {

	EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext);

}
