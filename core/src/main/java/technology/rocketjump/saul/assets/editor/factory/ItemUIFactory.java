package technology.rocketjump.saul.assets.editor.factory;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextField;
import technology.rocketjump.saul.assets.editor.UniqueAssetNameValidator;
import technology.rocketjump.saul.assets.editor.message.ShowCreateAssetDialogMessage;
import technology.rocketjump.saul.assets.editor.model.ItemNameBuilders;
import technology.rocketjump.saul.assets.editor.widgets.OkCancelDialog;
import technology.rocketjump.saul.assets.editor.widgets.entitybrowser.EntityBrowserValue;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.assets.editor.widgets.vieweditor.ItemAttributesPane;
import technology.rocketjump.saul.assets.entities.CompleteAssetDictionary;
import technology.rocketjump.saul.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.saul.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.saul.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.saul.assets.entities.item.model.ItemSize;
import technology.rocketjump.saul.assets.entities.item.model.ItemStyle;
import technology.rocketjump.saul.assets.entities.model.EntityAsset;
import technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.entities.factories.ItemEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.item.ItemEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.item.ItemQuality;
import technology.rocketjump.saul.entities.model.physical.item.ItemType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.persistence.FileUtils;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

import static technology.rocketjump.saul.assets.entities.item.model.ItemPlacement.BEING_CARRIED;
import static technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation.*;

@Singleton
public class ItemUIFactory implements UIFactory {
    private final MessageDispatcher messageDispatcher;
    private final ItemEntityFactory itemEntityFactory;
    private final ItemTypeDictionary itemTypeDictionary;
    private final ItemAttributesPane itemAttributesPane;

    private final EntityAssetTypeDictionary entityAssetTypeDictionary;
    private final CompleteAssetDictionary completeAssetDictionary;

    @Inject
    public ItemUIFactory(MessageDispatcher messageDispatcher, ItemEntityFactory itemEntityFactory, ItemTypeDictionary itemTypeDictionary,
                         ItemAttributesPane itemAttributesPane, EntityAssetTypeDictionary entityAssetTypeDictionary,
                         CompleteAssetDictionary completeAssetDictionary) {
        this.messageDispatcher = messageDispatcher;
        this.itemEntityFactory = itemEntityFactory;
        this.itemTypeDictionary = itemTypeDictionary;
        this.itemAttributesPane = itemAttributesPane;
        this.entityAssetTypeDictionary = entityAssetTypeDictionary;
        this.completeAssetDictionary = completeAssetDictionary;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ITEM;
    }

    @Override
    public List<EntityAssetOrientation> getApplicableOrientations(EntityAsset entityAsset) {
        List<ItemPlacement> itemPlacements = ((ItemEntityAsset) entityAsset).getItemPlacements();
        if (itemPlacements.isEmpty() || itemPlacements.contains(BEING_CARRIED)) {
            return List.of(DOWN, DOWN_LEFT, DOWN_RIGHT, UP_LEFT, UP_RIGHT, UP);
        } else {
            return List.of(DOWN);
        }
    }

    @Override
    public Entity createEntityForRendering(String name) {
        Random random = new Random();
        GameContext gameContext = new GameContext();
        gameContext.setRandom(new RandomXS128());
        ItemType itemType = itemTypeDictionary.getByName(name);
        ItemEntityAttributes attributes = new ItemEntityAttributes();
        attributes.setItemType(itemType);
        attributes.setSeed(random.nextLong());
        attributes.setQuantity(1);
        return itemEntityFactory.create(attributes, new GridPoint2(), true, gameContext);
    }

    @Override
    public VisTable getViewEditorControls() {
        itemAttributesPane.reload();
        return itemAttributesPane;
    }

    @Override
    public OkCancelDialog createEntityDialog(Path path) {
        ItemType itemType = new ItemType();
        OkCancelDialog dialog = new OkCancelDialog("Create new " + getEntityType()) {
            @Override
            public void onOk() {

            }
        };

        return dialog;
    }

