package technology.rocketjump.mountaincore.entities.factories;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import org.pmw.tinylog.Logger;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.mountaincore.entities.EntityAssetUpdater;
import technology.rocketjump.mountaincore.entities.behaviour.items.ItemBehaviour;
import technology.rocketjump.mountaincore.entities.components.*;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.LocationComponent;
import technology.rocketjump.mountaincore.entities.model.physical.PhysicalEntityComponent;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.materials.GameMaterialDictionary;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.messaging.MessageType;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class ItemEntityFactory {

	private static final float MAX_POSITION_OFFSET = 0.05f;
	private static final float ITEM_RADIUS = 0.4f;

	private final MessageDispatcher messageDispatcher;
	private final GameMaterialDictionary gameMaterialDictionary;
	private final EntityAssetUpdater entityAssetUpdater;
	private float MERCHANT_VALUE_MULTIPLIER = 1.3f;

	@Inject
	public ItemEntityFactory(MessageDispatcher messageDispatcher, GameMaterialDictionary gameMaterialDictionary, EntityAssetUpdater entityAssetUpdater) {
		this.messageDispatcher = messageDispatcher;
		this.gameMaterialDictionary = gameMaterialDictionary;
		this.entityAssetUpdater = entityAssetUpdater;
	}
	
	public Entity createByItemType(ItemType itemType, GameContext gameContext, boolean addToGameContext, Faction faction) {
		ItemEntityAttributes attributes = new ItemEntityAttributes(gameContext.getRandom().nextLong());
		attributes.setItemType(itemType);

		for (GameMaterialType requiredMaterialType : itemType.getMaterialTypes()) {
			List<GameMaterial> materialsToPickFrom;
			if (requiredMaterialType.equals(itemType.getPrimaryMaterialType()) && !itemType.getSpecificMaterials().isEmpty()) {
				materialsToPickFrom = itemType.getSpecificMaterials();
			} else {
				materialsToPickFrom = gameMaterialDictionary.getByType(requiredMaterialType).stream()
						.filter(GameMaterial::isUseInRandomGeneration)
						.collect(Collectors.toList());
			}
			if (materialsToPickFrom.isEmpty()) {
				// No use-in-random-generation materials
				Logger.error("Needed a material of type " + requiredMaterialType + " to use in random generation");
			}
			GameMaterial material = materialsToPickFrom.get(gameContext.getRandom().nextInt(materialsToPickFrom.size()));
			attributes.setMaterial(material);
		}
		attributes.setQuantity(1);

		return this.create(attributes, null, addToGameContext, gameContext, faction);
	}


	public Entity create(ItemEntityAttributes attributes, GridPoint2 tilePosition, boolean addToGameContext, GameContext gameContext, Faction faction) {
		PhysicalEntityComponent physicalComponent = new PhysicalEntityComponent();
		physicalComponent.setAttributes(attributes);
		if (attributes.getPrimaryMaterial() == null) {
			attributes.setMaterial(gameMaterialDictionary.getExampleMaterial(attributes.getItemType().getPrimaryMaterialType()));
		}
		BehaviourComponent behaviorComponent = new ItemBehaviour();
		LocationComponent locationComponent = createLocationComponent(tilePosition, attributes.getSeed());

		Entity entity = new Entity(EntityType.ITEM, physicalComponent, behaviorComponent, locationComponent, messageDispatcher, gameContext);
		entity.addComponent(new ItemAllocationComponent());
		entity.addComponent(new FactionComponent());
		entity.init(messageDispatcher, gameContext);

		attributes.getAllMaterials().stream().filter(m -> m.getOxidisation() != null)
				.findAny()
				.ifPresent((a) -> {
					OxidisationComponent oxidisationComponent = new OxidisationComponent();
					oxidisationComponent.init(entity, messageDispatcher, gameContext);
					entity.addComponent(oxidisationComponent);
				});

		entity.getOrCreateComponent(FactionComponent.class).setFaction(faction);
		if (faction.equals(Faction.MERCHANTS) && !attributes.getItemType().isValueFixedToMaterial()) {
			attributes.setValuePerItem((Math.round(attributes.getValuePerItem() * MERCHANT_VALUE_MULTIPLIER)));
		}

		entityAssetUpdater.updateEntityAssets(entity);
		if (addToGameContext) {
			messageDispatcher.dispatchMessage(MessageType.ENTITY_CREATED, entity);
		}
		return entity;
	}

	public static LocationComponent createLocationComponent(GridPoint2 tilePosition, long seed) {
		Random random = new RandomXS128(seed);

		LocationComponent locationComponent = new LocationComponent();
		if (tilePosition != null) {
			Vector2 worldPosition = new Vector2(tilePosition.x + 0.5f, tilePosition.y + 0.5f);
			// Randomise world position offset
			worldPosition.x += (random.nextFloat() * MAX_POSITION_OFFSET * 2) - MAX_POSITION_OFFSET;
			worldPosition.y += (random.nextFloat() * MAX_POSITION_OFFSET * 2) - MAX_POSITION_OFFSET;

			locationComponent.setWorldPosition(worldPosition, false);
		}
		locationComponent.setFacing(EntityAssetOrientation.DOWN.toVector2().cpy());
		locationComponent.setRadius(ITEM_RADIUS);
		return locationComponent;
	}

}
