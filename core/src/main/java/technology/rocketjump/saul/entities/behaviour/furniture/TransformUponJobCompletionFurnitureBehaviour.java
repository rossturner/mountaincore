package technology.rocketjump.saul.entities.behaviour.furniture;

import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.model.Job;
import technology.rocketjump.saul.jobs.model.JobPriority;
import technology.rocketjump.saul.jobs.model.JobState;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.TransformFurnitureMessage;
import technology.rocketjump.saul.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.saul.persistence.model.InvalidSaveException;
import technology.rocketjump.saul.persistence.model.SavedGameStateHolder;

import static technology.rocketjump.saul.entities.behaviour.furniture.CraftingStationBehaviour.getAnyNavigableWorkspace;

public class TransformUponJobCompletionFurnitureBehaviour extends FurnitureBehaviour implements OnJobCompletion, Prioritisable {

	private Job jobToComplete;

	@Override
	public void init(Entity parentEntity, MessageDispatcher messageDispatcher, GameContext gameContext) {
		super.init(parentEntity, messageDispatcher, gameContext);
		if (relatedJobTypes.size() != 1) {
			Logger.error("Expecting 1 related job type for " + this.getClass().getSimpleName());
		}
		if (relatedFurnitureTypes.size() != 1) {
			Logger.error("Expecting 1 related furniture type for " + this.getClass().getSimpleName());
		}
	}

	@Override
	public void setPriority(JobPriority jobPriority) {
		super.setPriority(jobPriority);
		if (jobToComplete != null) {
			jobToComplete.setJobPriority(priority);
		}
	}

	@Override
	public void infrequentUpdate(GameContext gameContext) {
		super.infrequentUpdate(gameContext);

		if (jobToComplete != null && jobToComplete.getJobState().equals(JobState.REMOVED)) {
			jobToComplete = null;
		}

		if (parentEntity.isOnFire()) {
			if (jobToComplete != null) {
				messageDispatcher.dispatchMessage(MessageType.JOB_REMOVED, jobToComplete);
				jobToComplete = null;
			}
			return;
		}

		if (jobToComplete == null) {
			FurnitureLayout.Workspace navigableWorkspace = getAnyNavigableWorkspace(parentEntity, gameContext.getAreaMap());
			if (navigableWorkspace != null) {
				jobToComplete = new Job(relatedJobTypes.get(0));
				jobToComplete.setJobPriority(priority);
				jobToComplete.setTargetId(parentEntity.getId());
				jobToComplete.setJobLocation(navigableWorkspace.getAccessedFrom());
				messageDispatcher.dispatchMessage(MessageType.JOB_CREATED, jobToComplete);
			}
		}


	}

	@Override
	public void jobCompleted(GameContext gameContext, Entity completedByEntity) {
		messageDispatcher.dispatchMessage(MessageType.TRANSFORM_FURNITURE_TYPE, new TransformFurnitureMessage(parentEntity, relatedFurnitureTypes.get(0)));
	}

	@Override
	public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
		super.writeTo(asJson, savedGameStateHolder);
		if (jobToComplete != null) {
			asJson.put("jobToComplete", jobToComplete.getJobId());
		}
	}

	@Override
	public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
		super.readFrom(asJson, savedGameStateHolder, relatedStores);

		Long jobId = asJson.getLong("jobToComplete");
		if (jobId != null) {
			jobToComplete = savedGameStateHolder.jobs.get(jobId);
			if (jobToComplete == null) {
				throw new InvalidSaveException("Could not find job with ID " + jobId);
			}
		}
	}

}
