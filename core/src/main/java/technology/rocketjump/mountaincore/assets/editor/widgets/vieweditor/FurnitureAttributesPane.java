package technology.rocketjump.mountaincore.assets.editor.widgets.vieweditor;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.RandomXS128;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import technology.rocketjump.mountaincore.assets.editor.model.EditorStateProvider;
import technology.rocketjump.mountaincore.assets.editor.widgets.ToStringDecorator;
import technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.mountaincore.assets.model.FloorType;
import technology.rocketjump.mountaincore.entities.components.Faction;
import technology.rocketjump.mountaincore.entities.components.InventoryComponent;
import technology.rocketjump.mountaincore.entities.components.furniture.DecorationInventoryComponent;
import technology.rocketjump.mountaincore.entities.factories.ItemEntityFactory;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemHoldPosition;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.mountaincore.environment.GameClock;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.mapping.model.TiledMap;
import technology.rocketjump.mountaincore.materials.GameMaterialDictionary;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;

import java.util.*;

@Singleton
public class FurnitureAttributesPane extends AbstractAttributesPane {
    private static final ItemType NULL_ITEM_TYPE = new ItemType();
    static {
        NULL_ITEM_TYPE.setItemTypeName("-none-");
    }
    private final GameMaterialDictionary materialDictionary;
    private final GameContext fakeContext;
    private final ItemEntityFactory itemEntityFactory;
    private final ItemTypeDictionary itemTypeDictionary;
    private final GameClock gameClock;
    private final Deque<Entity> inventoryEntities = new LinkedList<>();

    @Inject
    public FurnitureAttributesPane(EditorStateProvider editorStateProvider, MessageDispatcher messageDispatcher,
								   GameMaterialDictionary materialDictionary, ItemEntityFactory itemEntityFactory,
								   ItemTypeDictionary itemTypeDictionary) {
        super(editorStateProvider, messageDispatcher);
        this.materialDictionary = materialDictionary;
        this.itemEntityFactory = itemEntityFactory;
        this.itemTypeDictionary = itemTypeDictionary;
        this.gameClock = new GameClock();
        this.fakeContext = new GameContext();
        fakeContext.setAreaMap(new TiledMap(1, 1, 1, FloorType.NULL_FLOOR, GameMaterial.NULL_MATERIAL));
        fakeContext.setGameClock(gameClock);
        fakeContext.setRandom(new RandomXS128());
    }


    public void reload() {
        this.clearChildren();

        Entity currentEntity = editorStateProvider.getState().getCurrentEntity();
        FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) currentEntity.getPhysicalEntityComponent().getAttributes();

        FurnitureType furnitureType = attributes.getFurnitureType();
        Collection<GameMaterialType> materialTypes = furnitureType.getRequirements().keySet();


        List<ToStringDecorator<GameMaterial>> availableMaterials = new ArrayList<>();
        for (GameMaterialType type : materialTypes) {
            materialDictionary.getByType(type)
                    .stream()
                    .map(ToStringDecorator::material)
                    .forEach(availableMaterials::add);
        }

        ToStringDecorator<GameMaterial> initalMaterial = ToStringDecorator.material(attributes.getMaterials().get(attributes.getPrimaryMaterialType()));

        Set<FurnitureLayout> furnitureLayouts = new LinkedHashSet<>();
        FurnitureLayout currentLayout = attributes.getCurrentLayout();
        while (currentLayout != null && !furnitureLayouts.contains(currentLayout)) {
            furnitureLayouts.add(currentLayout);
            currentLayout = currentLayout.getRotatesTo();
        }

        add(WidgetBuilder.selectField("Layout", attributes.getCurrentLayout(), furnitureLayouts, null, update(attributes::setCurrentLayout)));

        add(WidgetBuilder.selectField("Material", initalMaterial, availableMaterials, null, update(decorated -> {
            GameMaterial selectedMaterial = decorated.getObject();
            attributes.setPrimaryMaterialType(selectedMaterial.getMaterialType());
            attributes.setMaterial(selectedMaterial);
        })));
        //seed?

        row();
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

        DecorationInventoryComponent decorationComponent = currentEntity.getOrCreateComponent(DecorationInventoryComponent.class);
        decorationComponent.init(currentEntity, messageDispatcher, fakeContext);
        ItemType[] decorations = new ItemType[3];
        decorations[0] = NULL_ITEM_TYPE;
        decorations[1] = NULL_ITEM_TYPE;
        decorations[2] = NULL_ITEM_TYPE;
        add(WidgetBuilder.selectField(ItemHoldPosition.DECORATION_1.name(), null, itemTypes, NULL_ITEM_TYPE, update(itemType -> {
            setDecorations(decorationComponent, decorations, itemType, 0);
        })));

        add(WidgetBuilder.selectField(ItemHoldPosition.DECORATION_2.name(), null, itemTypes, NULL_ITEM_TYPE, update(itemType -> {
            setDecorations(decorationComponent, decorations, itemType, 1);
        })));

        add(WidgetBuilder.selectField(ItemHoldPosition.DECORATION_3.name(), null, itemTypes, NULL_ITEM_TYPE, update(itemType -> {
            setDecorations(decorationComponent, decorations, itemType, 2);
        })));
    }

    private void setDecorations(DecorationInventoryComponent decorationComponent, ItemType[] decorations, ItemType itemType, int index) {
        decorations[index] = itemType;
        decorationComponent.clear();
        for (ItemType decoration : decorations) {
            if (NULL_ITEM_TYPE != decoration) {
                decorationComponent.add(itemEntityFactory.createByItemType(decoration, fakeContext, false, Faction.SETTLEMENT));
            }
        }
    }

    private void setInventoryItems(InventoryComponent inventoryComponent, ItemType[] inventoryItems, ItemType itemType, int index) {
        inventoryItems[index] = itemType;
        while(!inventoryEntities.isEmpty()) {
            Entity popped = inventoryEntities.pop();
            inventoryComponent.remove(popped.getId());
        }

        for (ItemType decoration : inventoryItems) {
            if (NULL_ITEM_TYPE != decoration) {
                Entity inventoryEntity = itemEntityFactory.createByItemType(decoration, fakeContext, false, Faction.SETTLEMENT);
                inventoryEntities.push(inventoryEntity);
                inventoryComponent.add(inventoryEntity, editorStateProvider.getState().getCurrentEntity(), messageDispatcher, gameClock);
            }
        }
    }

}
