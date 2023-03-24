package technology.rocketjump.mountaincore.entities.factories;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.google.inject.Inject;
import technology.rocketjump.mountaincore.entities.EntityAssetUpdater;
import technology.rocketjump.mountaincore.entities.behaviour.plants.PlantBehaviour;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.EntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.LocationComponent;
import technology.rocketjump.mountaincore.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.mountaincore.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.jobs.JobTypeDictionary;
import technology.rocketjump.mountaincore.jobs.model.JobType;

public class PlantEntityFactory {

	private static final float MAX_POSITION_OFFSET = 0.2f;
	private static final float PLANT_RADIUS = 0.4f;

	private final MessageDispatcher messageDispatcher;
	private final EntityAssetUpdater entityAssetUpdater;
	private final JobType removePestsJobType;

	@Inject
	public PlantEntityFactory(MessageDispatcher messageDispatcher, EntityAssetUpdater entityAssetUpdater, JobTypeDictionary jobTypeDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.entityAssetUpdater = entityAssetUpdater;
		this.removePestsJobType = jobTypeDictionary.getByName("REMOVE_PESTS_FROM_CROP");
	}


	public Entity create(PlantEntityAttributes attributes, GridPoint2 tilePosition, GameContext gameContext) {
		PhysicalEntityComponent physicalComponent = createPhysicalComponent(attributes);
		PlantBehaviour behaviorComponent = new PlantBehaviour();
		behaviorComponent.setRemovePestsJobType(removePestsJobType);
		LocationComponent locationComponent = ItemEntityFactory.createLocationComponent(tilePosition, attributes.getSeed());

		Entity entity = new Entity(EntityType.PLANT, physicalComponent, behaviorComponent, locationComponent,
				messageDispatcher, gameContext);
		entityAssetUpdater.updateEntityAssets(entity);
		return entity;
	}

	public static PhysicalEntityComponent createPhysicalComponent(EntityAttributes attributes) {
		PhysicalEntityComponent physicalComponent = new PhysicalEntityComponent();
		physicalComponent.setAttributes(attributes);
		return physicalComponent;
	}

}
