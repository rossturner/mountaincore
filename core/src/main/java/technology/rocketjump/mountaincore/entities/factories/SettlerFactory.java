package technology.rocketjump.mountaincore.entities.factories;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.entities.ai.goap.GoalDictionary;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.mountaincore.entities.components.Faction;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.components.creature.*;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.LocationComponent;
import technology.rocketjump.mountaincore.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.mountaincore.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.creature.HaulingComponent;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.jobs.model.Skill;
import technology.rocketjump.mountaincore.materials.GameMaterialDictionary;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.messaging.types.ItemPrimaryMaterialChangedMessage;

import java.util.Random;

import static technology.rocketjump.mountaincore.jobs.SkillDictionary.NULL_PROFESSION;

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
		Random random = gameContext.getRandom();
		CreatureEntityAttributes attributes = settlerAttributesFactory.create(gameContext);

		PhysicalEntityComponent physicalComponent = new PhysicalEntityComponent();
		physicalComponent.setAttributes(attributes);

		CreatureBehaviour behaviourComponent = new CreatureBehaviour();
		behaviourComponent.constructWith(goalDictionary);

		Vector2 facing = new Vector2((random.nextFloat() * 2.0f) - 1.0f, (random.nextFloat() * 2.0f) - 1.0f);
		LocationComponent locationComponent = new LocationComponent();
		locationComponent.setWorldPosition(worldPosition, true);
		locationComponent.setFacing(facing);

		Entity entity = new Entity(EntityType.CREATURE, physicalComponent, behaviourComponent, locationComponent,
				messageDispatcher, gameContext);
		entity.addComponent(new HaulingComponent());
		entity.addComponent(buildSkillsComponent(primaryProfession, secondaryProfession));
		entity.addComponent(new NeedsComponent(attributes.getRace().getBehaviour().getNeeds(), random));
		entity.addComponent(new MemoryComponent());
		entity.addComponent(new CombatStateComponent());
		entity.addComponent(new StatusComponent());
		entity.addComponent(new MilitaryComponent());

		HappinessComponent happinessComponent = entity.getOrCreateComponent(HappinessComponent.class);
		happinessComponent.add(HappinessComponent.HappinessModifier.NEW_SETTLEMENT_OPTIMISM);

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
		Entity rationItem = itemEntityFactory.createByItemType(rationItemType, gameContext, true, Faction.SETTLEMENT);
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
