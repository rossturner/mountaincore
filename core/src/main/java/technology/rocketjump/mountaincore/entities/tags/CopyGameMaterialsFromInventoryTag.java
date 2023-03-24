package technology.rocketjump.mountaincore.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.apache.commons.lang3.EnumUtils;
import technology.rocketjump.mountaincore.entities.components.CopyGameMaterialsFromInventoryComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;

public class CopyGameMaterialsFromInventoryTag extends Tag {
    @Override
    public String getTagName() {
        return "COPY_GAME_MATERIAL_FROM_INVENTORY";
    }

    @Override
    public boolean isValid(TagProcessingUtils tagProcessingUtils) {
        for (String arg : args) {
            if (!EnumUtils.isValidEnum(GameMaterialType.class, arg)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {
        CopyGameMaterialsFromInventoryComponent copyGameMaterialsFromInventoryComponent = new CopyGameMaterialsFromInventoryComponent();
        copyGameMaterialsFromInventoryComponent.setGameMaterialTypes(args.stream().map(GameMaterialType::valueOf).toList());
        entity.addComponent(copyGameMaterialsFromInventoryComponent);
    }
}
