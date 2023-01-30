package technology.rocketjump.saul.assets.editor.widgets.vieweditor;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.saul.assets.model.FloorType;
import technology.rocketjump.saul.entities.components.AttachedEntitiesComponent;
import technology.rocketjump.saul.entities.components.Faction;
import technology.rocketjump.saul.entities.components.InventoryComponent;
import technology.rocketjump.saul.entities.components.furniture.DecorationInventoryComponent;
import technology.rocketjump.saul.entities.factories.CreatureEntityAttributesFactory;
import technology.rocketjump.saul.entities.factories.CreatureEntityFactory;
import technology.rocketjump.saul.entities.factories.ItemEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.AttachedEntity;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.saul.entities.model.physical.item.ItemHoldPosition;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.vehicle.VehicleEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.vehicle.VehicleType;
import technology.rocketjump.saul.environment.GameClock;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.mapping.model.TiledMap;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;

import java.util.*;

@Singleton
public class VehicleAttributesPane extends AbstractAttributesPane {
	private static final ItemType NULL_ITEM_TYPE = new ItemType();
	private static final Race NULL_RACE = new Race();

	static {
		NULL_ITEM_TYPE.setItemTypeName("-none-");
		NULL_RACE.setName("-none-");
	}

	private final GameMaterialDictionary materialDictionary;
	private final GameContext fakeContext;
	private final ItemEntityFactory itemEntityFactory;
	private final ItemTypeDictionary itemTypeDictionary;
	private final RaceDictionary raceDictionary;
	private final CreatureEntityAttributesFactory creatureEntityAttributesFactory;
	private final CreatureEntityFactory creatureEntityFactory;
	private final GameClock gameClock;
	private final Deque<Entity> inventoryEntities = new LinkedList<>();

	@Inject
	public VehicleAttributesPane(EditorStateProvider editorStateProvider, MessageDispatcher messageDispatcher,
								 GameMaterialDictionary materialDictionary, ItemEntityFactory itemEntityFactory,
								 ItemTypeDictionary itemTypeDictionary, RaceDictionary raceDictionary,
								 CreatureEntityAttributesFactory creatureEntityAttributesFactory, CreatureEntityFactory creatureEntityFactory) {
		super(editorStateProvider, messageDispatcher);
		this.materialDictionary = materialDictionary;
		this.itemEntityFactory = itemEntityFactory;
		this.itemTypeDictionary = itemTypeDictionary;
		this.raceDictionary = raceDictionary;
		this.creatureEntityAttributesFactory = creatureEntityAttributesFactory;
		this.creatureEntityFactory = creatureEntityFactory;
		this.gameClock = new GameClock();
		this.fakeContext = new GameContext();
		fakeContext.setAreaMap(new TiledMap(1, 1, 1, FloorType.NULL_FLOOR, GameMaterial.NULL_MATERIAL));
		fakeContext.setGameClock(gameClock);
		fakeContext.setRandom(new RandomXS128());
	}


