package technology.rocketjump.saul.entities.model.physical.creature.status.alcohol;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.entities.ai.memory.Memory;
import technology.rocketjump.saul.entities.components.creature.HappinessComponent;
import technology.rocketjump.saul.entities.components.creature.MemoryComponent;
import technology.rocketjump.saul.entities.model.physical.creature.status.StatusEffect;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.StatusMessage;

import java.util.Deque;

import static technology.rocketjump.saul.entities.ai.memory.MemoryType.CONSUMED_ALCOHOLIC_DRINK;

public class AlcoholWithdrawl extends StatusEffect {

    public AlcoholWithdrawl() {
        super(null, 24.0 * 3.0, null, null);
    }

    @Override
    public void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher) {
        if (recentDrink(gameContext)) {
            messageDispatcher.dispatchMessage(MessageType.REMOVE_STATUS, new StatusMessage(parentEntity, this.getClass(), null, null));
            messageDispatcher.dispatchMessage(MessageType.APPLY_STATUS, new StatusMessage(parentEntity, AlcoholDependent.class, null, null));
        } else {
            HappinessComponent happinessComponent = parentEntity.getComponent(HappinessComponent.class);
            if (happinessComponent != null) {
                happinessComponent.add(HappinessComponent.HappinessModifier.ALCOHOL_WITHDRAWAL);
            }
        }
    }

    private boolean recentDrink(GameContext gameContext) {
        MemoryComponent memoryComponent = parentEntity.getOrCreateComponent(MemoryComponent.class);
        Deque<Memory> shortTermMemories = memoryComponent.getShortTermMemories(gameContext.getGameClock());
        return shortTermMemories.stream().anyMatch(mem -> mem.getType().equals(CONSUMED_ALCOHOLIC_DRINK));
    }

    @Override
    public boolean checkForRemoval(GameContext gameContext) {
        // As this does not have a nextStage property, need to remove this when applied for X hours
        return timeApplied > this.hoursUntilNextStage;
    }

    @Override
    public String getI18Key() {
        return "STATUS.ALCOHOL_WITHDRAWL";
    }
}
