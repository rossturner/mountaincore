package technology.rocketjump.mountaincore.entities.components.furniture;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.components.EntityComponent;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.jobs.model.Job;
import technology.rocketjump.mountaincore.jobs.model.JobState;
import technology.rocketjump.mountaincore.persistence.SavedGameDependentDictionaries;
import technology.rocketjump.mountaincore.persistence.model.InvalidSaveException;
import technology.rocketjump.mountaincore.persistence.model.SavedGameStateHolder;

import java.util.ArrayList;
import java.util.List;

public class DeceasedContainerComponent implements EntityComponent {

    private List<Job> incomingHaulingJobs = new ArrayList<>();

    @Override
    public EntityComponent clone(MessageDispatcher messageDispatcher, GameContext gameContext) {
        DeceasedContainerComponent clone = new DeceasedContainerComponent();
        clone.incomingHaulingJobs = this.incomingHaulingJobs;
        return clone;
    }


    public void addHaulingJob(Job haulingJob) {
        incomingHaulingJobs.add(haulingJob);
    }

    public int getHaulingJobCount() {
        incomingHaulingJobs.removeIf(job -> job.getJobState().equals(JobState.REMOVED));
        return incomingHaulingJobs.size();
    }

    @Override
    public void writeTo(JSONObject asJson, SavedGameStateHolder savedGameStateHolder) {
        if (!incomingHaulingJobs.isEmpty()) {
            JSONArray incomingHaulingJobsJson = new JSONArray();
            for (Job haulingJob : incomingHaulingJobs) {
                haulingJob.writeTo(savedGameStateHolder);
                incomingHaulingJobsJson.add(haulingJob.getJobId());
            }
            asJson.put("jobs", incomingHaulingJobsJson);
        }

    }

    @Override
    public void readFrom(JSONObject asJson, SavedGameStateHolder savedGameStateHolder, SavedGameDependentDictionaries relatedStores) throws InvalidSaveException {
        JSONArray incomingHaulingJobsJson = asJson.getJSONArray("jobs");
        if (incomingHaulingJobsJson != null) {
            for (int cursor = 0; cursor < incomingHaulingJobsJson.size(); cursor++) {
                long jobId = incomingHaulingJobsJson.getLongValue(cursor);
                Job job = savedGameStateHolder.jobs.get(jobId);
                if (job == null) {
                    throw new InvalidSaveException("Could not find job by ID " + jobId);
                } else {
                    incomingHaulingJobs.add(job);
                }
            }
        }
    }

}
