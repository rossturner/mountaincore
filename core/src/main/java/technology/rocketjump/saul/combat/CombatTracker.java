package technology.rocketjump.saul.combat;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.entities.ai.combat.AttackCreatureCombatAction;
import technology.rocketjump.saul.entities.ai.combat.CombatAction;
import technology.rocketjump.saul.entities.ai.combat.CreatureCombat;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.saul.entities.components.Faction;
import technology.rocketjump.saul.entities.components.FactionComponent;
import technology.rocketjump.saul.entities.components.creature.CombatStateComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.Updatable;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.ScreenWriter;
import technology.rocketjump.saul.settlement.CreatureTracker;

import java.util.*;

import static technology.rocketjump.saul.entities.components.Faction.HOSTILE_INVASION;
import static technology.rocketjump.saul.entities.components.Faction.MERCHANTS;
import static technology.rocketjump.saul.ui.i18n.I18nTranslator.oneDecimalFormat;

@Singleton
public class CombatTracker implements Updatable, Telegraph {

	private static final float COMBAT_ROUND_DURATION = 3.5f;
	private static final float COMBAT_ROUND_INITIAL_DELAY = 0.25f;
	private static final float COMBAT_ROUND_CLOSING_DELAY = COMBAT_ROUND_DURATION * 0.3f;
	private static final float MAX_TIME_BETWEEN_ATTACKS = 0.4f;

	private final CreatureTracker creatureTracker;
	private final MessageDispatcher messageDispatcher;
	private final ScreenWriter screenWriter;
	private GameContext gameContext;

	private final Map<Long, Entity> entitiesInCombatById = new HashMap<>();
	private final List<CombatAction> actionsToResolveThisRound = new ArrayList<>();

	@Inject
	public CombatTracker(CreatureTracker creatureTracker, MessageDispatcher messageDispatcher, ScreenWriter screenWriter) {
		this.creatureTracker = creatureTracker;
		this.messageDispatcher = messageDispatcher;
		this.screenWriter = screenWriter;

		messageDispatcher.addListener(this, MessageType.CREATURE_ENTERING_COMBAT);
		messageDispatcher.addListener(this, MessageType.CREATURE_EXITING_COMBAT);
	}

	@Override
	public void update(float deltaTime) {
		float currentElapsedTime = gameContext.getSettlementState().getCurrentCombatRoundElapsed();
		currentElapsedTime += deltaTime;
		if (currentElapsedTime >= COMBAT_ROUND_DURATION && allActionsResolved()) {
			onCombatRoundStart();
			currentElapsedTime = 0f;
		}

		screenWriter.printLine("Combat round elapsed: " + oneDecimalFormat.format(currentElapsedTime));

		gameContext.getSettlementState().setCurrentCombatRoundElapsed(currentElapsedTime);
	}

	public void onCombatRoundStart() {
		// defending combatants can replenish defense pool
		for (Entity entity : entitiesInCombatById.values()) {
			if (entity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour) {
				if (creatureBehaviour.getCombatBehaviour().getCurrentAction() != null) {
					creatureBehaviour.getCombatBehaviour().getCurrentAction().onRoundCompletion();
				}
			}
		}

		actionsToResolveThisRound.clear();

		for (Entity entity : entitiesInCombatById.values()) {
			if (entity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour) {
				CombatAction combatAction = creatureBehaviour.getCombatBehaviour().selectNewActionForRound(creatureBehaviour.isStunned());
				if (combatAction.completesInOneRound()) {
					actionsToResolveThisRound.add(combatAction);
				}
			}
		}
		for (Entity entity : entitiesInCombatById.values()) {
			if (entity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour) {
				CombatAction currentAction = creatureBehaviour.getCombatBehaviour().getCurrentAction();
				CombatAction newAction = creatureBehaviour.getCombatBehaviour().changeCombatActionAtStartOfRound();
				if (newAction != null) {
					actionsToResolveThisRound.remove(currentAction);
					if (newAction.completesInOneRound()) {
						actionsToResolveThisRound.add(newAction);
					}
				}
			}
		}


		// This class organises when in the round an attack will take place
		List<AttackCreatureCombatAction> attackActions = new ArrayList<>();
		for (CombatAction combatAction : actionsToResolveThisRound) {
			if (combatAction instanceof AttackCreatureCombatAction attack) {
				attackActions.add(attack);
			}
		}
		int numAttacksThisRound = attackActions.size();
		if (numAttacksThisRound > 0) {
			float timeToSplitAttacksOver = COMBAT_ROUND_DURATION - COMBAT_ROUND_INITIAL_DELAY - COMBAT_ROUND_CLOSING_DELAY;
			float timeBetweenAttacks = timeToSplitAttacksOver / numAttacksThisRound;
			timeBetweenAttacks = Math.min(timeBetweenAttacks, MAX_TIME_BETWEEN_ATTACKS);

			float cursor = COMBAT_ROUND_INITIAL_DELAY;
			Collections.shuffle(attackActions, gameContext.getRandom());
			for (AttackCreatureCombatAction attackAction : attackActions) {
				attackAction.setTimeUntilAttack(cursor);
				cursor += timeBetweenAttacks;
			}
		}


	}

