package technology.rocketjump.mountaincore.settlement.trading;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.mountaincore.entities.SequentialIdGenerator;
import technology.rocketjump.mountaincore.entities.ai.goap.EntityNeed;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureBehaviour;
import technology.rocketjump.mountaincore.entities.behaviour.creature.CreatureGroup;
import technology.rocketjump.mountaincore.entities.behaviour.creature.TraderCreatureGroup;
import technology.rocketjump.mountaincore.entities.components.AttachedEntitiesComponent;
import technology.rocketjump.mountaincore.entities.components.Faction;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.components.LiquidContainerComponent;
import technology.rocketjump.mountaincore.entities.components.creature.MilitaryComponent;
import technology.rocketjump.mountaincore.entities.components.creature.NeedsComponent;
import technology.rocketjump.mountaincore.entities.components.creature.SkillsComponent;
import technology.rocketjump.mountaincore.entities.factories.*;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.mountaincore.entities.model.physical.item.*;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.gamecontext.GameContextAware;
import technology.rocketjump.mountaincore.jobs.SkillDictionary;
import technology.rocketjump.mountaincore.jobs.model.Skill;
import technology.rocketjump.mountaincore.materials.GameMaterialDictionary;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.misc.VectorUtils;
import technology.rocketjump.mountaincore.settlement.trading.model.TradeCaravanDefinition;
import technology.rocketjump.mountaincore.settlement.trading.model.TraderInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Singleton
public class TradeCaravanGenerator implements GameContextAware {

	private final MessageDispatcher messageDispatcher;
	private final ItemEntityAttributesFactory itemEntityAttributesFactory;
	private final ItemEntityFactory itemEntityFactory;
	private final GameMaterialDictionary materialDictionary;
	private final TradeCaravanDefinitionDictionary tradeCaravanDefinitionDictionary;
	private final VehicleEntityAttributesFactory vehicleEntityAttributesFactory;
	private final VehicleEntityFactory vehicleEntityFactory;
	private final CreatureEntityAttributesFactory creatureEntityAttributesFactory;
	private final CreatureEntityFactory creatureEntityFactory;
	private final ItemTypeDictionary itemTypeDictionary;

	private GameContext gameContext;

	@Inject
	public TradeCaravanGenerator(MessageDispatcher messageDispatcher, ItemEntityAttributesFactory itemEntityAttributesFactory,
								 ItemEntityFactory itemEntityFactory, CreatureEntityAttributesFactory creatureEntityAttributesFactory,
								 CreatureEntityFactory creatureEntityFactory, GameMaterialDictionary materialDictionary,
								 TradeCaravanDefinitionDictionary tradeCaravanDefinitionDictionary,
								 VehicleEntityAttributesFactory vehicleEntityAttributesFactory,
								 VehicleEntityFactory vehicleEntityFactory, ItemTypeDictionary itemTypeDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.itemEntityAttributesFactory = itemEntityAttributesFactory;

		this.itemEntityFactory = itemEntityFactory;
		this.creatureEntityAttributesFactory = creatureEntityAttributesFactory;
		this.creatureEntityFactory = creatureEntityFactory;
		this.materialDictionary = materialDictionary;
		this.tradeCaravanDefinitionDictionary = tradeCaravanDefinitionDictionary;
		this.vehicleEntityAttributesFactory = vehicleEntityAttributesFactory;
		this.vehicleEntityFactory = vehicleEntityFactory;
		this.itemTypeDictionary = itemTypeDictionary;
	}

