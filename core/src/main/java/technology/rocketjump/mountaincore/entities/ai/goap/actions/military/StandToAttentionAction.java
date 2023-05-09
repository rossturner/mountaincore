package technology.rocketjump.mountaincore.entities.ai.goap.actions.military;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.mountaincore.entities.ai.combat.CreatureCombat;
import technology.rocketjump.mountaincore.entities.ai.goap.AssignedGoal;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.Action;
import technology.rocketjump.mountaincore.entities.ai.goap.actions.FaceTowardsLocationAction;
import technology.rocketjump.mountaincore.entities.ai.memory.Memory;
import technology.rocketjump.mountaincore.entities.ai.memory.MemoryType;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CombatBehaviour;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.mountaincore.entities.components.Faction;
import technology.rocketjump.mountaincore.entities.components.FactionComponent;
import technology.rocketjump.mountaincore.entities.components.creature.MemoryComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.tile.CompassDirection;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.mountaincore.misc.VectorUtils.toGridPoint;

public class StandToAttentionAction extends Action {

	private static final double MIN_HOURS_TO_IDLE = 0.1;
	private static final double MAX_HOURS_TO_IDLE = 0.3;
	private static final float TIME_BETWEEN_CHECKS_FOR_OPPONENT = 0.7f;

	public StandToAttentionAction(AssignedGoal parent) {
		super(parent);
	}

	private boolean initialised;
	private double elapsedTime;
	private double maxTime;
	private float nextCheckForOpponents = TIME_BETWEEN_CHECKS_FOR_OPPONENT;

	@Override
	public void update(float deltaTime, GameContext gameContext) {
		if (!initialised) {
			FaceTowardsLocationAction faceTowardsLocationAction = null;

			if (gameContext.getRandom().nextBoolean()) {
				// 50/50 face a new direction
				CompassDirection randomDirection = CompassDirection.values()[gameContext.getRandom().nextInt(CompassDirection.values().length)];
				Vector2 facingTarget = parent.parentEntity.getLocationComponent().getWorldOrParentPosition().cpy().add(randomDirection.toVector());
				faceTowardsLocationAction = new FaceTowardsLocationAction(parent); // push to head of queue
				parent.setTargetLocation(facingTarget);
			}
			if (faceTowardsLocationAction != null) {
				parent.actionQueue.push(faceTowardsLocationAction);
			}
			maxTime = gameContext.getGameClock().gameHoursToRealTimeSeconds(MIN_HOURS_TO_IDLE +
					(gameContext.getRandom().nextFloat() * (MAX_HOURS_TO_IDLE - MIN_HOURS_TO_IDLE)));

			initialised = true;
		}

		// Check to see if an enemy is nearby to attack
		nextCheckForOpponents -= deltaTime;
		if (nextCheckForOpponents < 0) {
			nextCheckForOpponents = TIME_BETWEEN_CHECKS_FOR_OPPONENT;
			checkForOpponents(gameContext);
		}

		elapsedTime += deltaTime;
		if (elapsedTime > maxTime) {
			completionType = CompletionType.SUCCESS;
		}
	}

	private void checkForOpponents(GameContext gameContext) {
		CreatureCombat combat = new CreatureCombat(parent.parentEntity);
		CreatureBehaviour parentBehaviour = (CreatureBehaviour) parent.parentEntity.getBehaviourComponent();
		Faction parentFaction = parent.parentEntity.getOrCreateComponent(FactionComponent.class).getFaction();

		int range = combat.getEquippedWeapon().getRange();
		GridPoint2 parentPosition = toGridPoint(parent.parentEntity.getLocationComponent().getWorldOrParentPosition());
		if (parentPosition == null) {
			return;
		}

		for (CompassDirection direction : CompassDirection.values()) {
			for (int distance = 1; distance <= range; distance++) {
				MapTile targetTile = gameContext.getAreaMap().getTile(parentPosition.x + (direction.getXOffset() * distance),
						parentPosition.y + (direction.getYOffset() * distance));
				if (targetTile == null || targetTile.hasWall()) {
					break;
				}

				for (Entity entity : targetTile.getEntities()) {
					Faction entityFaction = entity.getOrCreateComponent(FactionComponent.class).getFaction();
					if (entity.getType().equals(EntityType.CREATURE) && parentBehaviour.hostileFactions(parentFaction, entityFaction)) {
						// Found an enemy to attack

						attackCreature(entity, gameContext);
						// Not really failed but want to stop running this action and switch to combat
						completionType = CompletionType.FAILURE;
						return;
					}
				}
			}
		}
	}

	private void attackCreature(Entity entity, GameContext gameContext) {
		MemoryComponent memoryComponent = parent.parentEntity.getOrCreateComponent(MemoryComponent.class);
		Memory memory = new Memory(MemoryType.ABOUT_TO_ATTACK_CREATURE, gameContext.getGameClock());
		memory.setRelatedEntityId(entity.getId());
		memoryComponent.addShortTerm(memory, gameContext.getGameClock());

		// Force attack of opportunity
		CombatBehaviour combatBehaviour = ((CreatureBehaviour) parent.parentEntity.getBehaviourComponent()).getCombatBehaviour();
		combatBehaviour.makeAttackOfOpportunity(entity);
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (initialised) {
			asJson.put("initialised", true);
		}
		if (elapsedTime > 0) {
			asJson.put("elapsedTime", elapsedTime);
		}
		if (maxTime > 0) {
			asJson.put("maxTime", maxTime);
		}
		asJson.put("nextCheckForOpponents", nextCheckForOpponents);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.initialised = asJson.getBooleanValue("initialised");
		this.elapsedTime = asJson.getDoubleValue("elapsedTime");
		this.maxTime = asJson.getDoubleValue("maxTime");
		this.nextCheckForOpponents = asJson.getFloatValue("nextCheckForOpponents");
	}
}
