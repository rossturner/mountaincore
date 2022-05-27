package technology.rocketjump.saul.entities.ai.goap.actions;


import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.components.LiquidAllocation;
import technology.rocketjump.saul.entities.components.LiquidContainerComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.HaulingComponent;
import technology.rocketjump.saul.entities.tags.ItemUsageSoundTag;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.RequestSoundMessage;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.zones.Zone;
import technology.rocketjump.saul.zones.ZoneClassification;

import java.util.Optional;

import static technology.rocketjump.saul.entities.components.LiquidAllocation.LiquidAllocationType.FROM_LIQUID_CONTAINER;
import static technology.rocketjump.saul.jobs.LiquidMessageHandler.getLiquidContainerFromFurnitureInTile;
import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.saul.rooms.HaulingAllocation.AllocationPositionType.ZONE;

/**
 * This action fills the equipped LiquidContainer from the target zone
 */
public class FillContainerAction extends Action {
	public FillContainerAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		// Assuming zone is defined as assigned allocation target
		if (parent.getAssignedHaulingAllocation() != null && ZONE.equals(parent.getAssignedHaulingAllocation().getTargetPositionType())) {
			GridPoint2 targetPosition = parent.getAssignedHaulingAllocation().getTargetPosition();
			GridPoint2 parentEntityPosition = toGridPoint(parent.parentEntity.getLocationComponent().getWorldPosition());
			if (targetPosition.equals(parentEntityPosition)) {
				// Find correct zone
				MapTile currentTile = gameContext.getAreaMap().getTile(parentEntityPosition);
				Optional<Zone> filteredZone = currentTile.getZones().stream()
						.filter(zone -> zone.getClassification().getZoneType().equals(ZoneClassification.ZoneType.LIQUID_SOURCE))
						.filter(zone -> parent.getAssignedJob().getLiquidAllocation() == null || parent.getAssignedJob().getLiquidAllocation().getLiquidMaterial().equals(zone.getClassification().getTargetMaterial()))
						.findFirst();
				if (filteredZone.isPresent()) {
					GameMaterial targetMaterial = filteredZone.get().getClassification().getTargetMaterial();
					HaulingComponent haulingComponent = parent.parentEntity.getComponent(HaulingComponent.class);
					if (haulingComponent != null && haulingComponent.getHauledEntity() != null) {
						Entity hauledItem = haulingComponent.getHauledEntity();
						LiquidContainerComponent hauledLiquidContainer = hauledItem.getComponent(LiquidContainerComponent.class);
						if (hauledLiquidContainer != null) {
							hauledLiquidContainer.setTargetLiquidMaterial(targetMaterial);

							float amountToTransfer = hauledLiquidContainer.getMaxLiquidCapacity() - hauledLiquidContainer.getLiquidQuantity();
							LiquidAllocation removedAllocation = removeLiquidFromSource(gameContext);
							if (removedAllocation != null) {
								amountToTransfer = Math.min(amountToTransfer, removedAllocation.getAllocationAmount());
							}

							hauledLiquidContainer.setLiquidQuantity(hauledLiquidContainer.getLiquidQuantity() + amountToTransfer);

							ItemUsageSoundTag itemUsageSoundTag = hauledItem.getTag(ItemUsageSoundTag.class);
							if (itemUsageSoundTag != null && itemUsageSoundTag.getSoundAsset() != null) {
								parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(
										itemUsageSoundTag.getSoundAsset(), parent.parentEntity.getId(), parent.parentEntity.getLocationComponent().getWorldOrParentPosition(), null));
							}


							completionType = CompletionType.SUCCESS;
							return;
						}
					}
				}
			}
		}

		// Otherwise something went wrong and this failed
		completionType = CompletionType.FAILURE;
	}

	private LiquidAllocation removeLiquidFromSource(GameContext gameContext) {
		LiquidAllocation cancelledAllocation = null;
		if (parent.getLiquidAllocation() != null) {
			if (parent.getLiquidAllocation().getType().equals(FROM_LIQUID_CONTAINER)) {
				MapTile targetTile = gameContext.getAreaMap().getTile(parent.getLiquidAllocation().getTargetZoneTile().getTargetTile());
				LiquidContainerComponent sourceContainerComponent = getLiquidContainerFromFurnitureInTile(targetTile);
				if (sourceContainerComponent != null) {
					cancelledAllocation = sourceContainerComponent.cancelAllocationAndDecrementQuantity(parent.getLiquidAllocation());
				}
			}
			parent.setLiquidAllocation(null);
		}
		return cancelledAllocation;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		// No state to write
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		// No state to read
	}
}
