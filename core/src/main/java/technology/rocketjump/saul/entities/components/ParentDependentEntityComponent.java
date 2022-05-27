package technology.rocketjump.saul.entities.components;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;

public interface ParentDependentEntityComponent extends EntityComponent {

	void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext);

}
