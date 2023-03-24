package technology.rocketjump.mountaincore.entities.ai.combat;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.Goal;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.location.GoToLocationAction;
import technology.rocketjump.mountaincore.entities.components.creature.CombatStateComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.misc.VectorUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FleeFromCombatAction extends CombatAction {

	public static final float LARGE_DISTANCE_AWAY_FROM_OPPONENTS = 30f;
	public static final float MIN_SEPARATION_FROM_OPPONENTS_TO_LEAVE_COMBAT = 18f;

	private GoToLocationAction goToLocationAction;

	public FleeFromCombatAction(Entity parentEntity) {
		super(parentEntity);
	}

	@Override
	public void update(float deltaTime, GameContext gameContext, MessageDispatcher messageDispatcher) {
		if (goToLocationAction == null) {
			GridPoint2 destinationTile = pickFleeDestination(gameContext);
			if (destinationTile != null) {
				goToLocationAction = new GoToLocationAction(new AssignedGoal(Goal.NULL_GOAL, parentEntity, messageDispatcher));
				goToLocationAction.setOverrideLocation(VectorUtils.toVector(destinationTile));
			}
		}

		if (goToLocationAction != null) {
			CombatStateComponent combatStateComponent = parentEntity.getComponent(CombatStateComponent.class);
			if (combatStateComponent.isEngagedInMelee()) {
				messageDispatcher.dispatchMessage(MessageType.TRIGGER_ATTACK_OF_OPPORTUNITY, parentEntity);
				combatStateComponent.setEngagedInMelee(false);
			}

			goToLocationAction.update(deltaTime, gameContext);


			Action.CompletionType completion = MoveInRangeOfTargetCombatAction.isCompleted(goToLocationAction, gameContext);
			if (completion != null) {
				goToLocationAction = null;
			}
		}



		// complete when X distance away from any opponent
		List<Entity> opponentEntities = getOpponents(gameContext);
		if (opponentEntities.isEmpty()) {
			completed = true;
		} else {
			Vector2 parentPosition = parentEntity.getLocationComponent().getWorldOrParentPosition();
			float dist2ToNearestOpponent = LARGE_DISTANCE_AWAY_FROM_OPPONENTS * LARGE_DISTANCE_AWAY_FROM_OPPONENTS;
			for (Entity opponentEntity : opponentEntities) {
				float distance2ToOpponent = opponentEntity.getLocationComponent().getWorldOrParentPosition().dst2(parentPosition);
				if (distance2ToOpponent < dist2ToNearestOpponent) {
					dist2ToNearestOpponent = distance2ToOpponent;
				}
			}

			if (dist2ToNearestOpponent > MIN_SEPARATION_FROM_OPPONENTS_TO_LEAVE_COMBAT * MIN_SEPARATION_FROM_OPPONENTS_TO_LEAVE_COMBAT) {
				completed = true;
			}
		}
	}

	private GridPoint2 pickFleeDestination(GameContext gameContext) {
		CombatStateComponent combatStateComponent = parentEntity.getComponent(CombatStateComponent.class);
		if (combatStateComponent.getOpponentEntityIds().isEmpty()) {
			return null;
		} else {
			Vector2 directionOfOpponents = new Vector2();
			Vector2 parentPosition = parentEntity.getLocationComponent().getWorldOrParentPosition();
			for (Entity opponentEntity : getOpponents(gameContext)) {
				Vector2 parentToOpponent = opponentEntity.getLocationComponent().getWorldOrParentPosition().cpy().sub(parentPosition);
				directionOfOpponents.add(parentToOpponent);
			}

			GridPoint2 destination = pickDestinationInDirection(directionOfOpponents.nor().scl(-1f), gameContext);

			if (destination != null) {
				return destination;
			} else {
				List<EntityAssetOrientation> randomisedDirections = Arrays.asList(EntityAssetOrientation.values());
				Collections.shuffle(randomisedDirections);
				for (EntityAssetOrientation orientation : randomisedDirections) {
					destination = pickDestinationInDirection(orientation.toVector2(), gameContext);
					if (destination != null) {
						return destination;
					}
				}
			}
		}
		return null;
	}

	private List<Entity> getOpponents(GameContext gameContext) {
		List<Entity> result = new ArrayList<>();
		for (Long opponentEntityId : parentEntity.getComponent(CombatStateComponent.class).getOpponentEntityIds()) {
			Entity opponent = gameContext.getEntities().get(opponentEntityId);
			if (opponent != null) {
				result.add(opponent);
			}
		}
		return result;
	}

	private GridPoint2 pickDestinationInDirection(Vector2 directionToFlee, GameContext gameContext) {
		int parentRegion = -1;
		MapTile parentTile = gameContext.getAreaMap().getTile(parentEntity.getLocationComponent().getWorldOrParentPosition());
		if (parentTile != null) {
			parentRegion = parentTile.getRegionId();
		}

		for (int attempt = 0; attempt < 5; attempt++) {
			// Randomly picks a place in +/-5 tiles in direction
			Vector2 target = directionToFlee.cpy().scl(LARGE_DISTANCE_AWAY_FROM_OPPONENTS).add(
					-5f + gameContext.getRandom().nextFloat(10f), -5f + gameContext.getRandom().nextFloat(10f)
			).add(parentTile.getTileX(), parentTile.getTileY());
			MapTile targetTile = gameContext.getAreaMap().getTile(target);
			if (targetTile != null && targetTile.isNavigable(parentEntity) && targetTile.getRegionId() == parentRegion) {
				return targetTile.getTilePosition();
			}
		}
		return null;
	}

	@Override
	public void interrupted(MessageDispatcher messageDispatcher) {

	}

	@Override
	public boolean completesInOneRound() {
		return false;
	}

	// don't do persistence, just create a new GoToLocationAction

}
