package technology.rocketjump.mountaincore.assets.editor.factory;

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
import technology.rocketjump.mountaincore.assets.editor.UniqueAssetNameValidator;
import technology.rocketjump.mountaincore.assets.editor.model.EditorEntitySelection;
import technology.rocketjump.mountaincore.assets.editor.model.ItemNameBuilders;
import technology.rocketjump.mountaincore.assets.editor.model.ShowCreateAssetDialogMessage;
import technology.rocketjump.mountaincore.assets.editor.widgets.OkCancelDialog;
import technology.rocketjump.mountaincore.assets.editor.widgets.entitybrowser.EntityBrowserValue;
import technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor.DefenseInfoWidget;
import technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor.TagsWidget;
import technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor.WeaponInfoWidget;
import technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor.item.ItemApplicableMaterialsWidget;
import technology.rocketjump.mountaincore.assets.editor.widgets.vieweditor.ItemAttributesPane;
import technology.rocketjump.mountaincore.assets.entities.CompleteAssetDictionary;
import technology.rocketjump.mountaincore.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.mountaincore.assets.entities.item.model.ItemEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.item.model.ItemPlacement;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAsset;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetType;
import technology.rocketjump.mountaincore.assets.model.FloorType;
import technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary;
import technology.rocketjump.mountaincore.entities.components.Faction;
import technology.rocketjump.mountaincore.entities.factories.ItemEntityFactory;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.combat.DefenseInfo;
import technology.rocketjump.mountaincore.entities.model.physical.combat.WeaponInfo;
import technology.rocketjump.mountaincore.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.mountaincore.entities.model.physical.item.*;
import technology.rocketjump.mountaincore.environment.GameClock;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.jobs.CraftingTypeDictionary;
import technology.rocketjump.mountaincore.jobs.SkillDictionary;
import technology.rocketjump.mountaincore.jobs.model.CraftingType;
import technology.rocketjump.mountaincore.mapping.model.TiledMap;
import technology.rocketjump.mountaincore.materials.GameMaterialDictionary;
import technology.rocketjump.mountaincore.materials.model.GameMaterial;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.particles.ParticleEffectTypeDictionary;
import technology.rocketjump.mountaincore.persistence.FileUtils;
import technology.rocketjump.mountaincore.production.StockpileGroup;
import technology.rocketjump.mountaincore.production.StockpileGroupDictionary;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

import static technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor.WidgetBuilder.orderedArray;
import static technology.rocketjump.mountaincore.assets.entities.model.EntityAssetOrientation.*;
import static technology.rocketjump.mountaincore.audio.model.SoundAssetDictionary.NULL_SOUND_ASSET;

@Singleton
public class ItemUIFactory implements UIFactory {
    private final MessageDispatcher messageDispatcher;
    private final ItemEntityFactory itemEntityFactory;
    private final ItemTypeDictionary itemTypeDictionary;
    private final ItemAttributesPane viewEditorControls;

    private final EntityAssetTypeDictionary entityAssetTypeDictionary;
    private final CompleteAssetDictionary completeAssetDictionary;
    private final CraftingTypeDictionary craftingTypeDictionary;
    private final StockpileGroupDictionary stockpileGroupDictionary;
    private final SoundAssetDictionary soundAssetDictionary;
    private final ParticleEffectTypeDictionary particleEffectTypeDictionary;
    private final SkillDictionary skillDictionary;
    private final RaceDictionary raceDictionary;
    private final GameMaterialDictionary materialDictionary;

    @Inject
    public ItemUIFactory(MessageDispatcher messageDispatcher, ItemEntityFactory itemEntityFactory, ItemTypeDictionary itemTypeDictionary,
                         ItemAttributesPane viewEditorControls, EntityAssetTypeDictionary entityAssetTypeDictionary,
                         CompleteAssetDictionary completeAssetDictionary, CraftingTypeDictionary craftingTypeDictionary,
                         StockpileGroupDictionary stockpileGroupDictionary, SoundAssetDictionary soundAssetDictionary,
                         ParticleEffectTypeDictionary particleEffectTypeDictionary, SkillDictionary skillDictionary,
                         RaceDictionary raceDictionary, GameMaterialDictionary materialDictionary) {
        this.messageDispatcher = messageDispatcher;
        this.itemEntityFactory = itemEntityFactory;
        this.itemTypeDictionary = itemTypeDictionary;
        this.viewEditorControls = viewEditorControls;
        this.entityAssetTypeDictionary = entityAssetTypeDictionary;
        this.completeAssetDictionary = completeAssetDictionary;
        this.craftingTypeDictionary = craftingTypeDictionary;
        this.stockpileGroupDictionary = stockpileGroupDictionary;
        this.soundAssetDictionary = soundAssetDictionary;
        this.particleEffectTypeDictionary = particleEffectTypeDictionary;
        this.skillDictionary = skillDictionary;
        this.raceDictionary = raceDictionary;
        this.materialDictionary = materialDictionary;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.ITEM;
    }