	public void reload() {
		this.clearChildren();

		Entity currentEntity = editorStateProvider.getState().getCurrentEntity();
		VehicleEntityAttributes attributes = (VehicleEntityAttributes) currentEntity.getPhysicalEntityComponent().getAttributes();

		VehicleType vehicleType = attributes.getVehicleType();

		List<ItemType> itemTypes = itemTypeDictionary.getAll();

		InventoryComponent inventoryComponent = currentEntity.getOrCreateComponent(InventoryComponent.class);
		inventoryComponent.setAddAsAllocationPurpose(null);
		ItemType[] workspaceItemTypes = new ItemType[3];
		Arrays.fill(workspaceItemTypes, NULL_ITEM_TYPE);
		for (int i = 0; i < workspaceItemTypes.length; i++) {
			final int finalIndex = i; //Rocky - Maybe its Friday, or maybe i dislike Java's weird behaviour with lambdas
			add(WidgetBuilder.selectField(ItemHoldPosition.FURNITURE_WORKSPACES.get(i).name(), null, itemTypes, NULL_ITEM_TYPE, update(itemType -> {
				setInventoryItems(inventoryComponent, workspaceItemTypes, itemType, finalIndex);
			})));
		}


		List<Race> sapientRaces = raceDictionary.getAll().stream()
				.filter(r -> r.getBehaviour().getIsSapient())
				.toList();
		List<Race> nonSapientRaces = raceDictionary.getAll().stream()
				.filter(r -> !r.getBehaviour().getIsSapient())
				.toList();

		add(WidgetBuilder.selectField(ItemHoldPosition.VEHICLE_DRIVER.name(), null, sapientRaces, NULL_RACE, update(race -> {
			AttachedEntitiesComponent attachedEntitiesComponent = currentEntity.getComponent(AttachedEntitiesComponent.class);
			if (attachedEntitiesComponent == null) {
				attachedEntitiesComponent = new AttachedEntitiesComponent();
				attachedEntitiesComponent.init(currentEntity, messageDispatcher, fakeContext);
				currentEntity.addComponent(attachedEntitiesComponent);
			}

			Optional<AttachedEntity> alreadyAttached = attachedEntitiesComponent.getAttachedEntities().stream()
					.filter(a -> a.holdPosition.equals(ItemHoldPosition.VEHICLE_DRIVER))
					.findAny();
			if (alreadyAttached.isPresent()) {
				attachedEntitiesComponent.remove(alreadyAttached.get().entity);
			}

			if (race != null && !NULL_RACE.equals(race)) {
				CreatureEntityAttributes creatureAttributes = creatureEntityAttributesFactory.create(race);
				Entity creature = creatureEntityFactory.create(creatureAttributes, new Vector2(), EntityAssetOrientation.DOWN.toVector2(), fakeContext, Faction.SETTLEMENT);
				attachedEntitiesComponent.addAttachedEntity(creature, ItemHoldPosition.VEHICLE_DRIVER);
			}
		})));
		add(WidgetBuilder.selectField(ItemHoldPosition.VEHICLE_DRAUGHT_ANIMAL.name(), null, nonSapientRaces, NULL_RACE, update(race -> {
			AttachedEntitiesComponent attachedEntitiesComponent = currentEntity.getComponent(AttachedEntitiesComponent.class);
			if (attachedEntitiesComponent == null) {
				attachedEntitiesComponent = new AttachedEntitiesComponent();
				attachedEntitiesComponent.init(currentEntity, messageDispatcher, fakeContext);
				currentEntity.addComponent(attachedEntitiesComponent);
			}

			Optional<AttachedEntity> alreadyAttached = attachedEntitiesComponent.getAttachedEntities().stream()
					.filter(a -> a.holdPosition.equals(ItemHoldPosition.VEHICLE_DRAUGHT_ANIMAL))
					.findAny();
			if (alreadyAttached.isPresent()) {
				attachedEntitiesComponent.remove(alreadyAttached.get().entity);
			}

			if (race != null && !NULL_RACE.equals(race)) {
				CreatureEntityAttributes creatureAttributes = creatureEntityAttributesFactory.create(race);
				Entity creature = creatureEntityFactory.create(creatureAttributes, new Vector2(), EntityAssetOrientation.DOWN.toVector2(), fakeContext, Faction.SETTLEMENT);
				attachedEntitiesComponent.addAttachedEntity(creature, ItemHoldPosition.VEHICLE_DRAUGHT_ANIMAL);
			}
		})));
	}

	private void setDecorations(DecorationInventoryComponent decorationComponent, ItemType[] decorations, ItemType itemType, int index) {
		decorations[index] = itemType;
		decorationComponent.clear();
		for (ItemType decoration : decorations) {
			if (NULL_ITEM_TYPE != decoration) {
				decorationComponent.add(itemEntityFactory.createByItemType(decoration, fakeContext, false));
			}
		}
	}

	private void setInventoryItems(InventoryComponent inventoryComponent, ItemType[] inventoryItems, ItemType itemType, int index) {
		inventoryItems[index] = itemType;
		while (!inventoryEntities.isEmpty()) {
			Entity popped = inventoryEntities.pop();
			inventoryComponent.remove(popped.getId());
		}

		for (ItemType decoration : inventoryItems) {
			if (NULL_ITEM_TYPE != decoration) {
				Entity inventoryEntity = itemEntityFactory.createByItemType(decoration, fakeContext, false);
				inventoryEntities.push(inventoryEntity);
				inventoryComponent.add(inventoryEntity, editorStateProvider.getState().getCurrentEntity(), messageDispatcher, gameClock);
			}
		}
	}

}
