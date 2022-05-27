package technology.rocketjump.saul.entities.components.humanoid;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.entities.components.EntityComponent;
import technology.rocketjump.saul.entities.model.physical.creature.DeathReason;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.EnumParser;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

/**
 * Currently just stores death reason which may want to move to a more settlement-wide historical records system
 */
public class HistoryComponent implements EntityComponent {

	private DeathReason deathReason;

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

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (deathReason != null) {
			asJson.put("death", deathReason.name());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		deathReason = EnumParser.getEnumValue(asJson, "death", DeathReason.class, null);
	}
}
