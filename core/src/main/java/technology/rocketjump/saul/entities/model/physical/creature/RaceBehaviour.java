package technology.rocketjump.saul.entities.model.physical.creature;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import technology.rocketjump.saul.entities.ai.goap.EntityNeed;
import technology.rocketjump.saul.entities.ai.goap.Schedule;
import technology.rocketjump.saul.entities.components.BehaviourComponent;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RaceBehaviour {

	private String behaviourName;
	@JsonIgnore
	private Class<? extends BehaviourComponent> behaviourClass;

	private String scheduleName;
	@JsonIgnore
	private Schedule schedule;

	private List<EntityNeed> needs = new ArrayList<>();
	private RaceBehaviourGroup group;
	private AggressionResponse aggressionResponse;

	public String getBehaviourName() {
		return behaviourName;
	}

	public void setBehaviourName(String behaviourName) {
		this.behaviourName = behaviourName;
	}

	public Class<? extends BehaviourComponent> getBehaviourClass() {
		return behaviourClass;
	}

	public void setBehaviourClass(Class<? extends BehaviourComponent> behaviourClass) {
		this.behaviourClass = behaviourClass;
	}

	public String getScheduleName() {
		return scheduleName;
	}

	public void setScheduleName(String scheduleName) {
		this.scheduleName = scheduleName;
	}

	public Schedule getSchedule() {
		return schedule;
	}

	public void setSchedule(Schedule schedule) {
		this.schedule = schedule;
	}

	public RaceBehaviourGroup getGroup() {
		return group;
	}

	public void setGroup(RaceBehaviourGroup group) {
		this.group = group;
	}

	public List<EntityNeed> getNeeds() {
		return needs;
	}

	public void setNeeds(List<EntityNeed> needs) {
		this.needs = needs;
	}

	public AggressionResponse getAggressionResponse() {
		return aggressionResponse;
	}

	public void setAggressionResponse(AggressionResponse aggressionResponse) {
		this.aggressionResponse = aggressionResponse;
	}
}
