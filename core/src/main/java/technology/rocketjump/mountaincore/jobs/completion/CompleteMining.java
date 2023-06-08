package technology.rocketjump.mountaincore.jobs.completion;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import technology.rocketjump.mountaincore.constants.ConstantsRepo;
import technology.rocketjump.mountaincore.constants.WorldConstants;
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
    private final WorldConstants worldConstants;

    @Inject
    public CompleteMining(MessageDispatcher messageDispatcher, EntityStore entityStore, ConstantsRepo constantsRepo) {
        this.messageDispatcher = messageDispatcher;
        this.entityStore = entityStore;
        this.worldConstants = constantsRepo.getWorldConstants();
    }

    @Override
    public void onCompletion(JobCompletedMessage message) {
        Job completedJob = message.getJob();
        int skillLevelOfCompletion = 0;
        if (message.getCompletedBy() != null) {
            skillLevelOfCompletion = message.getCompletedBy().getSkillLevel(completedJob.getRequiredProfession());
        }


        MapTile targetTile = gameContext.getAreaMap().getTile(completedJob.getJobLocation());
        if (targetTile != null && targetTile.hasWall()) {
            Wall wall = targetTile.getWall();

            if (wall.hasOre()) {
                GameMaterial oreMaterial = wall.getOreMaterial();
                float chanceToDropItem = calculateDropChance(skillLevelOfCompletion, worldConstants.getOreHarvestMinSkillChance(), worldConstants.getOreHarvestMaxSkillChance());
                if (gameContext.getRandom().nextFloat() < chanceToDropItem) {
                    entityStore.createResourceItem(oreMaterial, completedJob.getJobLocation(), 1, wall.getMaterial());
                }
            } else {
                float chanceToDropItem = calculateDropChance(skillLevelOfCompletion, worldConstants.getStoneHarvestMinSkillChance(), worldConstants.getStoneHarvestMaxSkillChance());
                if (gameContext.getRandom().nextFloat() < chanceToDropItem) {
                    entityStore.createResourceItem(wall.getMaterial(), completedJob.getJobLocation(), 1);
                }
            }

            messageDispatcher.dispatchMessage(MessageType.REMOVE_WALL, completedJob.getJobLocation());
        }
    }

    private float calculateDropChance(int skillLevelOfCompletion, float minSkillChance, float maxSkillChance) {
        float skillLevel = (float)skillLevelOfCompletion/100f;
        return minSkillChance + ((maxSkillChance - minSkillChance) * (skillLevel));
    }

}
