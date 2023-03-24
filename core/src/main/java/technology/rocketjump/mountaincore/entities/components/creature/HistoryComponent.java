package technology.rocketjump.mountaincore.entities.components.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.components.EntityComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.DeathReason;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.persistence.EnumParser;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

/**
 * Currently just stores death reason which may want to move to a more settlement-wide historical records system
 */
public class HistoryComponent implements EntityComponent {

	private DeathReason deathReason;
	private Entity killedBy;

	public DeathReason getDeathReason() {
		return deathReason;
	}

	public void setDeathReason(DeathReason deathReason) {
		this.deathReason = deathReason;
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		HistoryComponent cloned = new HistoryComponent();
		cloned.deathReason = this.deathReason;
		return cloned;
	}

	public void setKilledBy(Entity killedBy) {
		this.killedBy = killedBy;
	}

	public Entity getKilledBy() {
		return killedBy;
	}

	private transient boolean attemptedToWriteKiller;

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (deathReason != null) {
			asJson.put("death", deathReason.name());
		}
		if (killedBy != null) {
			if (!attemptedToWriteKiller) {
				// There's a possibility that two entities fire a projectile at each other and kill each other
				// leading to an infinite loop on save of attempting to write the other to the file first
				// in this situation we'll break here and lose the information for one of them
				attemptedToWriteKiller = true;
				killedBy.writeTo(savedGameStateHolder);
			}
			asJson.put("killedBy", killedBy.getId());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		deathReason = EnumParser.getEnumValue(asJson, "death", DeathReason.class, null);

		Long killedByEntityId = asJson.getLong("killedBy");
		if (killedByEntityId != null) {
			this.killedBy = savedGameStateHolder.entities.get(killedByEntityId);
			// Not checking if it has loaded correctly
		}
	}

}
