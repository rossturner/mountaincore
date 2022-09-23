package technology.rocketjump.saul.invasions;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.saul.entities.components.Faction;
import technology.rocketjump.saul.entities.components.FactionComponent;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.components.creature.MilitaryComponent;
import technology.rocketjump.saul.entities.factories.CreatureEntityAttributesFactory;
import technology.rocketjump.saul.entities.factories.CreatureEntityFactory;
import technology.rocketjump.saul.entities.factories.ItemEntityAttributesFactory;
import technology.rocketjump.saul.entities.factories.ItemEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemQuality;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.QuantifiedItemTypeWithMaterial;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.invasions.model.InvasionDefinition;
import technology.rocketjump.saul.invasions.model.InvasionEquipmentDescriptor;
import technology.rocketjump.saul.invasions.model.InvasionParticipant;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.materials.model.GameMaterialType;
import technology.rocketjump.saul.messaging.MessageType;

import java.util.*;

@Singleton
public class InvasionGenerator implements GameContextAware {

	private final MessageDispatcher messageDispatcher;
	private final ItemEntityAttributesFactory itemEntityAttributesFactory;
	private final ItemEntityFactory itemEntityFactory;
	private final CreatureEntityAttributesFactory creatureEntityAttributesFactory;
	private final CreatureEntityFactory creatureEntityFactory;
	private final GameMaterialDictionary materialDictionary;
	private GameContext gameContext;

	@Inject
	public InvasionGenerator(MessageDispatcher messageDispatcher, ItemEntityAttributesFactory itemEntityAttributesFactory,
							 ItemEntityFactory itemEntityFactory, CreatureEntityAttributesFactory creatureEntityAttributesFactory, CreatureEntityFactory creatureEntityFactory, GameMaterialDictionary materialDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.itemEntityAttributesFactory = itemEntityAttributesFactory;

		this.itemEntityFactory = itemEntityFactory;
		this.creatureEntityAttributesFactory = creatureEntityAttributesFactory;
		this.creatureEntityFactory = creatureEntityFactory;
		this.materialDictionary = materialDictionary;
	}

