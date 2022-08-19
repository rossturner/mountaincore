package technology.rocketjump.saul.military;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.entities.components.creature.MilitaryComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.military.model.Squad;
import technology.rocketjump.saul.ui.i18n.I18nTranslator;

@Singleton
public class MilitaryMessageHandler implements Telegraph, GameContextAware {

	private final MessageDispatcher messageDispatcher;
	private final I18nTranslator i18nTranslator;
	private GameContext gameContext;

	@Inject
	public MilitaryMessageHandler(MessageDispatcher messageDispatcher, I18nTranslator i18nTranslator) {
		this.messageDispatcher = messageDispatcher;
		this.i18nTranslator = i18nTranslator;

		messageDispatcher.addListener(this, MessageType.MILITARY_ASSIGNMENT_CHANGED);
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

				// TODO swap behaviour of dwarf over to military schedule - need to interrupt current goal and resolve what it is doing

				// TODO update entity assets, use clothing from armour for dwarf


				return true;
			}
			default -> throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + this.getClass().getSimpleName() + ", " + msg);
		}
	}

	private void createSquad(long squadId) {
		Squad squad = new Squad();
		squad.setId(squadId);
		squad.setName(i18nTranslator.getTranslatedString("MILITARY.SQUAD.DEFAULT_NAME") + " #" + squadId);
		gameContext.getSquads().put(squadId, squad);
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