	public List<Entity> generateTradeCaravan(Vector2 tradeSpawnLocation, TraderInfo traderInfo) {
		List<Entity> participants = new ArrayList<>();
		Random random = gameContext.getRandom();

		TraderCreatureGroup group = new TraderCreatureGroup();
		group.setGroupId(SequentialIdGenerator.nextId());
		group.setHomeLocation(VectorUtils.toGridPoint(tradeSpawnLocation));
		TradeCaravanDefinition definition = tradeCaravanDefinitionDictionary.get();
		group.setCaravanDefinition(definition);

		int numVehicles = definition.getVehicles().getMinQuantity() + random.nextInt(definition.getVehicles().getMaxQuantity() - definition.getVehicles().getMinQuantity() + 1);

		for (int i = 0; i < numVehicles; i++) {
			Entity vehicle = vehicleEntityFactory.create(vehicleEntityAttributesFactory.create(definition.getVehicles().getVehicleType()), VectorUtils.toGridPoint(tradeSpawnLocation), gameContext, Faction.MERCHANTS);
			participants.add(vehicle);
			group.addMemberId(vehicle.getId());

			AttachedEntitiesComponent vehicleAttachedEntities = new AttachedEntitiesComponent();
			vehicleAttachedEntities.init(vehicle, messageDispatcher, gameContext);
			vehicle.addComponent(vehicleAttachedEntities);

			Entity draughtAnimal = creatureEntityFactory.create(creatureEntityAttributesFactory.create(definition.getVehicles().getDraughtAnimalRace()), tradeSpawnLocation, EntityAssetOrientation.DOWN.toVector2(), gameContext, Faction.MERCHANTS);
			participants.add(draughtAnimal);
			vehicleAttachedEntities.addAttachedEntity(draughtAnimal, ItemHoldPosition.VEHICLE_DRAUGHT_ANIMAL);

			int numTraders = definition.getTraders().getMinQuantityPerVehicle() + random.nextInt(definition.getTraders().getMaxQuantityPerVehicle() - definition.getTraders().getMinQuantityPerVehicle() + 1);
			int numGuards = definition.getGuards().getMinQuantityPerVehicle() + random.nextInt(definition.getGuards().getMaxQuantityPerVehicle() - definition.getGuards().getMinQuantityPerVehicle() + 1);
			boolean driverAttached = false;
			for (int counter = 0; counter < numTraders; counter++) {
				Entity trader = createCreatureEntity(tradeSpawnLocation, definition.getTraders(), group);
				participants.add(trader);

				if (!driverAttached) {
					vehicleAttachedEntities.addAttachedEntity(trader, ItemHoldPosition.VEHICLE_DRIVER);
					driverAttached = true;
				}
			}
			for (int counter = 0; counter < numGuards; counter++) {
				Entity guard = createCreatureEntity(tradeSpawnLocation, definition.getGuards(), group);
				participants.add(guard);
			}

			InventoryComponent vehicleInventory = vehicle.getOrCreateComponent(InventoryComponent.class);
			vehicleInventory.setAddAsAllocationPurpose(null); // Add items as not allocated to be sold later
			int totalGoodsValue = 0;
			int totalItemStacks = 0;

			List<ItemTypeWithMaterial> requestedItems = new ArrayList<>();
			// double up on requested items
			requestedItems.addAll(traderInfo.getRequestedItemsForNextVisit());
			requestedItems.addAll(traderInfo.getRequestedItemsForNextVisit());

			while (totalGoodsValue < definition.getVehicles().getMaxValuePerVehicleInventory() && totalItemStacks < definition.getVehicles().getImportInventoryPerVehicle()) {

				ItemEntityAttributes itemAttributes;
				if (requestedItems.isEmpty()) {
					ItemType itemType = itemTypeDictionary.getTradeImports().get(random.nextInt(itemTypeDictionary.getTradeImports().size()));
					itemAttributes = itemEntityAttributesFactory.createItemAttributes(itemType, itemType.getMaxStackSize());
				} else {
					Collections.shuffle(requestedItems);
					ItemTypeWithMaterial requested = requestedItems.remove(0);

					itemAttributes = itemEntityAttributesFactory.createItemAttributes(requested.getItemType(), requested.getItemType().getMaxStackSize(), requested.getMaterial());
				}
				Entity itemEntity = itemEntityFactory.create(itemAttributes, null, true, gameContext, Faction.MERCHANTS);

				vehicleInventory.add(itemEntity, vehicle, messageDispatcher, gameContext.getGameClock());
				totalGoodsValue += itemAttributes.getTotalValue();
				totalItemStacks++;
			}
			messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, vehicle);
		}
		return participants;
	}

	private Entity createCreatureEntity(Vector2 tradeSpawnLocation, TradeCaravanDefinition.TradeCaravanCreatureDescriptor creatureDescriptor, CreatureGroup traderCreatureGroup) {
		CreatureEntityAttributes traderAttributes = creatureEntityAttributesFactory.create(creatureDescriptor.getRace());
		Entity creature = creatureEntityFactory.create(traderAttributes, tradeSpawnLocation, EntityAssetOrientation.DOWN.toVector2(), gameContext, Faction.MERCHANTS);
		if (creature.getBehaviourComponent() instanceof CreatureBehaviour creatureBehaviour) {
			creatureBehaviour.setCreatureGroup(traderCreatureGroup);
		}

		NeedsComponent needsComponent = new NeedsComponent(List.of(EntityNeed.SLEEP, EntityNeed.FOOD, EntityNeed.DRINK), gameContext.getRandom());
		creature.addComponent(needsComponent);

		Skill profession = creatureDescriptor.getProfession();
		if (profession == null) {
			profession = SkillDictionary.NULL_PROFESSION;
		}

		SkillsComponent skillsComponent = creature.getOrCreateComponent(SkillsComponent.class);
		skillsComponent.setSkillLevel(profession, 50);
		skillsComponent.activateProfession(profession);

		addInventoryAndEquipment(creature, creatureDescriptor);
		messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, creature);
		return creature;
	}

	private void addInventoryAndEquipment(Entity entity, TradeCaravanDefinition.TradeCaravanCreatureDescriptor creatureDescriptor) {
		InventoryComponent inventoryComponent = entity.getOrCreateComponent(InventoryComponent.class);

		for (QuantifiedItemTypeWithMaterial inventoryRequirement : creatureDescriptor.getInventoryItems()) {
			ItemEntityAttributes itemAttributes = itemEntityAttributesFactory.createItemAttributes(inventoryRequirement.getItemType(), inventoryRequirement.getQuantity(), inventoryRequirement.getMaterial());
			Entity inventoryItem = itemEntityFactory.create(itemAttributes, null, true, gameContext, Faction.MERCHANTS);

			LiquidContainerComponent liquidContainerComponent = inventoryItem.getComponent(LiquidContainerComponent.class);
			if (liquidContainerComponent != null) {
				liquidContainerComponent.setTargetLiquidMaterial(pickThirstQuenchingLiquid());
				liquidContainerComponent.setLiquidQuantity(liquidContainerComponent.getMaxLiquidCapacity());
			}

			inventoryComponent.add(inventoryItem, entity, messageDispatcher, gameContext.getGameClock());
		}

		MilitaryComponent militaryComponent = new MilitaryComponent();
		militaryComponent.init(entity, messageDispatcher, gameContext);
		entity.addComponent(militaryComponent);

		if (!creatureDescriptor.getWeaponItemTypes().isEmpty()) {
			String weaponItemTypeName = creatureDescriptor.getWeaponItemTypes().get(gameContext.getRandom().nextInt(creatureDescriptor.getWeaponItemTypes().size()));
			if (weaponItemTypeName != null) {
				ItemType itemType = itemTypeDictionary.getByName(weaponItemTypeName);
				ItemEntityAttributes weaponItemAttributes = itemEntityAttributesFactory.createItemAttributes(itemType, 1, pickMaterial(itemType.getPrimaryMaterialType()));
				Entity weaponItem = itemEntityFactory.create(weaponItemAttributes, null, true, gameContext, Faction.MERCHANTS);
				inventoryComponent.add(weaponItem, entity, messageDispatcher, gameContext.getGameClock());

				militaryComponent.setAssignedWeaponId(weaponItem.getId());
				militaryComponent.addToMilitary(-1);

				SkillsComponent skillsComponent = entity.getOrCreateComponent(SkillsComponent.class);
				skillsComponent.setSkillLevel(itemType.getWeaponInfo().getCombatSkill(), creatureDescriptor.getMinWeaponSkill() +
						gameContext.getRandom().nextInt(creatureDescriptor.getMaxWeaponSkill() - creatureDescriptor.getMinWeaponSkill()));
			}
		}

		if (!creatureDescriptor.getShieldItemTypes().isEmpty()) {
			String shieldItemTypeName = creatureDescriptor.getShieldItemTypes().get(gameContext.getRandom().nextInt(creatureDescriptor.getShieldItemTypes().size()));
			if (shieldItemTypeName != null) {
				ItemType itemType = itemTypeDictionary.getByName(shieldItemTypeName);
				ItemEntityAttributes shieldItemAttributes = itemEntityAttributesFactory.createItemAttributes(itemType, 1, pickMaterial(itemType.getPrimaryMaterialType()));
				Entity shieldItem = itemEntityFactory.create(shieldItemAttributes, null, true, gameContext, Faction.MERCHANTS);
				inventoryComponent.add(shieldItem, entity, messageDispatcher, gameContext.getGameClock());

				militaryComponent.setAssignedShieldId(shieldItem.getId());
			}
		}

		if (!creatureDescriptor.getArmorItemTypes().isEmpty()) {
			String armorItemTypeName = creatureDescriptor.getArmorItemTypes().get(gameContext.getRandom().nextInt(creatureDescriptor.getArmorItemTypes().size()));
			if (armorItemTypeName != null) {
				ItemType itemType = itemTypeDictionary.getByName(armorItemTypeName);
				ItemEntityAttributes armorItemAttributes = itemEntityAttributesFactory.createItemAttributes(itemType, 1, pickMaterial(itemType.getPrimaryMaterialType()));
				Entity armorItem = itemEntityFactory.create(armorItemAttributes, null, true, gameContext, Faction.MERCHANTS);
				militaryComponent.setAssignedArmorId(armorItem.getId());
				entity.getOrCreateComponent(EquippedItemComponent.class).setEquippedClothing(armorItem, entity, messageDispatcher);
			}
		}

	}

	private GameMaterial pickThirstQuenchingLiquid() {
		List<GameMaterial> thirstQuenchingMaterials = new ArrayList<>(materialDictionary.getThirstQuenchingMaterials());
		return thirstQuenchingMaterials.get(gameContext.getRandom().nextInt(thirstQuenchingMaterials.size()));
	}

	private GameMaterial pickMaterial(GameMaterialType materialType) {
		List<GameMaterial> materials = materialDictionary.getByType(materialType).stream()
				.filter(GameMaterial::isUseInRandomGeneration)
				.toList();
		return materials.get(gameContext.getRandom().nextInt(materials.size()));
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
