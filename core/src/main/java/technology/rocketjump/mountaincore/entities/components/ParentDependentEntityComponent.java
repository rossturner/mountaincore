package technology.rocketjump.mountaincore.entities.components;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public interface ParentDependentEntityComponent extends EntityComponent {

	void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext);

}
