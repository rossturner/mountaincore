package technology.rocketjump.saul.entities.ai.combat;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import org.apache.commons.lang3.NotImplementedException;
import technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.Goal;
import technology.rocketjump.saul.entities.ai.goap.actions.location.GoToLocationAction;
import technology.rocketjump.saul.entities.components.creature.CombatStateComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static technology.rocketjump.saul.misc.VectorUtils.toVector;

public class FleeFromCombatAction extends CombatAction {

	private static final float LARGE_DISTANCE_AWAY_FROM_OPPONENTS = 40f;
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
				goToLocationAction.setOverrideLocation(toVector(destinationTile));
			}
		}

		if (goToLocationAction != null) {
			CombatStateComponent combatStateComponent = parentEntity.getComponent(CombatStateComponent.class);
			if (combatStateComponent.isEngagedInMelee()) {
				messageDispatcher.dispatchMessage(MessageType.TRIGGER_ATTACK_OF_OPPORTUNITY, parentEntity);
				combatStateComponent.setEngagedInMelee(false);
			}

			goToLocationAction.update(deltaTime, gameContext);
		}

		// complete when X distance away from any opponent
	}

	private GridPoint2 pickFleeDestination(GameContext gameContext) {
		CombatStateComponent combatStateComponent = parentEntity.getComponent(CombatStateComponent.class);
		if (combatStateComponent.getOpponentEntityIds().isEmpty()) {
			return null;
		} else {
			Vector2 directionOfOpponents = new Vector2();
			Vector2 parentPosition = parentEntity.getLocationComponent().getWorldOrParentPosition();
			for (Long opponentEntityId : combatStateComponent.getOpponentEntityIds()) {
				Entity opponentEntity = gameContext.getEntities().get(opponentEntityId);
				if (opponentEntity != null) {
					Vector2 parentToOpponent = opponentEntity.getLocationComponent().getWorldOrParentPosition().cpy().sub(parentPosition);
					directionOfOpponents.add(parentToOpponent);
				}
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
			);
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

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		throw new NotImplementedException("");
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {

	}
}
