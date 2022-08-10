package technology.rocketjump.saul.entities.components.creature;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.saul.entities.components.EntityComponent;
import technology.rocketjump.saul.entities.components.ParentDependentEntityComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.List;

public class CombatStateComponent implements ParentDependentEntityComponent {

	private Entity parentEntity;

	private boolean inCombat;
	private boolean hasInitiative;
	private boolean engagedInMelee;
	private int defensePool; // TODO fill this upon entering combat

	private GridPoint2 heldLocation; // The tile the combatant is keeping control of - other combatants should not share this tile
	private Long targetedOpponentId;
	private List<Long> opponentEntityIds = new ArrayList<>();

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		this.parentEntity = parentEntity;
	}

	public void clearState() {
		this.inCombat = false;
		this.hasInitiative = false;
		this.engagedInMelee = false;
		this.defensePool = 0;
		this.heldLocation = null;
		this.targetedOpponentId = null;
		this.opponentEntityIds = new ArrayList<>();
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		CombatStateComponent cloned = new CombatStateComponent();
		// TODO copy state over
		return cloned;
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {

	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {

	}

	public boolean isInCombat() {
		return inCombat;
	}

	public void setInCombat(boolean inCombat) {
		this.inCombat = inCombat;
	}

	public Long getTargetedOpponentId() {
		return targetedOpponentId;
	}

	public void setTargetedOpponentId(Long targetedOpponentId) {
		this.targetedOpponentId = targetedOpponentId;
	}

	public List<Long> getOpponentEntityIds() {
		return opponentEntityIds;
	}

	public void setOpponentEntityIds(List<Long> opponentEntityIds) {
		this.opponentEntityIds = opponentEntityIds;
	}

	public boolean isHasInitiative() {
		return hasInitiative;
	}

	public void setHasInitiative(boolean hasInitiative) {
		this.hasInitiative = hasInitiative;
	}

	public boolean isEngagedInMelee() {
		return engagedInMelee;
	}

	public void setEngagedInMelee(boolean engagedInMelee) {
		this.engagedInMelee = engagedInMelee;
	}

	public int getDefensePool() {
		return defensePool;
	}

	public void setDefensePool(int defensePool) {
		this.defensePool = defensePool;
	}

	public GridPoint2 getHeldLocation() {
		return heldLocation;
	}

	public void setHeldLocation(GridPoint2 heldLocation) {
		this.heldLocation = heldLocation;
	}
}
