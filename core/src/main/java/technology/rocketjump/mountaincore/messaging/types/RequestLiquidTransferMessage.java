package technology.rocketjump.mountaincore.messaging.types;

import com.badlogic.gdx.math.Vector2;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.jobs.model.JobPriority;
import technology.rocketjump.mountaincore.jobs.model.Skill;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;

public class RequestLiquidTransferMessage {

	public final GameMaterial targetLiquidMaterial;
	public final boolean useSmallCapacityZones;
	public final Entity requesterEntity;
	public final Vector2 requesterPosition;
	public final ItemType liquidContainerItemType;
	public final JobCreatedCallback jobCreatedCallback;
	public final Skill requiredProfession;
	public final JobPriority jobPriority;

	public RequestLiquidTransferMessage(GameMaterial targetLiquidMaterial, boolean useSmallCapacityZones, Entity requesterEntity, Vector2 requesterPosition,
										ItemType liquidContainerItemType, Skill requiredProfession, JobPriority jobPriority, JobCreatedCallback jobCreatedCallback) {
		this.targetLiquidMaterial = targetLiquidMaterial;
		this.useSmallCapacityZones = useSmallCapacityZones;
		this.requesterEntity = requesterEntity;
		this.requesterPosition = requesterPosition;
		this.liquidContainerItemType = liquidContainerItemType;
		this.jobCreatedCallback = jobCreatedCallback;
		this.requiredProfession = requiredProfession;
		this.jobPriority = jobPriority;
	}
}
