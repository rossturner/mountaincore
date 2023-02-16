package technology.rocketjump.saul.entities.ai.goap.actions.nourishment;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.actions.Action;
import technology.rocketjump.saul.entities.components.furniture.FeastingLocationComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.Optional;

import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.SUCCESS;

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
