package technology.rocketjump.saul.entities.ai.goap.actions.crafting;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.saul.entities.ai.goap.actions.Action;
import technology.rocketjump.saul.entities.components.LiquidContainerComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.HaulingComponent;
import technology.rocketjump.saul.entities.tags.ItemUsageSoundTag;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.model.Job;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.RequestSoundAssetMessage;
import technology.rocketjump.saul.messaging.types.RequestSoundMessage;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;

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

				GridPoint2 parentPosition = toGridPoint(parent.parentEntity.getLocationComponent(true).getWorldOrParentPosition());
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
						soundAsset, parent.parentEntity.getId(), parent.parentEntity.getLocationComponent(true).getWorldOrParentPosition(), null));
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
