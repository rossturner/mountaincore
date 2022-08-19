package technology.rocketjump.saul.military.model;

import com.alibaba.fastjson.JSONObject;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.Persistable;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import java.util.HashSet;
import java.util.Set;

/**
 * A grouping of military dwarves into a single unit to give orders to
 */
public class Squad implements Persistable {

	private long id; // Think this will just be 1 to 6 for labelling as such in the UI
	private final Set<Long> memberEntityIds = new HashSet<>();

	private String name;
	private MilitaryShift shift = MilitaryShift.DAYTIME;
	private SquadFormation formation = SquadFormation.SINGLE_SPACED_LINE;

	// TODO something to keep orders


	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public Set<Long> getMemberEntityIds() {
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

	@Override
	public void writeTo(SavedGameStateHolder savedGameStateHolder) {
		if (savedGameStateHolder.squads.containsKey(id)) {
			return;
		}

		JSONObject asJson = new JSONObject(true);
		asJson.put("id", id);

		throw new RuntimeException("Implement the other fields");

		/*
		savedGameStateHolder.squadsJson.add(asJson);
		savedGameStateHolder.squads.put(id, this);
		*/
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		this.id = asJson.getLong("id");

		throw new RuntimeException("Implement the other fields");
	}
}
