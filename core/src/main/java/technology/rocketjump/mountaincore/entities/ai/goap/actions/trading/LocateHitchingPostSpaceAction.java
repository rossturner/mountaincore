package technology.rocketjump.mountaincore.entities.ai.goap.actions.trading;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.mountaincore.entities.tags.HitchingPostTag;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.FurnitureAssignmentRequest;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.rooms.Room;

public class LocateHitchingPostSpaceAction extends Action {


	public LocateHitchingPostSpaceAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		Room groupLocationRoom = null;
		if (parent.parentEntity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour) {
			if (creatureBehaviour.getCreatureGroup() != null && creatureBehaviour.getCreatureGroup().getHomeLocation() != null) {
				MapTile tile = gameContext.getAreaMap().getTile(creatureBehaviour.getCreatureGroup().getHomeLocation());
				if (tile != null && tile.hasRoom()) {
					groupLocationRoom = tile.getRoomTile().getRoom();
				}
			}
		}
		Long groupRoomId = groupLocationRoom == null ? null : groupLocationRoom.getRoomId();

		parent.messageDispatcher.dispatchMessage(MessageType.REQUEST_FURNITURE_ASSIGNMENT,
				new FurnitureAssignmentRequest(HitchingPostTag.class, parent.parentEntity, furnitureToFilter -> {
					// Check hitching post is in the same room as the group's current location
					MapTile furnitureTile = gameContext.getAreaMap().getTile(furnitureToFilter.getLocationComponent().getWorldPosition());
					return furnitureTile != null && furnitureTile.hasRoom() && furnitureTile.getRoomTile().getRoom().getRoomId() == groupRoomId;
				}, assignedFurniture -> {
					if (assignedFurniture == null) {
						completionType = CompletionType.FAILURE;
					} else {
						parent.setAssignedFurnitureId(assignedFurniture.getId());
						parent.setTargetLocation(averageOfFurnitureWorkspaces(assignedFurniture));
						completionType = CompletionType.SUCCESS;
					}
				}));
	}

	private Vector2 averageOfFurnitureWorkspaces(Entity furniture) {
		FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) furniture.getPhysicalEntityComponent().getAttributes();
		FurnitureLayout layout = attributes.getCurrentLayout();
		Vector2 location = furniture.getLocationComponent().getWorldPosition();

		Vector2 average = new Vector2();
		for (FurnitureLayout.Workspace workspace : layout.getWorkspaces()) {
			average.add(location.x + workspace.getAccessedFrom().x, location.y + workspace.getAccessedFrom().y);
		}
		average.scl(1f / layout.getWorkspaces().size());
		return average;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
	}
}
