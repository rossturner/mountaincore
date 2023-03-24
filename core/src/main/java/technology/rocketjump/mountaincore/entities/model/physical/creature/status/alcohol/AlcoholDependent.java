package technology.rocketjump.mountaincore.entities.model.physical.creature.status.alcohol;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.ai.memory.Memory;
import technology.rocketjump.mountaincore.entities.ai.memory.MemoryType;
import technology.rocketjump.mountaincore.entities.components.creature.MemoryComponent;
import technology.rocketjump.mountaincore.entities.model.physical.creature.status.StatusEffect;
import technology.rocketjump.mountaincore.gamecontext.GameContext;

import java.util.Deque;

public class AlcoholDependent extends StatusEffect {

    public AlcoholDependent() {
        super(AlcoholWithdrawl.class, 24.0 * 3.0, null, null);
    }

    @Override
    public void applyOngoingEffect(GameContext gameContext, MessageDispatcher messageDispatcher) {
        MemoryComponent memoryComponent = parentEntity.getOrCreateComponent(MemoryComponent.class);
        Deque<Memory> shortTermMemories = memoryComponent.getShortTermMemories(gameContext.getGameClock());

        shortTermMemories.stream().filter(mem -> mem.getType().equals(MemoryType.CONSUMED_ALCOHOLIC_DRINK)).findAny().ifPresent(memoryOfDrink -> {
            // Reset time applied if a drink has been consumed recently
            this.timeApplied = 0;
        });
    }

    @Override
    public boolean checkForRemoval(GameContext gameContext) {
        // Only removed by time expiry to next stage
        return false;
    }

    @Override
    public String getI18Key() {
        return null;
    }
}
