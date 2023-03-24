package technology.rocketjump.mountaincore.entities.ai.goap.actions.invasion;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.SwitchGoalException;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureGroup;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.misc.VectorUtils;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import java.util.concurrent.atomic.AtomicInteger;

import static technology.rocketjump.mountaincore.entities.ai.goap.actions.Action.CompletionType.FAILURE;
import static technology.rocketjump.mountaincore.entities.ai.goap.actions.Action.CompletionType.SUCCESS;

public class MoveGroupHomeTowardSettlersAction extends Action {

	private static final int RANGE = 8;

	public MoveGroupHomeTowardSettlersAction(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) throws SwitchGoalException {
		if (parent.parentEntity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour &&
			creatureBehaviour.getCreatureGroup() != null) {
			CreatureGroup creatureGroup = creatureBehaviour.getCreatureGroup();
			MapTile currentHomeTile = gameContext.getAreaMap().getTile(creatureGroup.getHomeLocation());
			if (currentHomeTile == null) {
				completionType = FAILURE;
				return;
			}

			Vector2 homeAsVector = VectorUtils.toVector(creatureGroup.getHomeLocation());
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

			GridPoint2 targetArea = VectorUtils.toGridPoint(homeAsVector.add(towardsSettlers));
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
				parent.setTargetLocation(VectorUtils.toVector(targetTile.getTilePosition()));
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
