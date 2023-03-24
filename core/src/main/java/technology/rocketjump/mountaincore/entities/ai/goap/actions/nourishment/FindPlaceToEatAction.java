package technology.rocketjump.mountaincore.entities.ai.goap.actions.nourishment;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.components.furniture.FeastingLocationComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import java.util.Optional;

import static technology.rocketjump.mountaincore.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.mountaincore.entities.ai.goap.actions.Action.CompletionType.SUCCESS;

/**
 * This Action attempts to find a table and chair in the current room to eat at
 */
public class FindPlaceToEatAction extends Action {

	public FindPlaceToEatAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		Vector2 parentPosition = parent.parentEntity.getLocationComponent().getWorldOrParentPosition();
		MapTile parentTile = gameContext.getAreaMap().getTile(parentPosition);
		if (parentTile == null || parentTile.getRoomTile() == null) {
			completionType = FAILURE;
		} else {
			for (GridPoint2 roomTilePosition : parentTile.getRoomTile().getRoom().getRoomTiles().keySet()) {
				MapTile tile = gameContext.getAreaMap().getTile(roomTilePosition);
				Optional<Entity> unassignedFeastingLocation = tile.getEntities().stream().filter(entity -> entity.getType().equals(EntityType.FURNITURE) &&
						entity.getComponent(FeastingLocationComponent.class) != null &&
						((FurnitureEntityAttributes) entity.getPhysicalEntityComponent().getAttributes()).getAssignedToEntityId() == null)
						.findFirst();
				if (unassignedFeastingLocation.isPresent()) {
					((FurnitureEntityAttributes)unassignedFeastingLocation.get().getPhysicalEntityComponent().getAttributes()).setAssignedToEntityId(parent.parentEntity.getId());
					parent.setAssignedFurnitureId(unassignedFeastingLocation.get().getId());
					completionType = SUCCESS;
					break;
				}
			}
			if (parent.getAssignedFurnitureId() == null) {
				completionType = FAILURE;
			}
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		// No state
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		// No state
	}
}
