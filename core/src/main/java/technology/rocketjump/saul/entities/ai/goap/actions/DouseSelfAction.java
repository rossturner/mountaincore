package technology.rocketjump.saul.entities.ai.goap.actions;

import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.actions.nourishment.ConsumeLiquidFromContainerAction;
import technology.rocketjump.saul.entities.components.LiquidAllocation;
import technology.rocketjump.saul.entities.components.humanoid.StatusComponent;
import technology.rocketjump.saul.entities.model.physical.creature.status.OnFireStatus;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.RequestSoundAssetMessage;
import technology.rocketjump.saul.messaging.types.RequestSoundMessage;

public class DouseSelfAction extends ConsumeLiquidFromContainerAction {

	public DouseSelfAction(AssignedGoal parent) {
		super(parent);
	}

	protected float getTimeToSpendDrinking() {
		return 1.5f;
	}

	@Override
	protected void effectsOfDrinkConsumption(GameMaterial consumedLiquid, LiquidAllocation liquidAllocation, GameContext gameContext) {
		if (consumedLiquid.isQuenchesThirst()) {
			StatusComponent statusComponent = parent.parentEntity.getComponent(StatusComponent.class);
			statusComponent.remove(OnFireStatus.class);

			parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND_ASSET, new RequestSoundAssetMessage("WaterSizzle", (soundAsset) -> {
				if (soundAsset != null) {
					parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(soundAsset, parent.parentEntity));
				}
			}));
		} else {
			Logger.error(this.getSimpleName() + " does not work with " + consumedLiquid.getMaterialName() + " as it does not quench thirst");
		}
	}

}