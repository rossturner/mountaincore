package technology.rocketjump.saul.military;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.entities.ai.goap.AssignedGoal;
import technology.rocketjump.saul.entities.ai.goap.actions.military.AttackOpponentAction;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.saul.entities.components.creature.CombatStateComponent;
import technology.rocketjump.saul.entities.components.creature.MilitaryComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.SquadOrderChangeMessage;
import technology.rocketjump.saul.military.model.Squad;
import technology.rocketjump.saul.military.model.SquadOrderType;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;

@Singleton
public class MilitaryMessageHandler implements Telegraph, GameContextAware {

	public static final int MAX_SQUAD_COUNT = 6;
	private final MessageDispatcher messageDispatcher;
	private final I18nTranslator i18nTranslator;
	private final SquadFormationDictionary squadFormationDictionary;
	private GameContext gameContext;

	@Inject
	public MilitaryMessageHandler(MessageDispatcher messageDispatcher, I18nTranslator i18nTranslator, SquadFormationDictionary squadFormationDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.i18nTranslator = i18nTranslator;
		this.squadFormationDictionary = squadFormationDictionary;

		messageDispatcher.addListener(this, MessageType.MILITARY_ASSIGNMENT_CHANGED);
		messageDispatcher.addListener(this, MessageType.MILITARY_SQUAD_SHIFT_CHANGED);
		messageDispatcher.addListener(this, MessageType.MILITARY_SQUAD_ORDERS_CHANGED);
		messageDispatcher.addListener(this, MessageType.MILITARY_CREATE_SQUAD);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.MILITARY_ASSIGNMENT_CHANGED -> {
				Entity entity = (Entity) msg.extraInfo;
				MilitaryComponent militaryComponent = entity.getComponent(MilitaryComponent.class);
				Long squadId = militaryComponent.getSquadId();

				if (squadId != null && !gameContext.getSquads().containsKey(squadId)) {
					createSquad(squadId);
				}

				for (Squad squad : gameContext.getSquads().values()) {
					if (squadId != null && squad.getId() == squadId) {
						squad.getMemberEntityIds().add(entity.getId());
					} else {
						squad.getMemberEntityIds().remove(entity.getId());
						// TODO need to do anything if squad is now empty?
					}
				}

				if (entity.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour) {
					creatureBehaviour.militaryAssignmentChanged();
				}

				messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, entity);
				return true;
			}
			case MessageType.MILITARY_SQUAD_SHIFT_CHANGED -> {
				Squad squad = (Squad) msg.extraInfo;

				interruptCurrentBehaviour(squad);

				return true;
			}
			case MessageType.MILITARY_SQUAD_ORDERS_CHANGED -> {
				SquadOrderChangeMessage message = (SquadOrderChangeMessage) msg.extraInfo;

				if (message.newOrderType.equals(SquadOrderType.RETREATING)) {
					retreatFromCombat(message.squad);
					message.squad.setCurrentOrderType(SquadOrderType.TRAINING);
				} else {
					message.squad.setCurrentOrderType(message.newOrderType);
				}

				if (!message.newOrderType.equals(SquadOrderType.COMBAT)) {
					message.squad.getAttackEntityIds().clear();
				}


				if (message.squad.isOnDuty(gameContext.getGameClock())) {
					interruptCurrentBehaviour(message.squad);
				}

				return true;
			}
			case MessageType.MILITARY_CREATE_SQUAD -> {
				Squad newSquad = (Squad) msg.extraInfo;
				createSquad(newSquad);
				return true;
			}
			default ->
					throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.getClass().getSimpleName() + ", " + msg);
		}
	}

	private void retreatFromCombat(Squad squad) {
		for (Long memberEntityId : squad.getMemberEntityIds()) {
			Entity squadMember = gameContext.getEntities().get(memberEntityId);
			if (squadMember != null) {
				CombatStateComponent combatStateComponent = squadMember.getComponent(CombatStateComponent.class);
				if (combatStateComponent.isInCombat()) {
					combatStateComponent.setForceRetreat(true);
				} else if (squadMember.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour &&
						creatureBehaviour.getCurrentGoal() != null &&
						hasPendingAttackAction(creatureBehaviour.getCurrentGoal())) {
					creatureBehaviour.getCurrentGoal().setInterrupted(true);
				}
			}
		}

	}

	private boolean hasPendingAttackAction(AssignedGoal goal) {
		return goal.actionQueue.stream()
				.anyMatch(a -> a.getClass().equals(AttackOpponentAction.class));
	}

	private void interruptCurrentBehaviour(Squad squad) {
		for (Long memberEntityId : squad.getMemberEntityIds()) {
			Entity squadMember = gameContext.getEntities().get(memberEntityId);
			if (squadMember.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour) {
				if (creatureBehaviour.getCurrentGoal() != null) {
					creatureBehaviour.getCurrentGoal().setInterrupted(true);
				}
			}
		}
	}

	//TODO: consider erroring/custom handler if someone edits the save to have more than 6 squads
	private void createSquad(long squadId) {
		Squad squad = new Squad();
		squad.setId(squadId);
		squad.setName(i18nTranslator.getTranslatedString("MILITARY.SQUAD.DEFAULT_NAME") + " #" + squadId);
		createSquad(squadId);
	}

	private void createSquad(Squad squad) {
		squad.setFormation(squadFormationDictionary.getAll().iterator().next());
		gameContext.getSquads().put(squad.getId(), squad);
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
