package technology.rocketjump.saul.entities.components.creature;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import org.apache.commons.lang3.NotImplementedException;
import technology.rocketjump.saul.entities.components.EntityComponent;
import technology.rocketjump.saul.entities.components.ParentDependentEntityComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.persistence.JSONUtils;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.HashSet;
import java.util.Set;

public class CombatStateComponent implements ParentDependentEntityComponent {

	private Entity parentEntity;

	private boolean inCombat;
	private boolean hasInitiative;
	private boolean engagedInMelee;
	private int defensePool;

	private GridPoint2 heldLocation; // The tile the combatant is keeping control of - other combatants should not share this tile
	private Long targetedOpponentId;
	private Set<Long> opponentEntityIds = new HashSet<>();

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
		this.opponentEntityIds = new HashSet<>();
	}

	@Override
	public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
		throw new NotImplementedException(getClass().getSimpleName() + ".clone()");
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

	public Set<Long> getOpponentEntityIds() {
		return opponentEntityIds;
	}

	public void setOpponentEntityIds(Set<Long> opponentEntityIds) {
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


	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		if (inCombat) {
			asJson.put("inCombat", true);
		}
		if (hasInitiative) {
			asJson.put("hasInitiative", true);
		}
		if (engagedInMelee) {
			asJson.put("engagedInMelee", true);
		}
		if (defensePool > 0) {
			asJson.put("defensePool", defensePool);
		}
		if (heldLocation != null) {
			asJson.put("heldLocation", JSONUtils.toJSON(heldLocation));
		}
		if (targetedOpponentId != null) {
			asJson.put("targetedOpponentId", targetedOpponentId);
		}
		if (!opponentEntityIds.isEmpty()) {
			JSONArray opponentIdsJson = new JSONArray();
			opponentIdsJson.addAll(opponentEntityIds);
			asJson.put("opponentEntityIds", opponentIdsJson);
		}

	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.inCombat = asJson.getBooleanValue("inCombat");
		this.hasInitiative = asJson.getBooleanValue("hasInitiative");
		this.engagedInMelee = asJson.getBooleanValue("engagedInMelee");
		this.defensePool = asJson.getIntValue("defensePool");
		this.heldLocation = JSONUtils.gridPoint2(asJson.getJSONObject("heldLocation"));
		this.targetedOpponentId = asJson.getLong("targetedOpponentId");

		JSONArray opponentIdsJson = asJson.getJSONArray("opponentEntityIds");
		if (opponentIdsJson != null) {
			for (int cursor = 0; cursor < opponentIdsJson.size(); cursor++) {
				this.opponentEntityIds.add(opponentIdsJson.getLong(cursor));
			}
		}
	}

}