    @Override
    public VisTable getEntityPropertyControls(Object typeDescriptor, Path basePath) {
        ItemType itemType = (ItemType) typeDescriptor;
//        private long itemTypeId;
//        private String itemTypeName;
//
//        private int maxStackSize = 1;
//        private int maxHauledAtOnce; // or requiresHauling
//        private List<GameMaterialType> materialTypes = new ArrayList<>();
//        private GameMaterialType primaryMaterialType;
//
//        private ItemHoldPosition holdPosition = ItemHoldPosition.IN_FRONT;
//        private boolean impedesMovement = false;
//        private boolean blocksMovement = false;
//        private boolean equippedWhileWorkingOnJob = true; // Might need replacing with "can be shown hauling" property
//        private double hoursInInventoryUntilUnused = DEFAULT_HOURS_FOR_ITEM_TO_BECOME_UNUSED;
//
//        private List<String> relatedCraftingTypeNames = new ArrayList<>();
//        private List<CraftingType> relatedCraftingTypes = new ArrayList<>();
//
//        private String stockpileGroupName;
//        private StockpileGroup stockpileGroup;
//
//        private Map<String, List<String>> tags = new HashMap<>();
//        private List<Tag> processedTags = new ArrayList<>();
//
//        private String placementSoundAssetName;
//        private SoundAsset placementSoundAsset;
//
//        private String consumeSoundAssetName;
//        private SoundAsset consumeSoundAsset;
//
//        private WeaponInfo weaponInfo;
//        private AmmoType isAmmoType;
//
//        private boolean describeAsMaterialOnly;
        return new VisTable();
    }

    @Override
    public OkCancelDialog createAssetDialog(ShowCreateAssetDialogMessage message) {
        //TODO: quite a bit of duplication between here and the editor
        final Path directory = FileUtils.getDirectory(message.path()); //duplicated from CreatureUI
        Path descriptorsFile = directory.resolve("descriptors.json");
        ItemType itemType = (ItemType) message.typeDescriptor();

        ItemEntityAsset itemEntityAsset = new ItemEntityAsset();
        OkCancelDialog dialog = new OkCancelDialog("Create asset under " + directory) {
            @Override
            public void onOk() {
                completeAssetDictionary.add(itemEntityAsset);
                EntityBrowserValue value = EntityBrowserValue.forAsset(getEntityType(), descriptorsFile, itemEntityAsset, itemType);
                messageDispatcher.dispatchMessage(MessageType.EDITOR_ASSET_CREATED, value);
                messageDispatcher.dispatchMessage(MessageType.EDITOR_BROWSER_TREE_SELECTION, value);
            }
        };

        //todo: nicer pattern for name building
        Collection<EntityAssetType> entityAssetTypes = entityAssetTypeDictionary.getByEntityType(getEntityType());
        Collection<String> itemTypeNames = itemTypeDictionary.getAll().stream().map(ItemType::getItemTypeName).toList();
        Collection<ItemPlacement> itemPlacements = Arrays.asList(ItemPlacement.values());

        VisTable nameComponent = WidgetBuilder.textField("Name", itemEntityAsset.getUniqueName(), itemEntityAsset::setUniqueName, new UniqueAssetNameValidator(completeAssetDictionary));
        Optional<VisTextField> optionalNameField = WidgetBuilder.findFirst(nameComponent, VisTextField.class);
        Consumer<Object> uniqueNameRebuilder = o -> {
            ItemType selectedItemType = itemTypeDictionary.getByName(itemEntityAsset.getItemTypeName());//Bit weird with the item type thing
            String builtName = ItemNameBuilders.buildUniqueNameForAsset(selectedItemType, itemEntityAsset);
            optionalNameField.ifPresent(textBox -> textBox.setText(builtName));
        };

        dialog.addRow(WidgetBuilder.selectField("Item Type", itemEntityAsset.getItemTypeName(), itemTypeNames, itemType.getItemTypeName(), compose(itemEntityAsset::setItemTypeName, uniqueNameRebuilder)));
        dialog.addRow(WidgetBuilder.selectField("Type", itemEntityAsset.getType(), entityAssetTypes, null, compose(itemEntityAsset::setType, uniqueNameRebuilder)));
        dialog.addRow(WidgetBuilder.checkboxGroup("Placements", itemEntityAsset.getItemPlacements(), itemPlacements,
                compose(itemEntityAsset.getItemPlacements()::add, uniqueNameRebuilder), compose(itemEntityAsset.getItemPlacements()::remove, uniqueNameRebuilder)));
        dialog.addRow(nameComponent);
        return dialog;
    }

