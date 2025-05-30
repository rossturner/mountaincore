package technology.rocketjump.mountaincore.entities.model.physical.creature.status;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

public class LossOfMainHand extends StatusEffect {

    public LossOfMainHand() {
        super(null, null, null, null);
    }

    @Override
    public void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher) {
        EquippedItemComponent equipped = parentEntity.getOrCreateComponent(EquippedItemComponent.class);
        if (equipped != null && equipped.isMainHandEnabled()) {
            Entity unusableEntity = equipped.disableMainHand();
            InventoryComponent inventoryComponent = parentEntity.getComponent(InventoryComponent.class);
            if (unusableEntity != null && inventoryComponent != null) {
                inventoryComponent.add(unusableEntity, parentEntity, messageDispatcher, gameContext.getGameClock());
            }
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
