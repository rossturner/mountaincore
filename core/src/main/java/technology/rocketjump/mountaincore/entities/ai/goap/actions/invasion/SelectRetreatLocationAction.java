package technology.rocketjump.mountaincore.entities.ai.goap.actions.invasion;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureGroup;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.invasions.InvasionMessageHandler;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.misc.VectorUtils;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

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
				parent.setTargetLocation(VectorUtils.toVector(group.getHomeLocation()));
				completionType = CompletionType.SUCCESS;
			} else {
				// Set home location to nearest navigable map edge
				MapTile parentEntityTile = gameContext.getAreaMap().getTile(parent.parentEntity.getLocationComponent().getWorldOrParentPosition());
				MapTile nearestTileToMapEdge = null;
				float distance2ToNearest = Float.MAX_VALUE;

				for (MapTile edgeTile : InvasionMessageHandler.getNavigableMapEdgeTiles(parentEntityTile.getRegionId(), gameContext)) {
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
					parent.setTargetLocation(VectorUtils.toVector(group.getHomeLocation()));
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