	public List<Entity> generateInvasionParticipants(InvasionDefinition definition, Vector2 invasionLocation, int pointsBudget) {
		List<Entity> participants = new ArrayList<>();
		Random random = gameContext.getRandom();
		Map<GameMaterialType, GameMaterial> invasionMaterials = new HashMap();

		int pointsSpent = 0;
		while (pointsSpent < pointsBudget) {
			InvasionParticipant participant = definition.getParticipants().get(random.nextInt(definition.getParticipants().size()));

			CreatureEntityAttributes creatureAttributes = creatureEntityAttributesFactory.create(participant.getRace());
			pointsSpent += participant.getBasePointsCost();
			Entity creature = creatureEntityFactory.create(creatureAttributes, invasionLocation, EntityAssetOrientation.DOWN.toVector2(), gameContext, Faction.HOSTILE_INVASION);
			InventoryComponent inventoryComponent = creature.getOrCreateComponent(InventoryComponent.class);
			MilitaryComponent militaryComponent = new MilitaryComponent();
			militaryComponent.init(creature, messageDispatcher, gameContext);
			creature.addComponent(militaryComponent);
			EquippedItemComponent equippedItemComponent = creature.getOrCreateComponent(EquippedItemComponent.class);

			for (QuantifiedItemTypeWithMaterial inventoryEntry : participant.getFixedInventory()) {
				ItemEntityAttributes inventoryItemAttributes = itemEntityAttributesFactory.createItemAttributes(inventoryEntry.getItemType(), inventoryEntry.getQuantity(), inventoryEntry.getMaterial());
				if (!inventoryEntry.getItemType().isStackable()) {
					inventoryItemAttributes.setItemQuality(pickQuality(participant.getItemQualities()));
				}
				Entity inventoryEntity = itemEntityFactory.create(inventoryItemAttributes, null, true, gameContext);
				inventoryEntity.getOrCreateComponent(FactionComponent.class).setFaction(Faction.HOSTILE_INVASION);
				inventoryComponent.add(inventoryEntity, creature, messageDispatcher, gameContext.getGameClock());
			}

			InvasionEquipmentDescriptor weaponOption = participant.getEquipmentOptions().getWeapons().get(random.nextInt(participant.getEquipmentOptions().getWeapons().size()));
			Entity weaponEntity = null;
			if (!weaponOption.isNone()) {
				ItemQuality quality = pickQuality(participant.getItemQualities());
				int itemPointCost = Math.round(quality.combatMultiplier * (float)weaponOption.getStandardPointsCost());
				pointsSpent += itemPointCost;
				weaponEntity = buildEquipment(weaponOption, quality, invasionMaterials);
				if (weaponEntity != null) {
					militaryComponent.setAssignedWeaponId(weaponEntity.getId());
					equippedItemComponent.setMainHandItem(weaponEntity, creature, messageDispatcher);
				}
			}

			InvasionEquipmentDescriptor shieldOption = participant.getEquipmentOptions().getShield().get(random.nextInt(participant.getEquipmentOptions().getShield().size()));
			boolean weaponIsTwoHanded = false;
			if (weaponEntity != null) {
				ItemEntityAttributes weaponAttributes = (ItemEntityAttributes) weaponEntity.getPhysicalEntityComponent().getAttributes();
				weaponIsTwoHanded = weaponAttributes.getItemType().getWeaponInfo().isTwoHanded();
			}
			if (!shieldOption.isNone() && !weaponIsTwoHanded) {
				ItemQuality quality = pickQuality(participant.getItemQualities());
				int itemPointCost = Math.round(quality.combatMultiplier * (float)shieldOption.getStandardPointsCost());
				pointsSpent += itemPointCost;
				Entity shieldEntity = buildEquipment(shieldOption, quality, invasionMaterials);
				if (shieldEntity != null) {
					militaryComponent.setAssignedShieldId(shieldEntity.getId());
					equippedItemComponent.setOffHandItem(shieldEntity, creature, messageDispatcher);
				}
			}

			InvasionEquipmentDescriptor armorOption = participant.getEquipmentOptions().getArmor().get(random.nextInt(participant.getEquipmentOptions().getArmor().size()));
			if (!armorOption.isNone()) {
				ItemQuality quality = pickQuality(participant.getItemQualities());
				int itemPointCost = Math.round(quality.combatMultiplier * (float)armorOption.getStandardPointsCost());
				pointsSpent += itemPointCost;
				Entity armorEntity = buildEquipment(armorOption, quality, invasionMaterials);
				if (armorEntity != null) {
					militaryComponent.setAssignedShieldId(armorEntity.getId());
					equippedItemComponent.setEquippedClothing(armorEntity, creature, messageDispatcher);
				}
			}

			messageDispatcher.dispatchMessage(MessageType.ENTITY_ASSET_UPDATE_REQUIRED, creature);
		}

		// TODO put all participants into group?

		return participants;
	}

	private Entity buildEquipment(InvasionEquipmentDescriptor equipmentDescriptor, ItemQuality quality, Map<GameMaterialType, GameMaterial> materials) {
		ItemType itemType = equipmentDescriptor.getItemType();
		if (!itemType.isStackable()) {
			Logger.error(itemType.getItemTypeName() + " should not be stackable i.e. should have ItemQuality");
			return null;
		}

		GameMaterial primaryMaterial = materials.getOrDefault(itemType.getPrimaryMaterialType(), pickMaterial(itemType.getPrimaryMaterialType()));
		materials.put(primaryMaterial.getMaterialType(), primaryMaterial);
		ItemEntityAttributes itemAttributes = itemEntityAttributesFactory.createItemAttributes(equipmentDescriptor.getItemType(), 1, primaryMaterial);
		itemAttributes.setItemQuality(quality);

		Entity itemEntity = itemEntityFactory.create(itemAttributes, null, true, gameContext);
		itemEntity.getOrCreateComponent(FactionComponent.class).setFaction(Faction.HOSTILE_INVASION);

		return itemEntity;
	}

	private GameMaterial pickMaterial(GameMaterialType materialType) {
		List<GameMaterial> materials = materialDictionary.getByType(materialType).stream()
				.filter(GameMaterial::isUseInRandomGeneration)
				.toList();
		return materials.get(gameContext.getRandom().nextInt(materials.size()));
	}

	private ItemQuality pickQuality(Map<ItemQuality, Float> itemQualities) {
		float totalRoll = itemQualities.values().stream().reduce(0f, Float::sum);
		float roll = gameContext.getRandom().nextFloat() * totalRoll;

		for (Map.Entry<ItemQuality, Float> entry : itemQualities.entrySet()) {
			roll -= entry.getValue();
			if (roll <= 0) {
				return entry.getKey();
			}
		}

		return itemQualities.keySet().iterator().next();
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
