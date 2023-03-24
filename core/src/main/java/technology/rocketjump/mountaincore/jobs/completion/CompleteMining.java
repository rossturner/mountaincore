package technology.rocketjump.mountaincore.jobs.completion;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.entities.EntityStore;
import technology.rocketjump.mountaincore.jobs.model.Job;
import technology.rocketjump.mountaincore.mapping.tile.MapTile;
import technology.rocketjump.mountaincore.mapping.tile.wall.Wall;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.JobCompletedMessage;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
@SuppressWarnings("unused")
public class CompleteMining extends OnJobCompletion {
    private final MessageDispatcher messageDispatcher;
    private final EntityStore entityStore;

    @Inject
    public CompleteMining(MessageDispatcher messageDispatcher, EntityStore entityStore) {
        this.messageDispatcher = messageDispatcher;
        this.entityStore = entityStore;
    }

    @Override
    public void onCompletion(JobCompletedMessage message) {
        Job completedJob = message.getJob();
        float skillLevelOfCompletion = 0f;
        if (message.getCompletedBy() != null) {
            skillLevelOfCompletion = message.getCompletedBy().getSkillLevel(completedJob.getRequiredProfession());
        }


        MapTile targetTile = gameContext.getAreaMap().getTile(completedJob.getJobLocation());
        if (targetTile != null && targetTile.hasWall()) {
            Wall wall = targetTile.getWall();

            if (wall.hasOre()) {
                GameMaterial oreMaterial = wall.getOreMaterial();
                if (gameContext.getRandom().nextInt(100) < skillLevelOfCompletion + 10) {
                    entityStore.createResourceItem(oreMaterial, completedJob.getJobLocation(), 1, wall.getMaterial());
                }
            } else {
                if (gameContext.getRandom().nextInt(100) < skillLevelOfCompletion) {
                    entityStore.createResourceItem(wall.getMaterial(), completedJob.getJobLocation(), 1);
                }
            }

            messageDispatcher.dispatchMessage(MessageType.REMOVE_WALL, completedJob.getJobLocation());
        }
    }

}
