package technology.rocketjump.saul.entities.components;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.FactionChangedMessage;
import technology.rocketjump.saul.persistence.EnumParser;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

public class FactionComponent implements ParentDependentEntityComponent {

	private Faction faction = Faction.SETTLEMENT;
	private transient Entity parentEntity;
	private transient MessageDispatcher messageDispatcher;

	public FactionComponent() {

	}

	public FactionComponent(Faction startingFaction) {
		this.faction = startingFaction;
	}

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.parentEntity = parentEntity;
		this.messageDispatcher = messageDispatcher;
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		FactionComponent cloned = new FactionComponent();
		cloned.faction = this.faction;
		return cloned;
	}

	public Faction getFaction() {
		return faction;
	}

	public void setFaction(Faction faction) {
		if (this.faction != faction) {
			if (this.parentEntity == null || this.messageDispatcher == null) {
				Logger.error("Setting faction in " + getClass().getSimpleName() + " without having been initialised");
			} else {
				messageDispatcher.dispatchMessage(MessageType.ENTITY_FACTION_CHANGED, new FactionChangedMessage(parentEntity, this.faction, faction));
			}
			this.faction = faction;
		}
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		asJson.put("faction", faction.name());
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.faction = EnumParser.getEnumValue(asJson, "faction", Faction.class, Faction.SETTLEMENT);
	}
}
