package technology.rocketjump.mountaincore.jobs;

import com.badlogic.gdx.Gdx;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.Updatable;
import technology.rocketjump.mountaincore.jobs.model.Job;
import technology.rocketjump.mountaincore.jobs.model.JobPriority;
import technology.rocketjump.mountaincore.jobs.model.JobType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collection;

@Singleton
public class JobPriorityUpdater implements Updatable {

    private static final float UPDATE_PERIOD_SECONDS = 1.0f;

    private final JobStore jobStore;
    private final JobType haulingJobType;
    private GameContext gameContext;
    private float deltaTimeAcc;

    @Inject
    public JobPriorityUpdater(JobStore jobStore, JobTypeDictionary jobTypeDictionary) {
        this.jobStore = jobStore;
        this.haulingJobType = jobTypeDictionary.getByName("HAULING");
    }

    @Override
    public void onContextChange(GameContext gameContext) {
        this.gameContext = gameContext;
    }

    @Override
    public void clearContextRelatedState() {

    }

    @Override
    public void update(float otherDeltaTime) {
        final float deltaTime = Gdx.graphics.getDeltaTime();
        if (gameContext != null) {
            deltaTimeAcc += deltaTime;
            if (deltaTimeAcc > UPDATE_PERIOD_SECONDS) {
                deltaTimeAcc = 0f;

                Collection<Job> haulingJobs = jobStore.getByType(haulingJobType);
                for (Job haulingJob : haulingJobs) {
                    if (JobPriority.DISABLED == haulingJob.getJobPriority()) {
                        haulingJob.setJobPriority(JobPriority.HIGHEST); //when detecting an issue/broken job. Shouldn't really happen
                    }
                }
            }
        }
    }

    @Override
    public boolean runWhilePaused() {
        return true;
    }
}