	public void add(Entity entity) {
		entitiesInCombatById.put(entity.getId(), entity);
		CreatureCombat combatStats = new CreatureCombat(entity);

		CombatStateComponent combatStateComponent = entity.getComponent(CombatStateComponent.class);
		combatStateComponent.setInCombat(true);
		combatStateComponent.setDefensePool(combatStats.maxDefensePool());

		if (combatStateComponent.getTargetedOpponentId() != null) {
			Entity targetedEntity = gameContext.getEntities().get(combatStateComponent.getTargetedOpponentId());

			// Figure out set of opponent IDs
			if (targetedEntity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour) {
				if (creatureBehaviour.getCreatureGroup() != null) {
					combatStateComponent.setOpponentEntityIds(new ArrayList<>(creatureBehaviour.getCreatureGroup().getMemberIds()));
				} else {
					Faction opponentFaction = targetedEntity.getOrCreateComponent(FactionComponent.class).getFaction();
					Faction entityFaction = entity.getOrCreateComponent(FactionComponent.class).getFaction();
					if (!opponentFaction.equals(entityFaction) && (opponentFaction.equals(HOSTILE_INVASION) || opponentFaction.equals(MERCHANTS))) {
						combatStateComponent.setOpponentEntityIds(getAllCreaturesInFaction(opponentFaction));
					} else {
						// TODO targeting monster or other settlement creature, set other opponents?
					}
				}
			}

			boolean combatantInMeleeRange = false;
			Vector2 combatantPosition = entity.getLocationComponent().getWorldOrParentPosition();
			for (Long opponentEntityId : combatStateComponent.getOpponentEntityIds()) {
				Entity opponentEntity = gameContext.getEntities().get(opponentEntityId);
				if (opponentEntity != null) {
					Vector2 opponentPosition = opponentEntity.getLocationComponent().getWorldOrParentPosition();
					if (Math.abs(combatantPosition.x - opponentPosition.x) <= 1 && Math.abs(combatantPosition.y - opponentPosition.y) <= 1) {
						combatantInMeleeRange = true;
						break;
					}
				}
			}
			combatStateComponent.setEngagedInMelee(combatantInMeleeRange);
		}
	}

	private List<Long> getAllCreaturesInFaction(Faction faction) {
		return creatureTracker.getLiving().stream()
				.filter(e -> e.getOrCreateComponent(FactionComponent.class).getFaction().equals(faction))
				.map(Entity::getId)
				.toList();
	}

	private boolean allActionsResolved() {
		return actionsToResolveThisRound.stream().allMatch(CombatAction::isCompleted);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.CREATURE_ENTERING_COMBAT: {
				creatureEnteringCombat((Entity) msg.extraInfo);
				return true;
			}
			case MessageType.CREATURE_EXITING_COMBAT: {
				return true;
			}
			default:
				throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.toString() + ", " + msg.toString());
		}
	}

	private void creatureEnteringCombat(Entity creature) {
		add(creature);

		if (creature.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour) {
			CombatAction combatAction = creatureBehaviour.getCombatBehaviour().selectNewActionForRound(creatureBehaviour.isStunned());
			combatActionAdded(combatAction);
		}

	}

	private void combatActionAdded(CombatAction combatAction) {
		if (combatAction.completesInOneRound()) {
			actionsToResolveThisRound.add(combatAction);
		}

	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {
		entitiesInCombatById.clear();
		actionsToResolveThisRound.clear();
	}

	@Override
	public boolean runWhilePaused() {
		return false;
	}
}