    @Override
    public VisTable getAssetPropertyControls(EntityAsset entityAsset) {
        ItemEntityAsset itemEntityAsset = (ItemEntityAsset) entityAsset;

        Collection<EntityAssetType> entityAssetTypes = entityAssetTypeDictionary.getByEntityType(getEntityType());
        Collection<String> itemTypeNames = itemTypeDictionary.getAll().stream().map(ItemType::getItemTypeName).toList();
        Collection<ItemSize> itemSizes = Arrays.asList(ItemSize.values());
        Collection<ItemStyle> itemStyles = Arrays.asList(ItemStyle.values());
        Collection<ItemPlacement> itemPlacements = Arrays.asList(ItemPlacement.values());
        Collection<ItemQuality> itemQualities = Arrays.asList(ItemQuality.values());

        var assetComponents = new VisTable() {
            private void addComponent(VisTable component) {
                Actor[] actors = component.getChildren().toArray();
                for (Actor actor : actors) {
                    this.add(actor);
                }
                this.row();
            }
        };
        assetComponents.defaults().left();
        assetComponents.columnDefaults(0).uniformX().left();
        assetComponents.columnDefaults(1).fillX();
        assetComponents.addComponent(WidgetBuilder.textField("Name", itemEntityAsset.getUniqueName(), itemEntityAsset::setUniqueName, new UniqueAssetNameValidator(completeAssetDictionary)));
        assetComponents.addComponent(WidgetBuilder.selectField("Type", itemEntityAsset.getType(), entityAssetTypes, null, itemEntityAsset::setType));
        assetComponents.addComponent(WidgetBuilder.selectField("Item Type", itemEntityAsset.getItemTypeName(), itemTypeNames, null, itemEntityAsset::setItemTypeName)); //TODO: consider fixing to entity selection
        assetComponents.addComponent(WidgetBuilder.intSpinner("Min Quantity", itemEntityAsset.getMinQuantity(), 1, Integer.MAX_VALUE, itemEntityAsset::setMinQuantity));
        assetComponents.addComponent(WidgetBuilder.intSpinner("Max Quantity", itemEntityAsset.getMaxQuantity(), 1, Integer.MAX_VALUE, itemEntityAsset::setMaxQuantity));
        assetComponents.row().padTop(15);
        assetComponents.addComponent(WidgetBuilder.checkboxGroup("Qualities", itemEntityAsset.getItemQualities(), itemQualities, itemEntityAsset.getItemQualities()::add, itemEntityAsset.getItemQualities()::remove));
        assetComponents.row().padTop(15);
        assetComponents.addComponent(WidgetBuilder.checkboxGroup("Placements", itemEntityAsset.getItemPlacements(), itemPlacements, itemEntityAsset.getItemPlacements()::add, itemEntityAsset.getItemPlacements()::remove));
        assetComponents.row().padTop(15);
        assetComponents.addComponent(WidgetBuilder.selectField("Size", itemEntityAsset.getItemSize(), itemSizes, null, itemEntityAsset::setItemSize));
        assetComponents.addComponent(WidgetBuilder.selectField("Style", itemEntityAsset.getItemStyle(), itemStyles, null, itemEntityAsset::setItemStyle));

        VisTable parentTable = new VisTable();
        parentTable.add(assetComponents).left().row();
        return parentTable;
    }
}
