package technology.rocketjump.saul.entities;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.math.GridPoint2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.assets.entities.model.ColoringLayer;
import technology.rocketjump.saul.audio.model.SoundAsset;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.entities.behaviour.furniture.FurnitureBehaviour;
import technology.rocketjump.saul.entities.components.LiquidContainerComponent;
import technology.rocketjump.saul.entities.components.furniture.ConstructedEntityComponent;
import technology.rocketjump.saul.entities.components.furniture.PoweredFurnitureComponent;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.saul.entities.factories.FurnitureEntityAttributesFactory;
import technology.rocketjump.saul.entities.factories.FurnitureEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.entities.tags.DecorationFromInputTag;
import technology.rocketjump.saul.entities.tags.RequirementToColorMappingsTag;
import technology.rocketjump.saul.entities.tags.SupportsRoofTag;
import technology.rocketjump.saul.entities.tags.TagProcessor;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.gamecontext.GameContextAware;
import technology.rocketjump.saul.mapping.RoofConstructionManager;
import technology.rocketjump.saul.mapping.tile.MapTile;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.materials.model.GameMaterialType;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.messaging.types.*;

import java.util.*;

import static technology.rocketjump.saul.misc.VectorUtils.toVector;
import static technology.rocketjump.saul.rooms.constructions.ConstructionMessageHandler.findApplicableMaterial;

@Singleton
public class FurnitureEntityMessageHandler implements GameContextAware, Telegraph {

	private final MessageDispatcher messageDispatcher;
	private final FurnitureTypeDictionary furnitureTypeDictionary;
	private final FurnitureEntityAttributesFactory furnitureEntityAttributesFactory;
	private final FurnitureEntityFactory furnitureEntityFactory;
	private final ItemTypeDictionary itemTypeDictionary;
	private final Map<GameMaterialType, SoundAsset> completionSoundMapping = new EnumMap<>(GameMaterialType.class);
	private final TagProcessor tagProcessor;
	private final RoofConstructionManager roofConstructionManager;
	private final GameMaterialDictionary gameMaterialDictionary;
	private GameContext gameContext;

	@Inject
	public FurnitureEntityMessageHandler(MessageDispatcher messageDispatcher, FurnitureTypeDictionary furnitureTypeDictionary,
										 FurnitureEntityFactory furnitureEntityFactory,
										 ItemTypeDictionary itemTypeDictionary, SoundAssetDictionary soundAssetDictionary,
										 TagProcessor tagProcessor, RoofConstructionManager roofConstructionManager,
										 FurnitureEntityAttributesFactory furnitureEntityAttributesFactory, GameMaterialDictionary gameMaterialDictionary) {
		this.messageDispatcher = messageDispatcher;
		this.furnitureTypeDictionary = furnitureTypeDictionary;
		this.furnitureEntityFactory = furnitureEntityFactory;
		this.itemTypeDictionary = itemTypeDictionary;
		this.tagProcessor = tagProcessor;
		this.roofConstructionManager = roofConstructionManager;
		this.furnitureEntityAttributesFactory = furnitureEntityAttributesFactory;
		this.gameMaterialDictionary = gameMaterialDictionary;

		// FIXME this is also duplicated in ConstructionMessageHandler
		completionSoundMapping.put(GameMaterialType.WOOD, soundAssetDictionary.getByName("PaletteConstruct")); // MODDING Expose this
		completionSoundMapping.put(GameMaterialType.STONE, soundAssetDictionary.getByName("HeavyStoneItem")); // MODDING Expose this

		messageDispatcher.addListener(this, MessageType.LOOKUP_FURNITURE_TYPE);
		messageDispatcher.addListener(this, MessageType.FURNITURE_ATTRIBUTES_CREATION_REQUEST);
		messageDispatcher.addListener(this, MessageType.FURNITURE_CREATION_REQUEST);
	}

