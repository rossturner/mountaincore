package technology.rocketjump.saul.entities.factories;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.entities.ai.goap.GoalDictionary;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.components.creature.*;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.LocationComponent;
import technology.rocketjump.saul.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.HaulingComponent;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.model.Skill;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.ItemPrimaryMaterialChangedMessage;

import static technology.rocketjump.saul.entities.components.creature.HappinessComponent.HappinessModifier.NEW_SETTLEMENT_OPTIMISM;
import static technology.rocketjump.saul.jobs.SkillDictionary.NULL_PROFESSION;

@Singleton
public class SettlerFactory {

	private final SettlerCreatureAttributesFactory settlerAttributesFactory;
	private final ItemTypeDictionary itemTypeDictionary; // Needed to ensure order of JobType initialisation
	private final ItemEntityFactory itemEntityFactory;
	private final MessageDispatcher messageDispatcher;
	private final GameMaterialDictionary materialDictionary;
	private final GoalDictionary goalDictionary;


	@Inject
	public SettlerFactory(SettlerCreatureAttributesFactory settlerAttributesFactory,
						  ItemTypeDictionary itemTypeDictionary,
						  ItemEntityFactory itemEntityFactory, MessageDispatcher messageDispatcher,
						  GameMaterialDictionary materialDictionary, GoalDictionary goalDictionary) {
		this.settlerAttributesFactory = settlerAttributesFactory;
		this.itemTypeDictionary = itemTypeDictionary;
		this.itemEntityFactory = itemEntityFactory;
		this.messageDispatcher = messageDispatcher;
		this.materialDictionary = materialDictionary;
		this.goalDictionary = goalDictionary;
	}

	public Entity create(Vector2 worldPosition, Skill primaryProfession, Skill secondaryProfession, GameContext gameContext, boolean includeRations) {
		CreatureEntityAttributes attributes = settlerAttributesFactory.create(gameContext);

		PhysicalEntityComponent physicalComponent = new PhysicalEntityComponent();
		physicalComponent.setAttributes(attributes);

		CreatureBehaviour behaviourComponent = new CreatureBehaviour();
		behaviourComponent.constructWith(goalDictionary);

		LocationComponent locationComponent = new LocationComponent();
		locationComponent.setWorldPosition(worldPosition, true);

		Entity entity = new Entity(EntityType.CREATURE, physicalComponent, behaviourComponent, locationComponent,
				messageDispatcher, gameContext);
		entity.addComponent(new HaulingComponent());
		entity.addComponent(buildSkillsComponent(primaryProfession, secondaryProfession));
		entity.addComponent(new NeedsComponent(attributes.getRace().getBehaviour().getNeeds(), gameContext.getRandom()));
		entity.addComponent(new MemoryComponent());
		entity.addComponent(new CombatStateComponent());
		entity.addComponent(new StatusComponent());
		entity.addComponent(new MilitaryComponent());

		HappinessComponent happinessComponent = entity.getOrCreateComponent(HappinessComponent.class);
		happinessComponent.add(NEW_SETTLEMENT_OPTIMISM);

		entity.init(messageDispatcher, gameContext);

		if (includeRations) {
			addRations(entity, messageDispatcher, gameContext);
		}

		messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, entity);
		messageDispatcher.dispatchMessage(MessageType.ENTITY_CREATED, entity);
		return entity;
	}

	private SkillsComponent buildSkillsComponent(Skill primaryProfession, Skill secondaryProfession) {
		SkillsComponent skillsComponent = new SkillsComponent();
		if (primaryProfession == null) {
			primaryProfession = NULL_PROFESSION;
		}
		skillsComponent.setSkillLevel(primaryProfession, 50);
		if (secondaryProfession != null && !secondaryProfession.equals(primaryProfession)) {
			skillsComponent.setSkillLevel(secondaryProfession, 30);
		}
		return skillsComponent;
	}

	private void addRations(Entity settler, MessageDispatcher messageDispatcher, GameContext gameContext) {
		InventoryComponent inventoryComponent = settler.getOrCreateComponent(InventoryComponent.class);

		ItemType rationItemType = itemTypeDictionary.getByName("Product-Ration");
		Entity rationItem = itemEntityFactory.createByItemType(rationItemType, gameContext, true);
		ItemEntityAttributes attributes = (ItemEntityAttributes) rationItem.getPhysicalEntityComponent().getAttributes();
		attributes.setQuantity(50);
		GameMaterial oldPrimaryMaterial = attributes.getPrimaryMaterial();
		attributes.setMaterial(materialDictionary.getByName("Rockbread"));
		if (!oldPrimaryMaterial.equals(attributes.getPrimaryMaterial())) {
			messageDispatcher.dispatchMessage(MessageType.ITEM_PRIMARY_MATERIAL_CHANGED, new ItemPrimaryMaterialChangedMessage(rationItem, oldPrimaryMaterial));
		}

		inventoryComponent.add(rationItem, settler, messageDispatcher, gameContext.getGameClock());
	}
}
