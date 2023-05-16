package technology.rocketjump.mountaincore.entities.tags;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.apache.commons.lang3.math.NumberUtils;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public class LiquidContainerRefillThresholdTag extends Tag {
    @Override
    public String getTagName() {
        return "LIQUID_CONTAINER_REFILL_THRESHOLD";
    }

    @Override
    public boolean isValid(TagProcessingUtils tagProcessingUtils) {
        return args.size() == 1 && NumberUtils.isNumber(args.get(0));
    }

    @Override
    public void apply(Entity entity, TagProcessingUtils tagProcessingUtils, MessageDispatcher messageDispatcher, GameContext gameContext) {

    }

    public int getRefillThreshold() {
        return Integer.parseInt(args.get(0));
    }
}
