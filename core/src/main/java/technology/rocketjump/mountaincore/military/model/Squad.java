package technology.rocketjump.mountaincore.military.model;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import technology.rocketjump.mountaincore.entities.ai.goap.Schedule;
import technology.rocketjump.mountaincore.entities.ai.goap.ScheduleDictionary;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.SelectableDescription;
import technology.rocketjump.mountaincore.environment.GameClock;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.military.model.formations.SquadFormation;
import technology.rocketjump.mountaincore.persistence.EnumParser;
import technology.rocketjump.mountaincore.persistence.JSONUtils;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.Persistable;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;
import technology.rocketjump.mountaincore.ui.i18n.I18nText;
import technology.rocketjump.mountaincore.ui.i18n.I18nTranslator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static technology.rocketjump.mountaincore.entities.ai.goap.ScheduleCategory.MILITARY_DUTY;

/**
 * A grouping of military dwarves into a single unit to give orders to
 */
public class Squad implements Persistable, SelectableDescription {

	private long id; // Think this will just be 1 to 6 for labelling as such in the UI
	private final List<Long> memberEntityIds = new ArrayList<>();

	private String name;
	private MilitaryShift shift = MilitaryShift.DAYTIME;
	private SquadFormation formation;

	private SquadOrderType currentOrderType = SquadOrderType.TRAINING;
	private GridPoint2 guardingLocation;
	private Set<Long> attackEntityIds = new HashSet<>();
	private String emblemName;

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Override
	public List<I18nText> getDescription(I18nTranslator i18nTranslator, GameContext gameContext, MessageDispatcher messageDispatcher) {
		I18nText orderDescription;
		if (isOnDuty(gameContext.getGameClock())) {
			orderDescription = switch (currentOrderType) {
				case GUARDING -> i18nTranslator.getTranslatedString("MILITARY.SQUAD.ORDER_DESCRIPTION.GUARDING");
				case COMBAT -> i18nTranslator.getTranslatedString("MILITARY.SQUAD.ORDER_DESCRIPTION.COMBAT");
				case TRAINING -> i18nTranslator.getTranslatedString("MILITARY.SQUAD.ORDER_DESCRIPTION.TRAINING");
				case RETREATING -> i18nTranslator.getTranslatedString("MILITARY.SQUAD.ORDER_DESCRIPTION.RETREATING");
			};
		} else {
			orderDescription = i18nTranslator.getTranslatedString("MILITARY.SQUAD.ORDER_DESCRIPTION.OFF_DUTY");
		}
		return List.of(orderDescription);
	}

	public boolean isOnDuty(GameClock gameClock) {
		Schedule schedule = ScheduleDictionary.getScheduleForSquadShift(shift);
		return schedule.getCurrentApplicableCategories(gameClock).contains(MILITARY_DUTY);
	}

	public List<Long> getMemberEntityIds() {
		return memberEntityIds;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public MilitaryShift getShift() {
		return shift;
	}

	public void setShift(MilitaryShift shift) {
		this.shift = shift;
	}

	public SquadFormation getFormation() {
		return formation;
	}

	public void setFormation(SquadFormation formation) {
		this.formation = formation;
	}

	public SquadOrderType getCurrentOrderType() {
		return currentOrderType;
	}

	public void setCurrentOrderType(SquadOrderType currentOrderType) {
		this.currentOrderType = currentOrderType;
	}

	public GridPoint2 getGuardingLocation() {
		return guardingLocation;
	}

	public void setGuardingLocation(GridPoint2 guardingLocation) {
		this.guardingLocation = guardingLocation;
	}

	public Set<Long> getAttackEntityIds() {
		return attackEntityIds;
	}

	public int getMemberIndex(long id) {
		return Math.max(0, memberEntityIds.indexOf(id));
	}

	public String getEmblemName() {
		return emblemName;
	}

	public void setEmblemName(String emblemName) {
		this.emblemName = emblemName;
	}

	public String getSmallEmblemName() {
		if (getEmblemName() != null) {
			return getEmblemName() + "_small";
		}
		return null;
	}

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		if (savedGameStateHolder.squads.containsKey(id)) {
			return;
		}

		JSONObject asJson = new JSONObject(true);
		asJson.put("id", id);

		JSONArray memberJson = new JSONArray();
		memberJson.addAll(memberEntityIds);
		asJson.put("members", memberJson);

		asJson.put("name", name);
		if (!shift.equals(MilitaryShift.DAYTIME)) {
			asJson.put("shift", shift.name());
		}
		asJson.put("formation", formation.getFormationName());
		if (!currentOrderType.equals(SquadOrderType.TRAINING)) {
			asJson.put("orderType", currentOrderType.name());
		}

		if (guardingLocation != null) {
			asJson.put("guardingLocation", JSONUtils.toJSON(guardingLocation));
		}
		if (!attackEntityIds.isEmpty()) {
			JSONArray attackEntityJson = new JSONArray();
			attackEntityJson.addAll(attackEntityIds);
			asJson.put("attackEntityIds", attackEntityJson);
		}

		if (emblemName != null) {
			asJson.put("emblemName", emblemName);
		}

		savedGameStateHolder.squadsJson.add(asJson);
		savedGameStateHolder.squads.put(id, this);
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.id = asJson.getLong("id");

		JSONArray memberJson = asJson.getJSONArray("members");
		for (int cursor = 0; cursor < memberJson.size(); cursor++) {
			this.memberEntityIds.add(memberJson.getLong(cursor));
		}

		this.name = asJson.getString("name");
		this.shift = EnumParser.getEnumValue(asJson, "shift", MilitaryShift.class, MilitaryShift.DAYTIME);
		this.formation = relatedStores.squadFormationDictionary.getByName(asJson.getString("formation"));
		if (this.formation == null) {
			throw new InvalidSaveException("Could not find a squad formation with name " + asJson.getString("formation"));
		}
		this.currentOrderType = EnumParser.getEnumValue(asJson, "orderType", SquadOrderType.class, SquadOrderType.TRAINING);

		this.guardingLocation = JSONUtils.gridPoint2(asJson.getJSONObject("guardingLocation"));

		this.emblemName = asJson.getString("emblemName");

		JSONArray attackEntityJson = asJson.getJSONArray("attackEntityIds");
		if (attackEntityJson != null) {
			for (int cursor = 0; cursor < attackEntityJson.size(); cursor++) {
				this.attackEntityIds.add(attackEntityJson.getLong(cursor));
			}
		}

		savedGameStateHolder.squads.put(this.id, this);
	}

}
