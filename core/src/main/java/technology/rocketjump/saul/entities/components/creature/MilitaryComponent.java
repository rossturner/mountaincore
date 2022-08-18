package technology.rocketjump.saul.entities.components.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.apache.commons.lang3.NotImplementedException;
import technology.rocketjump.saul.entities.components.EntityComponent;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.components.ParentDependentEntityComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.misc.Destructible;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

public class MilitaryComponent implements ParentDependentEntityComponent, Destructible {

	private Long assignedToSquadId; // null means not in military

	private Long assignedWeaponId;
	private Long assignedShieldId;
	private Long assignedArmorId;

	private transient Entity parentEntity;
	private transient MessageDispatcher messageDispatcher;
	private transient GameContext gameContext;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.parentEntity = parentEntity;
		this.messageDispatcher = messageDispatcher;
		this.gameContext = gameContext;
	}

	@Override
	public void destroy(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		if (assignedToSquadId != null) {
			removeFromMilitary();
		}
	}

	public void addToMilitary(long squadId) {
		this.assignedToSquadId = squadId;
	}

	public void removeFromMilitary() {
		this.assignedToSquadId = null;


		InventoryComponent inventoryComponent = parentEntity.getOrCreateComponent(InventoryComponent.class);
	}

	public boolean isInMilitary() {
		return assignedToSquadId != null;
	}


	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		throw new NotImplementedException("Creatures are not cloned");
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		throw new NotImplementedException("Implement this");
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		throw new NotImplementedException("Implement this");
	}
}
