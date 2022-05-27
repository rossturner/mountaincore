package technology.rocketjump.saul.entities.factories;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.google.inject.Inject;
import technology.rocketjump.saul.entities.EntityAssetUpdater;
import technology.rocketjump.saul.entities.behaviour.plants.PlantBehaviour;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.EntityAttributes;
import technology.rocketjump.saul.entities.model.physical.LocationComponent;
import technology.rocketjump.saul.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.saul.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.JobTypeDictionary;
import technology.rocketjump.saul.jobs.model.JobType;

import static technology.rocketjump.saul.entities.factories.ItemEntityFactory.createLocationComponent;

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
		LocationComponent locationComponent = createLocationComponent(tilePosition, attributes.getSeed());

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
