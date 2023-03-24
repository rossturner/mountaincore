package technology.rocketjump.mountaincore.entities.ai.goap.actions.crafting;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.components.LiquidContainerComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.HaulingComponent;
import technology.rocketjump.mountaincore.entities.tags.ItemUsageSoundTag;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.jobs.model.Job;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.RequestSoundAssetMessage;
import technology.rocketjump.mountaincore.messaging.types.RequestSoundMessage;
import technology.rocketjump.mountaincore.misc.VectorUtils;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

public class PourIntoTargetAction extends Action {

	public PourIntoTargetAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		HaulingComponent haulingComponent = parent.parentEntity.getComponent(HaulingComponent.class);
		if (haulingComponent != null && haulingComponent.getHauledEntity() != null) {
			LiquidContainerComponent hauledLiquidContainer = haulingComponent.getHauledEntity().getComponent(LiquidContainerComponent.class);
			if (hauledLiquidContainer != null && hauledLiquidContainer.getLiquidQuantity() > 0) {

				GridPoint2 parentPosition = VectorUtils.toGridPoint(parent.parentEntity.getLocationComponent().getWorldOrParentPosition());
				Job assignedJob = parent.getParentGoal().getAssignedJob();
				if (parentPosition.equals(assignedJob.getJobLocation())) {
					Entity targetEntity = gameContext.getEntities().get(assignedJob.getTargetId());
					if (targetEntity != null) {
						LiquidContainerComponent targetLiquidContainer = targetEntity.getComponent(LiquidContainerComponent.class);
						if (targetLiquidContainer != null) {
							transferLiquid(haulingComponent.getHauledEntity(), hauledLiquidContainer, targetLiquidContainer);
							return;
						}
					}
				}
			}
		}
		completionType = CompletionType.FAILURE;
	}

	private void transferLiquid(Entity hauledItem, LiquidContainerComponent hauledLiquidContainer, LiquidContainerComponent targetLiquidContainer) {
		float amountToTransfer = Math.min(hauledLiquidContainer.getLiquidQuantity(), targetLiquidContainer.getMaxLiquidCapacity() - targetLiquidContainer.getLiquidQuantity());

		if (targetLiquidContainer.getTargetLiquidMaterial() == null || targetLiquidContainer.isEmpty()) {
			targetLiquidContainer.setTargetLiquidMaterial(hauledLiquidContainer.getTargetLiquidMaterial());
			targetLiquidContainer.setLiquidQuantity(amountToTransfer);
		} else if (!hauledLiquidContainer.getTargetLiquidMaterial().equals(targetLiquidContainer.getTargetLiquidMaterial())) {
			Logger.error("Liquid material mismatch when transferring liquids");
			completionType = CompletionType.FAILURE;
			return;
		} else {
			targetLiquidContainer.setLiquidQuantity(targetLiquidContainer.getLiquidQuantity() + amountToTransfer);
		}
		targetLiquidContainer.assignCraftingAllocation(amountToTransfer);

		ItemUsageSoundTag itemUsageSoundTag = hauledItem.getTag(ItemUsageSoundTag.class);
		if (itemUsageSoundTag != null && itemUsageSoundTag.getSoundAssetName() != null) {
			parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND_ASSET, new RequestSoundAssetMessage(itemUsageSoundTag.getSoundAssetName(), (soundAsset) -> {
				parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(
						soundAsset, parent.parentEntity.getId(), parent.parentEntity.getLocationComponent().getWorldOrParentPosition(), null));
			}));
		}

		// TODO splash particle effect

		hauledLiquidContainer.setLiquidQuantity(0);
		hauledLiquidContainer.setTargetLiquidMaterial(null);
		completionType = CompletionType.SUCCESS;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
	}
}