	@Override
	public boolean handleMessage(Telegram msg) {
		switch (msg.message) {
			case MessageType.LOOKUP_FURNITURE_TYPE -> {
				handleLookupFurniture((LookupFurnitureMessage) msg.extraInfo);
				return true;
			}
			case MessageType.FURNITURE_ATTRIBUTES_CREATION_REQUEST -> {
				handleCreateFurnitureAttributes((FurnitureAttributesCreationRequestMessage) msg.extraInfo);
				return true;
			}
			case MessageType.FURNITURE_CREATION_REQUEST -> {
				handleCreateFurniture((FurnitureCreationRequestMessage) msg.extraInfo);
				return true;
			}
			default ->
					throw new IllegalArgumentException("Unexpected message type " + msg.message + " received by " + getClass().getSimpleName() + ", " + msg);
		}
	}

	private void handleLookupFurniture(LookupFurnitureMessage lookupFurnitureMessage) {
		lookupFurnitureMessage.callback.accept(furnitureTypeDictionary.getByName(lookupFurnitureMessage.furnitureTypeName));
	}

	private void handleCreateFurnitureAttributes(FurnitureAttributesCreationRequestMessage message) {
		FurnitureEntityAttributes attributes = furnitureEntityAttributesFactory.byType(message.furnitureType, randomMaterial(message.gameMaterialType));
		message.callback.accept(attributes);
	}

	private GameMaterial randomMaterial(GameMaterialType materialType) {
		List<GameMaterial> materialsToPickFrom = gameMaterialDictionary.getByType(materialType).stream()
				.filter(GameMaterial::isUseInRandomGeneration).toList();
		return materialsToPickFrom.get(gameContext.getRandom().nextInt(materialsToPickFrom.size()));
	}

	private void handleCreateFurniture(FurnitureCreationRequestMessage message) {
		copyMaterials(message.furnitureAttributes, message.inputItems);

		Entity createdFurnitureEntity = furnitureEntityFactory.create(message.furnitureAttributes, message.primarylocation, new FurnitureBehaviour(), gameContext);
		FurnitureType furnitureType = message.furnitureAttributes.getFurnitureType();

		ConstructedEntityComponent constructedEntityComponent = new ConstructedEntityComponent();
		constructedEntityComponent.init(createdFurnitureEntity, messageDispatcher, gameContext);
		constructedEntityComponent.setAutoConstructed(furnitureType.isAutoConstructed());
		createdFurnitureEntity.addComponent(constructedEntityComponent);

		createdFurnitureEntity.getBehaviourComponent().init(createdFurnitureEntity, messageDispatcher, gameContext);

		copyLiquids(createdFurnitureEntity, message.inputItems);

		messageDispatcher.dispatchMessage(MessageType.ENTITY_CREATED, createdFurnitureEntity);
		SoundAsset soundAsset = completionSoundMapping.get(message.furnitureAttributes.getPrimaryMaterialType());
		if (soundAsset != null) {
			messageDispatcher.dispatchMessage(MessageType.REQUEST_SOUND, new RequestSoundMessage(soundAsset, createdFurnitureEntity));
		}

		// Fudge to update attached lightsource position
		createdFurnitureEntity.getLocationComponent().setWorldPosition(toVector(message.primarylocation), false);

		tagProcessor.apply(createdFurnitureEntity.getTags(), createdFurnitureEntity);
		DecorationFromInputTag decorationFromInputTag = createdFurnitureEntity.getTag(DecorationFromInputTag.class);
		if (decorationFromInputTag != null) {
			decorationFromInputTag.apply(createdFurnitureEntity, message.inputItems, itemTypeDictionary,
					messageDispatcher, gameContext);
		}
		RequirementToColorMappingsTag requirementToColorMappingsTag = createdFurnitureEntity.getTag(RequirementToColorMappingsTag.class);
		if (requirementToColorMappingsTag != null) {
			requirementToColorMappingsTag.apply(createdFurnitureEntity, message.inputItems, itemTypeDictionary);
		}
		PoweredFurnitureComponent poweredFurnitureComponent = createdFurnitureEntity.getComponent(PoweredFurnitureComponent.class);
		if (poweredFurnitureComponent != null) {
			poweredFurnitureComponent.updatePowerGridAtParentLocation();
		}
		if (createdFurnitureEntity.getTag(SupportsRoofTag.class) != null) {
			for (GridPoint2 tileLocation : message.tileLocations) {
				MapTile tileAtLocation = gameContext.getAreaMap().getTile(tileLocation);
				if (tileAtLocation != null) {
					roofConstructionManager.supportConstructed(tileAtLocation);
				}
			}
		}

		message.callback.accept(createdFurnitureEntity);
	}

