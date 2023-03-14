package technology.rocketjump.saul.jobs.completion;

import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.messaging.types.JobCompletedMessage;

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