    @Override
    public List<EntityAssetOrientation> getApplicableOrientations(EntityAsset entityAsset) {
        List<ItemPlacement> itemPlacements = ((ItemEntityAsset) entityAsset).getItemPlacements();
        if (itemPlacements.isEmpty() || itemPlacements.contains(ItemPlacement.BEING_CARRIED)) {
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
        return itemEntityFactory.create(attributes, new GridPoint2(), true, gameContext, Faction.SETTLEMENT);
    }

    @Override
    public VisTable getViewEditorControls() {
        viewEditorControls.reload();
        return viewEditorControls;
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

        controls.add(WidgetBuilder.label("i18n Key"));
        controls.add(new VisLabel(itemType.getI18nKey()));
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

        controls.add(WidgetBuilder.label("Trade Exportable"));
        controls.add(WidgetBuilder.toggle(itemType.isTradeExportable(), itemType::setTradeExportable));
        controls.row();

        controls.add(WidgetBuilder.label("Trade Importable"));
        controls.add(WidgetBuilder.toggle(itemType.isTradeImportable(), itemType::setTradeImportable));
        controls.row();

        //Todo: nicer display name
        Map<GameMaterialType, VisCheckBox> materialTypeMap = new HashMap<>();
        for (GameMaterialType materialType : GameMaterialType.values()) {
            VisCheckBox checkBox = WidgetBuilder.checkBox(materialType, itemType.getMaterialTypes().contains(materialType),
                    it -> {
                        if (!itemType.getMaterialTypes().contains(it)) {
                            itemType.getMaterialTypes().add(it);
                        }
                        viewEditorControls.reload();
                    },
                    it -> {
                        itemType.getMaterialTypes().remove(it);
                        viewEditorControls.reload();
                    });
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

        controls.add(WidgetBuilder.label("Base Value per Item"));
        controls.add(WidgetBuilder.intSpinner(itemType.getBaseValuePerItem(), 1, Integer.MAX_VALUE, itemType::setBaseValuePerItem));
        controls.row();

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

        controls.add(WidgetBuilder.label("Placement Sound"));
        controls.add(WidgetBuilder.select(itemType.getPlacementSoundAsset(), soundAssetDictionary.getAll(), NULL_SOUND_ASSET, soundAsset -> {
            itemType.setPlacementSoundAsset(soundAsset);
            itemType.setPlacementSoundAssetName(soundAsset.getName());
        }));
        controls.row();
        controls.add(WidgetBuilder.label("Consume Sound"));
        controls.add(WidgetBuilder.select(itemType.getConsumeSoundAsset(), soundAssetDictionary.getAll(), NULL_SOUND_ASSET, soundAsset -> {
            itemType.setConsumeSoundAsset(soundAsset);
            itemType.setConsumeSoundAssetName(soundAsset.getName());
        }));
        controls.row();

        controls.add(WidgetBuilder.label("Is Ammo Type"));
        controls.add(ammoTypeSelect(itemType.getIsAmmoType(), itemType::setIsAmmoType));
        controls.row();

        // Weapon info
        final WeaponInfo weaponInfo;
        boolean initialHasWeaponInfo = itemType.getWeaponInfo() != null;
        if (initialHasWeaponInfo) {
            weaponInfo = itemType.getWeaponInfo();
        } else {
            weaponInfo = new WeaponInfo();
        }
        WeaponInfoWidget weaponInfoControls = new WeaponInfoWidget(weaponInfo, soundAssetDictionary, particleEffectTypeDictionary, skillDictionary);

        CollapsibleWidget weaponCollapsible = new CollapsibleWidget(weaponInfoControls);
        weaponCollapsible.setCollapsed(!initialHasWeaponInfo);
        controls.add(WidgetBuilder.checkBox("Is Weapon:", initialHasWeaponInfo, checked -> {
            itemType.setWeaponInfo(weaponInfo);
            weaponCollapsible.setCollapsed(false, true);
        }, unchecked -> {
            itemType.setWeaponInfo(null);
            weaponCollapsible.setCollapsed(true, true);
        })).padTop(15);
        controls.row();
        controls.addSeparator().colspan(2).expand(false, false).row();

        controls.add(weaponCollapsible).colspan(2);
        controls.row();
        controls.addSeparator().colspan(2).padBottom(15).expand(false, false).row();

        // Defense info
        final DefenseInfo defenseInfo;
        boolean initialHasDefenseInfo = itemType.getDefenseInfo() != null;
        if (initialHasDefenseInfo) {
            defenseInfo = itemType.getDefenseInfo();
        } else {
            defenseInfo = new DefenseInfo();
        }
        DefenseInfoWidget defenseInfoWidget = new DefenseInfoWidget(defenseInfo, raceDictionary);

        CollapsibleWidget defenseCollapsible = new CollapsibleWidget(defenseInfoWidget);
        defenseCollapsible.setCollapsed(!initialHasDefenseInfo);
        controls.add(WidgetBuilder.checkBox("Is Defense:", initialHasDefenseInfo, checked -> {
            itemType.setDefenseInfo(defenseInfo);
            defenseCollapsible.setCollapsed(false, true);
        }, unchecked -> {
            itemType.setDefenseInfo(null);
            defenseCollapsible.setCollapsed(true, true);
        })).padTop(15);
        controls.row();
        controls.addSeparator().colspan(2).expand(false, false).row();

        controls.add(defenseCollapsible).colspan(2);
        controls.row();
        controls.addSeparator().colspan(2).padBottom(15).expand(false, false).row();

        return controls;
    }

    public static VisSelectBox<String> ammoTypeSelect(AmmoType currentValue, Consumer<AmmoType> listener) {
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
        itemEntityAsset.setItemTypeName(((ItemType) message.typeDescriptor()).getItemTypeName());
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
        assetComponents.add(new ItemApplicableMaterialsWidget(itemEntityAsset.getApplicableMaterialNames(), materialDictionary)).colspan(2).row();

        VisTable parentTable = new VisTable();
        parentTable.add(assetComponents).left().row();
        return parentTable;
    }
}
