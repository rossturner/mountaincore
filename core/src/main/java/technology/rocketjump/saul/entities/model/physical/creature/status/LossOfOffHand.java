package technology.rocketjump.saul.entities.model.physical.creature.status;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.saul.gamecontext.GameContext;

public class LossOfOffHand extends StatusEffect {

    public LossOfOffHand() {
        super(null, null, null);
    }

    @Override
    public void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher) {
        EquippedItemComponent equipped = parentEntity.getComponent(EquippedItemComponent.class);
        if (equipped != null && equipped.getOffHandItem() != null) {
            equipped.disableOffHand();
        }
    }

    @Override
    public boolean checkForRemoval(GameContext gameContext) {
        return false;
    }

    @Override
    public String getI18Key() {
        return "STATUS.LOSS_MAIN_HAND";
    }
}
