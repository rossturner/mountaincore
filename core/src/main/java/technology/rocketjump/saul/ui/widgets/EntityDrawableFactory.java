package technology.rocketjump.saul.ui.widgets;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.rendering.entities.EntityRenderer;

@Singleton
public class EntityDrawableFactory {

    private final EntityRenderer entityRenderer;
    private final MessageDispatcher messageDispatcher;

    @Inject
    public EntityDrawableFactory(EntityRenderer entityRenderer, MessageDispatcher messageDispatcher) {
        this.entityRenderer = entityRenderer;
        this.messageDispatcher = messageDispatcher;
    }

    public EntityDrawable create(Entity entity) {
        return new EntityDrawable(entity, entityRenderer, true, messageDispatcher);
    }

}
