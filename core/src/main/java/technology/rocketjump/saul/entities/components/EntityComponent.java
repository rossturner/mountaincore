package technology.rocketjump.saul.entities.components;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.model.ChildPersistable;

public interface EntityComponent extends ChildPersistable {

	EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext);

}
