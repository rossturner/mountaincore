package technology.rocketjump.mountaincore.entities.ai.goap.actions;

import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.nourishment.ConsumeLiquidFromContainerAction;
import technology.rocketjump.mountaincore.entities.components.LiquidAllocation;
import technology.rocketjump.mountaincore.entities.components.creature.StatusComponent;
import technology.rocketjump.mountaincore.entities.model.physical.creature.status.OnFireStatus;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.RequestSoundAssetMessage;
import technology.rocketjump.mountaincore.messaging.types.RequestSoundMessage;

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