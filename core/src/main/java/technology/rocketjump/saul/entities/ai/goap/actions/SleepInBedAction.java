package technology.rocketjump.saul.entities.ai.goap.actions;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.Vector2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.saul.assets.entities.tags.BedSleepingPositionTag;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.components.creature.HappinessComponent;
import technology.rocketjump.saul.entities.components.furniture.SleepingPositionComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;
import technology.rocketjump.saul.rooms.Room;
import technology.rocketjump.saul.rooms.RoomTile;

import static technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation.DOWN;
import static technology.rocketjump.saul.assets.entities.tags.BedSleepingPositionTag.BedCreaturePosition.INSIDE_FURNITURE;
import static technology.rocketjump.saul.assets.entities.tags.BedSleepingPositionTag.BedCreaturePosition.ON_GROUND;
import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.SUCCESS;
import static technology.rocketjump.saul.entities.components.creature.HappinessComponent.HappinessModifier.*;
import static technology.rocketjump.saul.misc.VectorUtils.toVector;

/**
 * This extends SleepOnFloorAction purely to share some code regarding sleeping
 */
public class SleepInBedAction extends SleepOnFloorAction {

	private static final int MIN_AVERAGE_BEDROOM_SIZE = 6;

	public SleepInBedAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (!isAsleep()) {
			getIntoAssignedBedAndSleep(gameContext);
		} else {

			Entity bedEntity = gameContext.getEntities().get(parent.getAssignedFurnitureId());
			if (bedEntity == null) {
				completionType = FAILURE;
				return;
			}
			MapTile bedTile = gameContext.getAreaMap().getTile(bedEntity.getLocationComponent(true).getWorldPosition());
			Room bedroom = null;
			if (bedTile != null && bedTile.getRoomTile() != null) {
				bedroom = bedTile.getRoomTile().getRoom();
			}

			Entity assignedFurniture = gameContext.getEntities().get(parent.getAssignedFurnitureId());
			SleepingPositionComponent sleepingPositionComponent = assignedFurniture.getComponent(SleepingPositionComponent.class);
			if (sleepingPositionComponent.getBedCreaturePosition().equals(ON_GROUND)) {
				parent.parentEntity.getComponent(HappinessComponent.class).add(SLEPT_ON_GROUND);
			}

			if (bedroom == null || bedroomIsShared(bedroom, gameContext)) {
				parent.parentEntity.getComponent(HappinessComponent.class).add(SLEPT_IN_SHARED_BEDROOM);
				if (bedroom != null && bedroomIsSmall(bedroom)) {
					parent.parentEntity.getComponent(HappinessComponent.class).add(SLEPT_IN_SMALL_BEDROOM);
				}
			} else {
				if (bedroom.isFullyEnclosed()) {
					parent.parentEntity.getComponent(HappinessComponent.class).add(SLEPT_IN_ENCLOSED_BEDROOM);
				}
				if (bedroomIsSmall(bedroom)) {
					parent.parentEntity.getComponent(HappinessComponent.class).add(SLEPT_IN_SMALL_BEDROOM);
				}
			}


			checkForWakingUp(gameContext);

			if (completionType == SUCCESS) {
				getOutOfBed(gameContext);
			}
		}
	}

	private boolean bedroomIsSmall(Room bedroom) {
		return bedroom.getRoomTiles().size() < MIN_AVERAGE_BEDROOM_SIZE;
	}

	private boolean bedroomIsShared(Room room, GameContext gameContext) {
		Entity bedEntity = gameContext.getEntities().get(parent.getAssignedFurnitureId());
		for (RoomTile roomTile : room.getRoomTiles().values()) {
			MapTile tile = roomTile.getTile();
			for (Entity entity : tile.getEntities()) {
				if (entity.getId() != bedEntity.getId() && entity.getType().equals(EntityType.FURNITURE)) {
					if (entity.getTag(BedSleepingPositionTag.class) != null) {
						// This is another bed
						FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes();
						Long assignedToEntityId = attributes.getAssignedToEntityId();
						if (assignedToEntityId != null && assignedToEntityId != parent.parentEntity.getId()) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	@Override
	public void actionInterrupted(GameContext gameContext) {
		if (isAsleep()) {
			changeToAwake(gameContext);
		}
		if (parent.parentEntity.getLocationComponent(true).getContainerEntity() != null) {
			getOutOfBed(gameContext);
		}
		completionType = SUCCESS;
	}

	@Override
	public String getDescriptionOverrideI18nKey() {
		return "ACTION.SLEEP_ON_FLOOR.DESCRIPTION";
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		// No state to write
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		// No state to read
	}

	private void getIntoAssignedBedAndSleep(GameContext gameContext) {
		Entity assignedFurniture = gameContext.getEntities().get(parent.getAssignedFurnitureId());
		if (assignedFurniture == null) {
			Logger.error("Could not find bed assigned for sleeping in");
			completionType = FAILURE;
			return;
		}
		SleepingPositionComponent sleepingPositionComponent = assignedFurniture.getComponent(SleepingPositionComponent.class);
		if (sleepingPositionComponent.getBedCreaturePosition().equals(ON_GROUND)) {
			changeToSleeping(gameContext);
		} else if (!locatedInFurnitureWorkspace(assignedFurniture)) {
			completionType = FAILURE;
		} else {
			InventoryComponent inventoryComponent = assignedFurniture.getOrCreateComponent(InventoryComponent.class);
			inventoryComponent.add(parent.parentEntity, assignedFurniture, parent.messageDispatcher, gameContext.getGameClock());
			changeToSleeping(gameContext);
			updateForSleepingOrientation(gameContext, assignedFurniture);
		}
	}

	private void getOutOfBed(GameContext gameContext) {
		Entity assignedFurniture = gameContext.getEntities().get(parent.getAssignedFurnitureId());
		if (assignedFurniture == null) {
			Logger.error("Can't get out of bed! Can't find assigned furniture with ID " + parent.getAssignedFurnitureId());
			completionType = FAILURE;
			return;
		}
		SleepingPositionComponent sleepingPositionComponent = assignedFurniture.getComponent(SleepingPositionComponent.class);
		if (sleepingPositionComponent.getBedCreaturePosition().equals(INSIDE_FURNITURE)) {
			InventoryComponent inventoryComponent = assignedFurniture.getOrCreateComponent(InventoryComponent.class);
			inventoryComponent.remove(parent.parentEntity.getId());

			Vector2 furnitureLocation = assignedFurniture.getLocationComponent(true).getWorldPosition().cpy();
			parent.parentEntity.getLocationComponent(true).setWorldPosition(furnitureLocation, false);

			FurnitureLayout.Workspace navigableWorkspace = FurnitureLayout.getAnyNavigableWorkspace(assignedFurniture, gameContext.getAreaMap());
			if (navigableWorkspace != null) {
				Vector2 workspaceLocation = toVector(navigableWorkspace.getAccessedFrom());
				parent.parentEntity.getLocationComponent(true).setWorldPosition(workspaceLocation, true);
			} //  Else we can no longer get out of bed into a workspace, place onto bed tile instead
			parent.messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, parent.parentEntity);
		}
	}

	private void updateForSleepingOrientation(GameContext gameContext, Entity assignedFurniture) {
		SleepingPositionComponent sleepingPositionComponent = assignedFurniture.getComponent(SleepingPositionComponent.class);
		EntityAssetOrientation sleepingOrientation = sleepingPositionComponent.getSleepingOrientation();

		parent.parentEntity.getLocationComponent(true).setFacing(sleepingOrientation.toVector2());
		if (sleepingOrientation.toVector2().x > 0) {
			parent.parentEntity.getLocationComponent(true).setRotation(260f + (gameContext.getRandom().nextFloat() * 20f));
		} else if (sleepingOrientation.toVector2().x < 0) {
			parent.parentEntity.getLocationComponent(true).setRotation(80f + (gameContext.getRandom().nextFloat() * 20f));
		} else if (sleepingOrientation.equals(EntityAssetOrientation.UP)) {
			parent.parentEntity.getLocationComponent(true).setRotation(180);
			parent.parentEntity.getLocationComponent(true).setFacing(DOWN.toVector2());
		} else {
			parent.parentEntity.getLocationComponent(true).setRotation(0);
		}
		parent.messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, parent.parentEntity);
	}

	private boolean locatedInFurnitureWorkspace(Entity assignedFurniture) {
		Vector2 parentEntityPosition = parent.parentEntity.getLocationComponent(true).getWorldPosition();
		Vector2 furniturePosition = assignedFurniture.getLocationComponent(true).getWorldPosition();
		FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) assignedFurniture.getPhysicalEntityComponent().getAttributes();
		for (FurnitureLayout.Workspace workspace : attributes.getCurrentLayout().getWorkspaces()) {
			Vector2 workspacePosition = furniturePosition.cpy().add(workspace.getAccessedFrom().x, workspace.getAccessedFrom().y);
			if (workspacePosition.dst2(parentEntityPosition) < 1) {
				return true;
			}
		}
		return false;
	}


}
