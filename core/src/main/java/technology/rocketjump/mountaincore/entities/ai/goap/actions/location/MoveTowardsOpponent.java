package technology.rocketjump.mountaincore.entities.ai.goap.actions.location;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.entities.ai.combat.CreatureCombat;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.components.creature.CombatStateComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import java.util.List;

import static technology.rocketjump.mountaincore.entities.ai.combat.FleeFromCombatAction.MIN_SEPARATION_FROM_OPPONENTS_TO_LEAVE_COMBAT;
import static technology.rocketjump.mountaincore.misc.VectorUtils.getGridpointsBetween;
import static technology.rocketjump.mountaincore.misc.VectorUtils.toVector;

public class MoveTowardsOpponent extends GoToLocationAction {

	private static final float MAX_TIME_TO_FOLLOW_SAME_PATH = 3.5f;
	private float timeFollowingPath;

	public MoveTowardsOpponent(AssignedGoal parent) {
		super(parent);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		super.update(deltaTime, gameContext);

		if (path != null) {
			if (timeFollowingPath > MAX_TIME_TO_FOLLOW_SAME_PATH) {
				// Reset pathing state to path closer to target
				pathfindingRequested = false;
				path = null;
			} else {
				timeFollowingPath += deltaTime;
			}
		}
	}

	@Override
	protected Vector2 selectDestination(GameContext gameContext) {
		Long targetId = getTargetId();
		if (targetId == null) {
			Logger.error("Was expecting a target for " + this.getSimpleName());
			return null;
		}

		Entity targetEntity = gameContext.getEntities().get(targetId);
		if (targetEntity != null) {
			if (targetEntity.getType().equals(EntityType.FURNITURE)) {
				return neatestTileToFurniture(targetEntity, parent.parentEntity, gameContext);
			} else {
				return targetEntity.getLocationComponent().getWorldOrParentPosition();
			}
		} else {
			return null;
		}
	}

	public static Vector2 neatestTileToFurniture(Entity targetFurniture, Entity parentEntity, GameContext gameContext) {
		List<GridPoint2> gridpointsBetween = getGridpointsBetween(targetFurniture.getLocationComponent().getWorldPosition(), parentEntity.getLocationComponent().getWorldOrParentPosition());
		for (GridPoint2 gridPoint : gridpointsBetween) {
			MapTile tile = gameContext.getAreaMap().getTile(gridPoint);

			if (tile.getEntities().stream().anyMatch(e -> e.equals(targetFurniture))) {
				continue;
			}

			// Else this should be the nearest tile to the furniture
			if (!tile.isNavigable(parentEntity)) {
				return null;
			}

			return toVector(gridPoint);
		}
		return null;
	}

	private Long getTargetId() {
		CombatStateComponent combatStateComponent = parent.parentEntity.getComponent(CombatStateComponent.class);
		return combatStateComponent.getTargetedOpponentId();
	}

	@Override
	protected void checkForCompletion(GameContext gameContext) {
		super.checkForCompletion(gameContext);

		if (completionType == null) {
			Entity targetEntity = gameContext.getEntities().get(getTargetId());
			if (targetEntity != null) {

				CreatureCombat creatureCombat = new CreatureCombat(parent.parentEntity);
				float minimumRequiredDistance = creatureCombat.getEquippedWeapon().getRange();
				if (minimumRequiredDistance <= 2) {
					// Melee weapon, so go to half escape distance
					minimumRequiredDistance = MIN_SEPARATION_FROM_OPPONENTS_TO_LEAVE_COMBAT / 2f;
				} else {
					// Ranged, so add 1 to distance
					minimumRequiredDistance += 1f;
				}

				float distanceToTarget = parent.parentEntity.getLocationComponent().getWorldOrParentPosition().dst(
						targetEntity.getLocationComponent().getWorldOrParentPosition()
				);

				if (distanceToTarget <= minimumRequiredDistance && hasLineOfSightBetween(parent.parentEntity, targetEntity, gameContext)) {
					completionType = CompletionType.SUCCESS;
				}
			}
		}
	}

	public static boolean hasLineOfSightBetween(Entity parentEntity, Entity targetEntity, GameContext gameContext) {
		List<GridPoint2> locationsToCheck = getGridpointsBetween(
				parentEntity.getLocationComponent().getWorldOrParentPosition(),
				targetEntity.getLocationComponent().getWorldOrParentPosition()
		);
		return locationsToCheck.stream().noneMatch(p -> {
			MapTile tile = gameContext.getAreaMap().getTile(p);
			return tile == null || tile.hasWall();
		});
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);

		asJson.put("timeFollowingPath", timeFollowingPath);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		this.timeFollowingPath = asJson.getFloatValue("timeFollowingPath");
	}
}
