package technology.rocketjump.saul.jobs.completion;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.saul.jobs.model.Job;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.JobCompletedMessage;
import technology.rocketjump.saul.rooms.constructions.Construction;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@SuppressWarnings("unused")
public class CompleteConstruction extends OnJobCompletion {
    private final MessageDispatcher messageDispatcher;

    @Inject
    public CompleteConstruction(MessageDispatcher messageDispatcher) {
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public void onCompletion(JobCompletedMessage message) {
        Job completedJob = message.getJob();
        MapTile targetTile = gameContext.getAreaMap().getTile(completedJob.getJobLocation());
        Construction construction = targetTile.getConstruction();
        messageDispatcher.dispatchMessage(MessageType.CONSTRUCTION_COMPLETED, construction);
    }
}
