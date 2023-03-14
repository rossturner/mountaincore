package technology.rocketjump.saul.jobs.completion;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Singleton;
import technology.rocketjump.saul.entities.EntityStore;
import technology.rocketjump.saul.entities.behaviour.plants.FallingTreeBehaviour;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.jobs.model.Job;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.messaging.types.JobCompletedMessage;

import javax.inject.Inject;

@Singleton
@SuppressWarnings("unused")
public class CompleteLogging extends OnJobCompletion {
    private final EntityStore entityStore;
    private final MessageDispatcher messageDispatcher;

    @Inject
    public CompleteLogging(EntityStore entityStore, MessageDispatcher messageDispatcher) {
        this.entityStore = entityStore;
        this.messageDispatcher = messageDispatcher;
    }

    @Override
    public void onCompletion(JobCompletedMessage message) {
        Job completedJob = message.getJob();
        MapTile targetTile = gameContext.getAreaMap().getTile(completedJob.getJobLocation());
        Entity targetTree = null;
        if (targetTile != null && targetTile.hasTree()) {
            for (Entity entity : targetTile.getEntities()) {
                if (entity.getType().equals(EntityType.PLANT)) {
                    targetTree = entity;
                    break;
                }
            }
        }

        if (targetTree != null) {
            Vector2 worldPositionOfChoppingEntity = message.getCompletedByEntity().getLocationComponent().getWorldPosition();

            boolean fallToWest = true;
            if (worldPositionOfChoppingEntity.x < targetTree.getLocationComponent().getWorldPosition().x) {
                fallToWest = false;
            }

            FallingTreeBehaviour fallingTreeBehaviour = new FallingTreeBehaviour(fallToWest);
            entityStore.changeBehaviour(targetTree, fallingTreeBehaviour, messageDispatcher);
        }
    }
}
