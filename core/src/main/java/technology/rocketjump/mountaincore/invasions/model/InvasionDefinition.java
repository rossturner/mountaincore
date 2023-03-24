package technology.rocketjump.mountaincore.invasions.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.mountaincore.misc.Name;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class InvasionDefinition {

	@Name
	private String name;
	private InvasionTrigger triggeredBy;
	private InvasionObjective objective;

	private int minDaysUntilFirstInvasion;
	private int minDaysBetweenInvasions;
	private int invasionHappensWithinDays;

	private List<InvasionParticipant> participants;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public InvasionTrigger getTriggeredBy() {
		return triggeredBy;
	}

	public void setTriggeredBy(InvasionTrigger triggeredBy) {
		this.triggeredBy = triggeredBy;
	}

	public InvasionObjective getObjective() {
		return objective;
	}

	public void setObjective(InvasionObjective objective) {
		this.objective = objective;
	}

	public int getMinDaysUntilFirstInvasion() {
		return minDaysUntilFirstInvasion;
	}

	public void setMinDaysUntilFirstInvasion(int minDaysUntilFirstInvasion) {
		this.minDaysUntilFirstInvasion = minDaysUntilFirstInvasion;
	}

	public int getMinDaysBetweenInvasions() {
		return minDaysBetweenInvasions;
	}

	public void setMinDaysBetweenInvasions(int minDaysBetweenInvasions) {
		this.minDaysBetweenInvasions = minDaysBetweenInvasions;
	}

	public int getInvasionHappensWithinDays() {
		return invasionHappensWithinDays;
	}

	public void setInvasionHappensWithinDays(int invasionHappensWithinDays) {
		this.invasionHappensWithinDays = invasionHappensWithinDays;
	}

	public List<InvasionParticipant> getParticipants() {
		return participants;
	}

	public void setParticipants(List<InvasionParticipant> participants) {
		this.participants = participants;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		InvasionDefinition that = (InvasionDefinition) o;
		return name.equals(that.name);
	}

	@Override
	public int hashCode() {
		return name.hashCode();
	}
}