	private void copyMaterials(FurnitureEntityAttributes createdAttributes, Map<Long, Entity> itemsRemovedFromConstruction) {
		List<GameMaterial> materialsToApply = new LinkedList<>();
		materialsToApply.addAll(getAllMaterials(itemsRemovedFromConstruction));
		// Then override with most common type
		for (GameMaterialType gameMaterialType : createdAttributes.getMaterials().keySet()) {
			GameMaterial applicableMaterialFromConstruction = findApplicableMaterial(itemsRemovedFromConstruction.values(), gameMaterialType);
			if (applicableMaterialFromConstruction != null) {
				materialsToApply.add(applicableMaterialFromConstruction);
			}
		}

		for (GameMaterial gameMaterial : materialsToApply) {
			createdAttributes.setMaterial(gameMaterial, false);
		}

		// Also copy over other colors
		for (Entity itemRemoved : itemsRemovedFromConstruction.values()) {
			ItemEntityAttributes attributes = (ItemEntityAttributes) itemRemoved.getPhysicalEntityComponent().getAttributes();

			for (ColoringLayer coloringLayer : otherColorsToCopy) {
				createdAttributes.setColor(coloringLayer, attributes.getColor(coloringLayer));
			}
		}

	}

	public static final List<ColoringLayer> otherColorsToCopy = Arrays.asList(ColoringLayer.BRANCHES_COLOR);

	private void copyLiquids(Entity createdFurnitureEntity, Map<Long, Entity> itemsRemovedFromConstruction) {
		LiquidContainerComponent furnitureLiquidContainer = createdFurnitureEntity.getComponent(LiquidContainerComponent.class);
		for (Entity entity : itemsRemovedFromConstruction.values()) {
			LiquidContainerComponent itemLiquidContainer = entity.getComponent(LiquidContainerComponent.class);
			if (itemLiquidContainer != null && itemLiquidContainer.getLiquidQuantity() > 0) {
				furnitureLiquidContainer.setTargetLiquidMaterial(itemLiquidContainer.getTargetLiquidMaterial());
				furnitureLiquidContainer.setLiquidQuantity(itemLiquidContainer.getLiquidQuantity());
				break;
			}
		}
	}

	private Entity callbackItem;

	private Entity createAttachedItem(ItemType requiredItemType) {
		callbackItem = null;
		messageDispatcher.dispatchMessage(MessageType.ITEM_CREATION_REQUEST, new ItemCreationRequestMessage(requiredItemType, false, item -> {
			callbackItem = item;
		}));
		if (callbackItem != null) {
			messageDispatcher.dispatchMessage(MessageType.ENTITY_DO_NOT_TRACK, callbackItem);
		}
		return callbackItem;
	}

	/**
	 * This method picks out the majority material for the supplied type in the furniture's construction
	 */
	private List<GameMaterial> getAllMaterials(Map<Long, Entity> entitiesRemovedFromConstruction) {
		List<GameMaterial> allInputMaterials = new ArrayList<>();
		for (Entity entity : entitiesRemovedFromConstruction.values()) {
			allInputMaterials.addAll(((ItemEntityAttributes)entity.getPhysicalEntityComponent().getAttributes()).getAllMaterials());
		}
		return allInputMaterials;
	}

	@Override
	public void onContextChange(GameContext gameContext) {
		this.gameContext = gameContext;
	}

	@Override
	public void clearContextRelatedState() {

	}
}
