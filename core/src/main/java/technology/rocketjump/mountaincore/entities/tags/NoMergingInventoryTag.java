package technology.rocketjump.mountaincore.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public class NoMergingInventoryTag extends Tag{
    @Override
    public String getTagName() {
        return "NO_MERGING_INVENTORY";
    }

    @Override
    public boolean isValid(TagProcessingUtils tagProcessingUtils) {
        return true;
    }

    @Override
    public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
        entity.getOrCreateComponent(InventoryComponent.class).setNoMerging(true);
    }
}
