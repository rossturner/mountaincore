package technology.rocketjump.saul.assets.editor.factory;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.widget.*;
import org.apache.commons.lang3.StringUtils;
import technology.rocketjump.saul.assets.editor.UniqueAssetNameValidator;
import technology.rocketjump.saul.assets.editor.message.ShowCreateAssetDialogMessage;
import technology.rocketjump.saul.assets.editor.model.EditorEntitySelection;
import technology.rocketjump.saul.assets.editor.model.ItemNameBuilders;
import technology.rocketjump.saul.assets.editor.widgets.OkCancelDialog;
import technology.rocketjump.saul.assets.editor.widgets.entitybrowser.EntityBrowserValue;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.TagsWidget;
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
import technology.rocketjump.saul.assets.model.FloorType;
import technology.rocketjump.saul.audio.model.SoundAsset;
import technology.rocketjump.saul.audio.model.SoundAssetDictionary;
import technology.rocketjump.saul.entities.factories.ItemEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.combat.CombatDamageType;
import technology.rocketjump.saul.entities.model.physical.item.*;
import technology.rocketjump.saul.environment.GameClock;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.CraftingTypeDictionary;
import technology.rocketjump.saul.jobs.model.CraftingType;
import technology.rocketjump.saul.mapping.model.TiledMap;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.materials.model.GameMaterialType;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.persistence.FileUtils;
import technology.rocketjump.saul.rooms.StockpileGroup;
import technology.rocketjump.saul.rooms.StockpileGroupDictionary;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

