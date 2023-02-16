package technology.rocketjump.saul.entities.ai.goap.actions.invasion;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.saul.entities.ai.goap.actions.Action;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureGroup;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.concurrent.atomic.AtomicInteger;

import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.saul.entities.ai.goap.actions.Action.CompletionType.SUCCESS;
import static technology.rocketjump.saul.misc.VectorUtils.toGridPoint;
import static technology.rocketjump.saul.misc.VectorUtils.toVector;

public class MoveGroupHomeTowardSettlersAction extends Action {

	private static final int RANGE = 8;

	public MoveGroupHomeTowardSettlersAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		if (parent.parentEntity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour) {
			CreatureGroup creatureGroup = creatureBehaviour.getCreatureGroup();
			MapTile currentHomeTile = gameContext.getAreaMap().getTile(creatureGroup.getHomeLocation());
			if (currentHomeTile == null) {
				completionType = FAILURE;
				return;
			}

			Vector2 homeAsVector = toVector(creatureGroup.getHomeLocation());
			Vector2 averageSettlerLocation = new Vector2();
			AtomicInteger numSettlers = new AtomicInteger(0);
			gameContext.getEntities().values().stream()
					.filter(Entity::isSettler)
					.forEach(settler -> {
						averageSettlerLocation.add(settler.getLocationComponent().getWorldOrParentPosition());
						numSettlers.set(numSettlers.get() + 1);
					});
			averageSettlerLocation.scl(1 / (float) numSettlers.get());

			Vector2 towardsSettlers = averageSettlerLocation.cpy().sub(homeAsVector);
			towardsSettlers.scl(0.33f);

			GridPoint2 targetArea = toGridPoint(homeAsVector.add(towardsSettlers));
			MapTile targetTile = null;

			for (int attempts = 0; attempts < 20; attempts++) {
				MapTile tile = gameContext.getAreaMap().getTile(targetArea.x - RANGE + gameContext.getRandom().nextInt((RANGE * 2) + 1),
						targetArea.y - RANGE + gameContext.getRandom().nextInt((RANGE * 2) + 1));

				if (tile != null && tile.isNavigable(parent.parentEntity) && tile.getRegionId() == currentHomeTile.getRegionId() &&
						tile.isEmpty()) {
					targetTile = tile;
					break;
				}
			}

			if (targetTile == null) {
				completionType = FAILURE;
			} else {
				creatureGroup.setHomeLocation(targetTile.getTilePosition());
				parent.setTargetLocation(toVector(targetTile.getTilePosition()));
				completionType = SUCCESS;
			}

		} else {
			completionType = FAILURE;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
	}
}
