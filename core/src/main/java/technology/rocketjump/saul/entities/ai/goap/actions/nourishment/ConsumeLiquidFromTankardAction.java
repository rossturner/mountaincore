package technology.rocketjump.saul.entities.ai.goap.actions.nourishment;

import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.cooking.model.FoodAllocation;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.components.LiquidContainerComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.messaging.MessageType;

import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.SUCCESS;

public class ConsumeLiquidFromTankardAction extends ConsumeLiquidFromContainerAction {

    public ConsumeLiquidFromTankardAction(AssignedGoal parent) {
        super(parent);
    }

    @Override
    public void update(float deltaTime, GameContext gameContext) {
        // Assuming we're using a food allocation to point at the tankard
        FoodAllocation foodAllocation = parent.getFoodAllocation();
        if (foodAllocation == null || foodAllocation.getTargetEntity() == null) {
            Logger.error("Unexpected null in " + this.getSimpleName());
            completionType = FAILURE;
            return;
        }

        elapsedTime += deltaTime;
        if (elapsedTime > TIME_TO_SPEND_DRINKING_SECONDS) {
            Entity tankardEntity = parent.getFoodAllocation().getTargetEntity();
            LiquidContainerComponent liquidContainerComponent = tankardEntity.getOrCreateComponent(LiquidContainerComponent.class);
            GameMaterial consumedLiquid = liquidContainerComponent.getTargetLiquidMaterial();
            effectsOfDrinkConsumption(consumedLiquid, null, gameContext);
            parent.messageDispatcher.dispatchMessage(MessageType.DESTROY_ENTITY, tankardEntity);
            completionType = SUCCESS;
        }
    }

}