import static technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder.orderedArray;
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
    private final CraftingTypeDictionary craftingTypeDictionary;
    private final StockpileGroupDictionary stockpileGroupDictionary;
    private final SoundAssetDictionary soundAssetDictionary;

    @Inject
    public ItemUIFactory(MessageDispatcher messageDispatcher, ItemEntityFactory itemEntityFactory, ItemTypeDictionary itemTypeDictionary,
                         ItemAttributesPane itemAttributesPane, EntityAssetTypeDictionary entityAssetTypeDictionary,
                         CompleteAssetDictionary completeAssetDictionary, CraftingTypeDictionary craftingTypeDictionary,
                         StockpileGroupDictionary stockpileGroupDictionary, SoundAssetDictionary soundAssetDictionary) {
        this.messageDispatcher = messageDispatcher;
        this.itemEntityFactory = itemEntityFactory;
        this.itemTypeDictionary = itemTypeDictionary;
        this.itemAttributesPane = itemAttributesPane;
        this.entityAssetTypeDictionary = entityAssetTypeDictionary;
        this.completeAssetDictionary = completeAssetDictionary;
        this.craftingTypeDictionary = craftingTypeDictionary;
        this.stockpileGroupDictionary = stockpileGroupDictionary;
        this.soundAssetDictionary = soundAssetDictionary;
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
        gameContext.setAreaMap(new TiledMap(1, 1, 1, FloorType.NULL_FLOOR, GameMaterial.NULL_MATERIAL));
        gameContext.setGameClock(new GameClock());
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
                String itemTypeName = itemType.getItemTypeName();
                itemType.setPrimaryMaterialType(GameMaterialType.STONE);
                String folderName = itemTypeName.toLowerCase(Locale.ROOT);
                Path basePath = FileUtils.createDirectory(path, folderName);

                itemTypeDictionary.add(itemType);
                completeAssetDictionary.rebuild();

                EditorEntitySelection editorEntitySelection = new EditorEntitySelection();
                editorEntitySelection.setEntityType(getEntityType());
                editorEntitySelection.setTypeName(itemTypeName);
                editorEntitySelection.setBasePath(basePath.toString());
                messageDispatcher.dispatchMessage(MessageType.EDITOR_ENTITY_SELECTION, editorEntitySelection);
                messageDispatcher.dispatchMessage(MessageType.EDITOR_BROWSER_TREE_SELECTION, EntityBrowserValue.forTypeDescriptor(getEntityType(), basePath, itemType));
            }
        };
        dialog.add(WidgetBuilder.label("Name"));
        InputValidator nonBlank = StringUtils::isNotBlank;
        InputValidator uniqueName = input -> itemTypeDictionary.getByName(input) == null;
        dialog.add(WidgetBuilder.textField(null, itemType::setItemTypeName, nonBlank, uniqueName));

        return dialog;
    }

    @Override
    public VisTable getEntityPropertyControls(Object typeDescriptor, Path basePath) {
        ItemType itemType = (ItemType) typeDescriptor;

        VisTable controls = new VisTable();

        controls.defaults().left();
        controls.columnDefaults(0).uniformX().left();
        controls.columnDefaults(1).fillX().left();

        VisValidatableTextField nameTextField = WidgetBuilder.textField(itemType.getItemTypeName(), itemType::setItemTypeName);
        nameTextField.setDisabled(true);
        nameTextField.setTouchable(Touchable.disabled);
        controls.add(WidgetBuilder.label("Name"));
        controls.add(nameTextField); //TODO: make editable and update child entity asset types
        controls.row();

        controls.add(WidgetBuilder.label("Max Stack Size"));
        controls.add(WidgetBuilder.intSpinner(itemType.getMaxStackSize(), 1, Integer.MAX_VALUE, itemType::setMaxStackSize));
        controls.row();

        controls.add(WidgetBuilder.label("Max Hauled At Once"));
        controls.add(WidgetBuilder.intSpinner(itemType.getMaxHauledAtOnce(), 1, Integer.MAX_VALUE, itemType::setMaxHauledAtOnce));
        controls.row();

        controls.add(WidgetBuilder.label("Materials")).padTop(15);
        controls.row();
        controls.addSeparator().colspan(2).expand(false, false).row();

        controls.add(WidgetBuilder.label("Material Only"));
        controls.add(WidgetBuilder.toggle(itemType.isDescribeAsMaterialOnly(), itemType::setDescribeAsMaterialOnly));
        controls.row();


        //Todo: nicer display name
        Map<GameMaterialType, VisCheckBox> materialTypeMap = new HashMap<>();
        for (GameMaterialType materialType : GameMaterialType.values()) {
            VisCheckBox checkBox = WidgetBuilder.checkBox(materialType, itemType.getMaterialTypes().contains(materialType),
                    it -> {
                        if (!itemType.getMaterialTypes().contains(it)) {
                            itemType.getMaterialTypes().add(it);
                        }
                    }, itemType.getMaterialTypes()::remove);
            materialTypeMap.put(materialType, checkBox);
        }

        controls.add(WidgetBuilder.label("Primary Type"));
        controls.add(WidgetBuilder.select(itemType.getPrimaryMaterialType(), GameMaterialType.values(), null, newMaterialType -> {
            GameMaterialType oldPrimary = itemType.getPrimaryMaterialType();
            materialTypeMap.get(oldPrimary).setDisabled(false);
            materialTypeMap.get(oldPrimary).setTouchable(Touchable.enabled);
            materialTypeMap.get(oldPrimary).setChecked(false);

            itemType.setPrimaryMaterialType(newMaterialType);
            itemType.getMaterialTypes().remove(oldPrimary);
            itemType.getMaterialTypes().add(newMaterialType);

            materialTypeMap.get(newMaterialType).setDisabled(true);
            materialTypeMap.get(newMaterialType).setTouchable(Touchable.disabled);
            materialTypeMap.get(newMaterialType).setChecked(true);

        }));
        controls.row();

        int checkboxColCount = 1;
        for (VisCheckBox checkBox : materialTypeMap.values()) {
            controls.add(checkBox).fill(false, false).left();
            if (checkboxColCount % 2 == 0) {
                controls.row();
            }
            checkboxColCount++;
        }
        controls.row();

        controls.addSeparator().colspan(2).padBottom(15).expand(false, false).row();

        controls.add(WidgetBuilder.label("Hold Position"));
        controls.add(WidgetBuilder.select(itemType.getHoldPosition(), ItemHoldPosition.values(), null, itemType::setHoldPosition));
        controls.row();

        controls.add(WidgetBuilder.label("Impedes Movement"));
        controls.add(WidgetBuilder.toggle(itemType.impedesMovement(), itemType::setImpedesMovement));
        controls.row();

        controls.add(WidgetBuilder.label("Blocks Movement"));
        controls.add(WidgetBuilder.toggle(itemType.blocksMovement(), itemType::setBlocksMovement));
        controls.row();

        controls.add(WidgetBuilder.label("Equipped While Working"));
        controls.add(WidgetBuilder.toggle(itemType.isEquippedWhileWorkingOnJob(), itemType::setEquippedWhileWorkingOnJob));
        controls.row();

        controls.add(WidgetBuilder.label("Inventory Until Unused (hours)"));
        controls.add(WidgetBuilder.doubleSpinner(itemType.getHoursInInventoryUntilUnused(), 0, Double.MAX_VALUE, itemType::setHoursInInventoryUntilUnused));
        controls.row();

        controls.add(WidgetBuilder.label("Crafting")).padTop(15);
        controls.row();
        controls.addSeparator().colspan(2);
        controls.row();

        int craftingCheckboxCount = 1;
        for (CraftingType craftingType : craftingTypeDictionary.getAll().stream().sorted().toList()) {
            VisCheckBox checkBox = WidgetBuilder.checkBox(craftingType, itemType.getRelatedCraftingTypes().contains(craftingType),
                    it -> {
                        if (!itemType.getRelatedCraftingTypes().contains(it)) {
                            itemType.getRelatedCraftingTypes().add(it);
                            itemType.getRelatedCraftingTypeNames().add(it.getName());
                        }
                    },
                    it -> {
                        itemType.getRelatedCraftingTypes().remove(it);
                        itemType.getRelatedCraftingTypeNames().remove(it.getName());
                    });

            controls.add(checkBox).fill(false, false).left();
            if (craftingCheckboxCount % 2 == 0) {
                controls.row();
            }
            craftingCheckboxCount++;
        }

        controls.row();
        controls.addSeparator().colspan(2).padBottom(15);
        controls.row();

        TagsWidget tagsWidget = new TagsWidget(itemType.getTags());
        tagsWidget.setFillParent(true);
        CollapsibleWidget tagsCollapsible = new CollapsibleWidget(tagsWidget);
        tagsCollapsible.setCollapsed(itemType.getTags().isEmpty());
        VisLabel tagsLabel = new VisLabel("Tags (click to show)");
        tagsLabel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                tagsCollapsible.setCollapsed(!tagsCollapsible.isCollapsed());
            }
        });
        controls.add(tagsLabel).left().row();
        controls.add();
        controls.add(tagsCollapsible).right().row();



        StockpileGroup nullStockpileGroup = new StockpileGroup() {
            @Override
            public String getName() {
                return null;
            }

            @Override
            public String toString() {
                return "-none-";
            }
        };
        controls.add(WidgetBuilder.label("Stockpile Group"));
        controls.add(WidgetBuilder.select(itemType.getStockpileGroup(), stockpileGroupDictionary.getAll(), nullStockpileGroup, stockpileGroup -> {
            itemType.setStockpileGroup(stockpileGroup);
            itemType.setStockpileGroupName(stockpileGroup.getName());
        }));
        controls.row();

        SoundAsset nullSoundAsset = new SoundAsset() {
            @Override
            public String getName() {
                return null;
            }

            @Override
            public String toString() {
                return "-none-";
            }
        };
        controls.add(WidgetBuilder.label("Placement Sound"));
        controls.add(WidgetBuilder.select(itemType.getPlacementSoundAsset(), soundAssetDictionary.getAll(), nullSoundAsset, soundAsset -> {
            itemType.setPlacementSoundAsset(soundAsset);
            itemType.setPlacementSoundAssetName(soundAsset.getName());
        }));
        controls.row();
        controls.add(WidgetBuilder.label("Consume Sound"));
        controls.add(WidgetBuilder.select(itemType.getConsumeSoundAsset(), soundAssetDictionary.getAll(), nullSoundAsset, soundAsset -> {
            itemType.setConsumeSoundAsset(soundAsset);
            itemType.setConsumeSoundAssetName(soundAsset.getName());
        }));
        controls.row();

        controls.add(WidgetBuilder.label("Is Ammo Type"));
        controls.add(ammoTypeSelect(itemType.getIsAmmoType(), itemType::setIsAmmoType));
        controls.row();

        // Weapon info
        final WeaponInfo weaponInfo;
        boolean initalHasWeaponInfo = itemType.getWeaponInfo() != null;
        if (initalHasWeaponInfo) {
            weaponInfo = itemType.getWeaponInfo();
        } else {
            weaponInfo = new WeaponInfo();
        }
        VisTable weaponInfoControls = new VisTable();
        weaponInfoControls.columnDefaults(0).uniformX().left();
        weaponInfoControls.columnDefaults(1).fillX().left();

        CollapsibleWidget weaponCollapsible = new CollapsibleWidget(weaponInfoControls);
        weaponCollapsible.setCollapsed(!initalHasWeaponInfo);
        controls.add(WidgetBuilder.checkBox("Is Weapon:", initalHasWeaponInfo, checked -> {
            itemType.setWeaponInfo(weaponInfo);
            weaponCollapsible.setCollapsed(false, true);
        }, unchecked -> {
            itemType.setWeaponInfo(null);
            weaponCollapsible.setCollapsed(true, true);
        })).padTop(15);
        controls.row();
        controls.addSeparator().colspan(2).expand(false, false).row();

        weaponInfoControls.add(WidgetBuilder.label("Modified By Strength"));
        weaponInfoControls.add(WidgetBuilder.toggle(weaponInfo.isModifiedByStrength(), weaponInfo::setModifiedByStrength));
        weaponInfoControls.row();

        weaponInfoControls.add(WidgetBuilder.label("Min Damage"));
        weaponInfoControls.add(WidgetBuilder.intSpinner(weaponInfo.getMinDamage(), 0, Integer.MAX_VALUE, weaponInfo::setMinDamage));
        weaponInfoControls.row();

        weaponInfoControls.add(WidgetBuilder.label("Max Damage"));
        weaponInfoControls.add(WidgetBuilder.intSpinner(weaponInfo.getMaxDamage(), 0, Integer.MAX_VALUE, weaponInfo::setMaxDamage));
        weaponInfoControls.row();

        weaponInfoControls.add(WidgetBuilder.label("Damage Type"));
        weaponInfoControls.add(WidgetBuilder.select(weaponInfo.getDamageType(), CombatDamageType.values(), null, weaponInfo::setDamageType));
        weaponInfoControls.row();

        weaponInfoControls.add(WidgetBuilder.label("Range"));
        weaponInfoControls.add(WidgetBuilder.intSpinner(weaponInfo.getRange(), 1, Integer.MAX_VALUE, weaponInfo::setRange));
        weaponInfoControls.row();

        weaponInfoControls.add(WidgetBuilder.label("Requires Ammo"));
        weaponInfoControls.add(ammoTypeSelect(weaponInfo.getRequiresAmmoType(), weaponInfo::setRequiresAmmoType));
        weaponInfoControls.row();

        weaponInfoControls.add(WidgetBuilder.label("Fire Sound"));
        weaponInfoControls.add(WidgetBuilder.select(weaponInfo.getFireWeaponSoundAsset(), soundAssetDictionary.getAll(), nullSoundAsset, soundAsset -> {
            weaponInfo.setFireWeaponSoundAsset(soundAsset);
            weaponInfo.setFireWeaponSoundAssetName(soundAsset.getName());
        }));
        weaponInfoControls.row();

        weaponInfoControls.add(WidgetBuilder.label("Hit Sound"));
        weaponInfoControls.add(WidgetBuilder.select(weaponInfo.getWeaponHitSoundAsset(), soundAssetDictionary.getAll(), nullSoundAsset, soundAsset -> {
            weaponInfo.setWeaponHitSoundAsset(soundAsset);
            weaponInfo.setWeaponHitSoundAssetName(soundAsset.getName());
        }));
        weaponInfoControls.row();

        weaponInfoControls.add(WidgetBuilder.label("Miss Sound"));
        weaponInfoControls.add(WidgetBuilder.select(weaponInfo.getWeaponMissSoundAsset(), soundAssetDictionary.getAll(), nullSoundAsset, soundAsset -> {
            weaponInfo.setWeaponMissSoundAsset(soundAsset);
            weaponInfo.setWeaponMissSoundAssetName(soundAsset.getName());
        }));
        weaponInfoControls.row();

        controls.add(weaponCollapsible).colspan(2);
        controls.row();
        controls.addSeparator().colspan(2).padBottom(15).expand(false, false).row();

        return controls;
    }

    private VisSelectBox<String> ammoTypeSelect(AmmoType currentValue, Consumer<AmmoType> listener) {
        String nullOption = "-none-";
        VisSelectBox<String> selectBox = new VisSelectBox<>();
        Array<String> items = orderedArray(Arrays.stream(AmmoType.values()).map(AmmoType::name).toList());
        items.insert(0, nullOption);
        selectBox.setItems(items);
        selectBox.setSelected(currentValue == null ? nullOption : currentValue.name());
        selectBox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String selected = selectBox.getSelected();
                if (Objects.equals(selected, nullOption)) {
                    listener.accept(null);
                } else {
                    listener.accept(AmmoType.valueOf(selected));
                }
            }
        });

        return selectBox;
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

        VisTextField nameTextField = WidgetBuilder.textField(itemEntityAsset.getUniqueName(), itemEntityAsset::setUniqueName, new UniqueAssetNameValidator(completeAssetDictionary));
        Consumer<Object> uniqueNameRebuilder = o -> {
            ItemType selectedItemType = itemTypeDictionary.getByName(itemEntityAsset.getItemTypeName());//Bit weird with the item type thing
            String builtName = ItemNameBuilders.buildUniqueNameForAsset(selectedItemType, itemEntityAsset);
            nameTextField.setText(builtName);
        };

        dialog.add(WidgetBuilder.label("Item Type"));
        dialog.add(WidgetBuilder.select(itemEntityAsset.getItemTypeName(), itemTypeNames, itemType.getItemTypeName(), compose(itemEntityAsset::setItemTypeName, uniqueNameRebuilder)));
        dialog.row();
        dialog.add(WidgetBuilder.label("Type"));
        dialog.add(WidgetBuilder.select(itemEntityAsset.getType(), entityAssetTypes, null, compose(itemEntityAsset::setType, uniqueNameRebuilder)));
        dialog.row();
        dialog.add(WidgetBuilder.label("Placements"));
        dialog.add(WidgetBuilder.checkboxes(itemEntityAsset.getItemPlacements(), itemPlacements,
                compose(itemEntityAsset.getItemPlacements()::add, uniqueNameRebuilder), compose(itemEntityAsset.getItemPlacements()::remove, uniqueNameRebuilder))).colspan(2);
        dialog.row();
        dialog.add(WidgetBuilder.label("Name"));
        dialog.add(nameTextField);
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

        var assetComponents = new VisTable() { //TODO: feels dirty to unpack the widgets again
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
