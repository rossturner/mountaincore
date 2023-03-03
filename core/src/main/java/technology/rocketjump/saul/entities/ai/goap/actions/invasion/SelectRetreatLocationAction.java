package technology.rocketjump.saul.entities.ai.goap.actions.invasion;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.saul.entities.ai.goap.actions.Action;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureGroup;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.saul.invasions.InvasionMessageHandler.getNavigableMapEdgeTiles;
import static technology.rocketjump.saul.misc.VectorUtils.toVector;

public class SelectRetreatLocationAction extends Action {

	public SelectRetreatLocationAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		if (parent.parentEntity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour &&
			creatureBehaviour.getCreatureGroup() != null) {
			CreatureGroup group = creatureBehaviour.getCreatureGroup();

			if (isAdjacentToMapEdge(group.getHomeLocation(), gameContext)) {
				parent.setTargetLocation(toVector(group.getHomeLocation()));
				completionType = CompletionType.SUCCESS;
			} else {
				// Set home location to nearest navigable map edge
				MapTile parentEntityTile = gameContext.getAreaMap().getTile(parent.parentEntity.getLocationComponent().getWorldOrParentPosition());
				MapTile nearestTileToMapEdge = null;
				float distance2ToNearest = Float.MAX_VALUE;

				for (MapTile edgeTile : getNavigableMapEdgeTiles(parentEntityTile.getRegionId(), gameContext)) {
					float distanceToTile = edgeTile.getTilePosition().dst2(parentEntityTile.getTilePosition());
					if (distanceToTile < distance2ToNearest) {
						nearestTileToMapEdge = edgeTile;
						distance2ToNearest = distanceToTile;
					}
				}

				if (nearestTileToMapEdge == null) {
					completionType = CompletionType.FAILURE;
				} else {
					creatureBehaviour.getCreatureGroup().setHomeLocation(nearestTileToMapEdge.getTilePosition());
					parent.setTargetLocation(toVector(group.getHomeLocation()));
					completionType = CompletionType.SUCCESS;
				}
			}
		} else {
			// Not yet implemented
			Logger.error("Not yet implemented: " + getSimpleName() + " for creature not part of CreatureGroup");
			completionType = CompletionType.FAILURE;
		}
	}

	private boolean isAdjacentToMapEdge(GridPoint2 location, GameContext gameContext) {
		return location.x == 0 || location.x == gameContext.getAreaMap().getWidth() - 1 ||
				location.y == 0 || location.y == gameContext.getAreaMap().getHeight() - 1;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
	}
}
