package technology.rocketjump.mountaincore.jobs.completion;

import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.GameContextAware;
import technology.rocketjump.mountaincore.messaging.types.JobCompletedMessage;

public abstract class OnJobCompletion implements GameContextAware {


    protected GameContext gameContext;

    public abstract void onCompletion(JobCompletedMessage message);

    @Override
    public void onContextChange(GameContext gameContext) {
        this.gameContext = gameContext;
    }

    @Override
    public void clearContextRelatedState() {
        this.gameContext = null;
    }
}
