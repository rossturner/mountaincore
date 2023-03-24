package technology.rocketjump.mountaincore.messaging.types;

import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.jobs.model.JobPriority;
import technology.rocketjump.mountaincore.jobs.model.Skill;

public class RequestHaulingMessage {

	public final Entity entityToBeMoved;
	public final Entity requestingEntity;
	public final boolean forceHaulingEvenWithoutStockpile;
	public final JobCreatedCallback callback;
	public final JobPriority jobPriority;

	private Skill specificProfessionRequired;

	public RequestHaulingMessage(Entity entityToBeMoved, Entity requestingEntity, boolean forceHaulingEvenWithoutStockpile, JobPriority jobPriority, JobCreatedCallback callback) {
		this.entityToBeMoved = entityToBeMoved;
		this.requestingEntity = requestingEntity;
		this.forceHaulingEvenWithoutStockpile = forceHaulingEvenWithoutStockpile;
		this.callback = callback;
		this.jobPriority = jobPriority;
	}

	public Entity getEntityToBeMoved() {
		return entityToBeMoved;
	}

	public boolean forceHaulingEvenWithoutStockpile() {
		return forceHaulingEvenWithoutStockpile;
	}

	public Skill getSpecificProfessionRequired() {
		return specificProfessionRequired;
	}

	public void setSpecificProfessionRequired(Skill specificProfessionRequired) {
		this.specificProfessionRequired = specificProfessionRequired;
	}

}
