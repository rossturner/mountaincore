package technology.rocketjump.mountaincore.assets.editor.factory;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.widget.*;
import org.apache.commons.lang3.StringUtils;
import technology.rocketjump.mountaincore.assets.editor.UniqueAssetNameValidator;
import technology.rocketjump.mountaincore.assets.editor.model.EditorEntitySelection;
import technology.rocketjump.mountaincore.assets.editor.model.EditorStateProvider;
import technology.rocketjump.mountaincore.assets.editor.model.FurnitureNameBuilders;
import technology.rocketjump.mountaincore.assets.editor.model.ShowCreateAssetDialogMessage;
import technology.rocketjump.mountaincore.assets.editor.widgets.OkCancelDialog;
import technology.rocketjump.mountaincore.assets.editor.widgets.ToStringDecorator;
import technology.rocketjump.mountaincore.assets.editor.widgets.entitybrowser.EntityBrowserValue;
import technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor.TagsWidget;
import technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor.furniture.RequiredMaterialsWidget;
import technology.rocketjump.mountaincore.assets.editor.widgets.vieweditor.FurnitureAttributesPane;
import technology.rocketjump.mountaincore.assets.entities.CompleteAssetDictionary;
import technology.rocketjump.mountaincore.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.mountaincore.assets.entities.furniture.model.FurnitureEntityAsset;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAsset;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetType;
import technology.rocketjump.mountaincore.entities.behaviour.furniture.FurnitureBehaviour;
import technology.rocketjump.mountaincore.entities.dictionaries.furniture.FurnitureLayoutDictionary;
import technology.rocketjump.mountaincore.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.mountaincore.entities.factories.FurnitureEntityFactory;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.mountaincore.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.mountaincore.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.mountaincore.gamecontext.GameContext;
import technology.rocketjump.mountaincore.materials.model.GameMaterialType;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.persistence.FileUtils;
import technology.rocketjump.mountaincore.ui.views.GuiViewName;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;

@Singleton
public class FurnitureUIFactory implements UIFactory {
    private final FurnitureEntityFactory furnitureEntityFactory;
    private final FurnitureTypeDictionary furnitureTypeDictionary;
    private final FurnitureAttributesPane viewEditorControls;
    private final FurnitureLayoutDictionary furnitureLayoutDictionary;
    private final MessageDispatcher messageDispatcher;
    private final ItemTypeDictionary itemTypeDictionary;
    private final EditorStateProvider editorStateProvider;
    private final CompleteAssetDictionary completeAssetDictionary;
    private final EntityAssetTypeDictionary entityAssetTypeDictionary;
    private final EntityAssetType FURNITURE_BASE_ASSET_TYPE;

    @Inject
    public FurnitureUIFactory(FurnitureEntityFactory furnitureEntityFactory, FurnitureTypeDictionary furnitureTypeDictionary,
                              FurnitureAttributesPane viewEditorControls, FurnitureLayoutDictionary furnitureLayoutDictionary,
                              MessageDispatcher messageDispatcher, ItemTypeDictionary itemTypeDictionary, EditorStateProvider editorStateProvider,
                              CompleteAssetDictionary completeAssetDictionary, EntityAssetTypeDictionary entityAssetTypeDictionary) {
        this.furnitureEntityFactory = furnitureEntityFactory;
        this.furnitureTypeDictionary = furnitureTypeDictionary;
        this.viewEditorControls = viewEditorControls;
        this.furnitureLayoutDictionary = furnitureLayoutDictionary;
        this.messageDispatcher = messageDispatcher;
        this.itemTypeDictionary = itemTypeDictionary;
        this.editorStateProvider = editorStateProvider;
        this.completeAssetDictionary = completeAssetDictionary;
        this.entityAssetTypeDictionary = entityAssetTypeDictionary;
        this.FURNITURE_BASE_ASSET_TYPE = entityAssetTypeDictionary.getByName("BASE_LAYER");
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.FURNITURE;
    }

    @Override
    public Entity createEntityForRendering(String name) {
        FurnitureType type = furnitureTypeDictionary.getByName(name);
        Optional<GameMaterialType> anyRequiredMaterial = type.getRequirements().keySet().stream().findAny();
        FurnitureEntityAttributes attributes = new FurnitureEntityAttributes();
        attributes.setFurnitureType(type);
        attributes.setPrimaryMaterialType(anyRequiredMaterial.get());
        return furnitureEntityFactory.create(attributes, new GridPoint2(), new FurnitureBehaviour(), new GameContext());
    }

    @Override
    public VisTable getViewEditorControls() {
        viewEditorControls.reload();
        return viewEditorControls;
    }

    @Override
    public OkCancelDialog createEntityDialog(Path path) {
        FurnitureType furnitureType = new FurnitureType();
        OkCancelDialog dialog = new OkCancelDialog("Create new " + getEntityType()) {
            @Override
            public void onOk() {
                String name = furnitureType.getName();
                //required defaults
                furnitureType.setI18nKey("FURNITURE."+name.toUpperCase(Locale.ROOT));
                furnitureType.setRequirements(new HashMap<>());
                furnitureType.getRequirements().put(GameMaterialType.EARTH, new ArrayList<>());
                furnitureType.setDefaultLayoutName("1x1");
                String folderName = name.toLowerCase(Locale.ROOT);
                Path basePath = FileUtils.createDirectory(path, folderName);

                furnitureTypeDictionary.add(furnitureType);
                completeAssetDictionary.rebuild();

                EditorEntitySelection editorEntitySelection = new EditorEntitySelection();
                editorEntitySelection.setEntityType(getEntityType());
                editorEntitySelection.setTypeName(name);
                editorEntitySelection.setBasePath(basePath.toString());
                messageDispatcher.dispatchMessage(MessageType.EDITOR_ENTITY_SELECTION, editorEntitySelection);
                messageDispatcher.dispatchMessage(MessageType.EDITOR_BROWSER_TREE_SELECTION, EntityBrowserValue.forTypeDescriptor(getEntityType(), basePath, furnitureType));
            }
        };
        dialog.add(WidgetBuilder.label("Name"));
        InputValidator nonBlank = StringUtils::isNotBlank;
        InputValidator uniqueName = input -> furnitureTypeDictionary.getByName(input) == null;
        dialog.add(WidgetBuilder.textField(null, furnitureType::setName, nonBlank, uniqueName));

        return dialog;
    }

    @Override
    public VisTable getEntityPropertyControls(Object typeDescriptor, Path basePath) {
        FurnitureType furnitureType = (FurnitureType) typeDescriptor;
        VisTable controls = new VisTable();
        controls.defaults().left();
        controls.columnDefaults(0).uniformX().left();
        controls.columnDefaults(1).fillX().left();

        VisValidatableTextField nameTextField = WidgetBuilder.textField(furnitureType.getName(), furnitureType::setName);
        nameTextField.setDisabled(true);
        nameTextField.setTouchable(Touchable.disabled);
        controls.add(WidgetBuilder.label("Name"));
        controls.add(nameTextField); //TODO: make editable and update child entity asset types
        controls.row();

        controls.add(WidgetBuilder.label("i18n Key"));
        controls.add(WidgetBuilder.textField(furnitureType.getI18nKey(), furnitureType::setI18nKey));
        controls.row();

        controls.add(WidgetBuilder.label("Blocks Movement"));
        controls.add(WidgetBuilder.toggle(furnitureType.isBlocksMovement(), furnitureType::setBlocksMovement));
        controls.row();

        controls.add(WidgetBuilder.label("Default Layout"));
        controls.add(WidgetBuilder.select(furnitureType.getDefaultLayout(), furnitureLayoutDictionary.getAll(), null, layout -> {
            furnitureType.setDefaultLayout(layout);
            furnitureType.setDefaultLayoutName(layout.getUniqueName());
            //TODO: feels dirty
            FurnitureEntityAttributes attributes = (FurnitureEntityAttributes) editorStateProvider.getState().getCurrentEntity().getPhysicalEntityComponent().getAttributes();
            attributes.setCurrentLayout(layout);
            viewEditorControls.reload();
        }));
        controls.row();

        controls.add(WidgetBuilder.label("Show in GUI View"));
        controls.add(WidgetBuilder.select(furnitureType.getShowInGuiView(), GuiViewName.values(), GuiViewName.NULL, furnitureType::setShowInGuiView));
        controls.row();


        controls.add(WidgetBuilder.label("Automatic Construction"));
        controls.add(WidgetBuilder.toggle(furnitureType.isAutoConstructed(), furnitureType::setAutoConstructed));
        controls.row();


        controls.add(WidgetBuilder.label("Required Materials")).padTop(15);
        controls.row();
        controls.addSeparator().colspan(2);
        controls.row();

        RequiredMaterialsWidget requiredMaterialsWidget = new RequiredMaterialsWidget(furnitureType, itemTypeDictionary) {
            @Override
            public void reload() {
                super.reload();
                viewEditorControls.reload();
            }
        };
        controls.add(requiredMaterialsWidget).fill(false, false).uniform(false).expand(false, false).colspan(2).left();
        controls.row();
        controls.row().padTop(10);

        VisTable addRequiredMaterialRow = new VisTable();


        VisSelectBox<ToStringDecorator<GameMaterialType>> requiredMaterialSelect = WidgetBuilder.select(null,
                Arrays.stream(GameMaterialType.values()).map(ToStringDecorator::materialType).toList(), null, selected -> {});

        addRequiredMaterialRow.add(WidgetBuilder.label("Furniture Material"));
        addRequiredMaterialRow.add(requiredMaterialSelect);
        addRequiredMaterialRow.add(WidgetBuilder.button("Add", x -> {
            GameMaterialType materialType = requiredMaterialSelect.getSelected().getObject();

            furnitureType.getRequirements().computeIfAbsent(materialType, s -> new ArrayList<>());
            requiredMaterialsWidget.reload();
        }));

        controls.add(addRequiredMaterialRow).colspan(2).right();
        controls.row();

        controls.addSeparator().colspan(2).padBottom(15);


        ToStringDecorator<GameMaterialType> requiredFloorMaterialType = ToStringDecorator.materialType(furnitureType.getRequiredFloorMaterialType());
        Collection<ToStringDecorator<GameMaterialType>> materialTypeOptions = Arrays.stream(GameMaterialType.values()).map(ToStringDecorator::materialType).toList();
        controls.add(WidgetBuilder.label("Required Floor"));
        controls.add(WidgetBuilder.select(requiredFloorMaterialType, materialTypeOptions, ToStringDecorator.none(), decorated -> {
            if (decorated != null) {
                furnitureType.setRequiredFloorMaterialType(decorated.getObject());
            }
        }));
        controls.row();

        TagsWidget tagsWidget = new TagsWidget(furnitureType.getTags());
        tagsWidget.setFillParent(true);

        CollapsibleWidget tagsCollapsible = new CollapsibleWidget(tagsWidget);
        tagsCollapsible.setCollapsed(furnitureType.getTags().isEmpty());
        VisLabel tagsLabel = new VisLabel("Tags (click to show)");
        tagsLabel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                tagsCollapsible.setCollapsed(!tagsCollapsible.isCollapsed());
            }
        });
        controls.add(tagsLabel).row();
        controls.add();
        controls.add(tagsCollapsible).right().row();

        controls.add(WidgetBuilder.label("Hide from Placement Menu"));
        controls.add(WidgetBuilder.toggle(furnitureType.isHiddenFromPlacementMenu(), furnitureType::setHiddenFromPlacementMenu));
        controls.row();

        return controls;
    }

    @Override
    public OkCancelDialog createAssetDialog(ShowCreateAssetDialogMessage message) {
        //TODO: quite a bit of duplication between here and the editor
        FurnitureType furnitureType = (FurnitureType) message.typeDescriptor();
        final Path directory = FileUtils.getDirectory(message.path()); //duplicated from CreatureUI
        Path descriptorsFile = directory.resolve("descriptors.json");

        FurnitureEntityAsset asset = new FurnitureEntityAsset();
        asset.setFurnitureTypeName(furnitureType.getName());
        asset.setType(FURNITURE_BASE_ASSET_TYPE);
        asset.setValidMaterialTypes(new ArrayList<>());
        asset.setFurnitureLayoutName(furnitureType.getDefaultLayoutName());
        Collection<EntityAssetType> entityAssetTypes = entityAssetTypeDictionary.getByEntityType(getEntityType());

        VisTextField nameTextField = WidgetBuilder.textField(asset.getUniqueName(), asset::setUniqueName, new UniqueAssetNameValidator(completeAssetDictionary));
        Consumer<Object> uniqueNameRebuilder = o -> {
            String builtName = FurnitureNameBuilders.buildUniqueNameForAsset(furnitureType, asset);
            nameTextField.setText(builtName);
        };


        OkCancelDialog dialog = new OkCancelDialog("Create asset under " + directory) {
            @Override
            public void onOk() {
                completeAssetDictionary.add(asset);
                EntityBrowserValue value = EntityBrowserValue.forAsset(getEntityType(), descriptorsFile, asset, furnitureType);
                messageDispatcher.dispatchMessage(MessageType.EDITOR_ASSET_CREATED, value);
                messageDispatcher.dispatchMessage(MessageType.EDITOR_BROWSER_TREE_SELECTION, value);
            }
        };
        dialog.add(WidgetBuilder.label("Type"));
        dialog.add(WidgetBuilder.select(asset.getType(), entityAssetTypes, entityAssetTypes.stream().filter(t -> t.name.equals("BASE_LAYER")).findAny().orElse(null),
                compose(asset::setType, uniqueNameRebuilder)));
        dialog.row();

        dialog.add(WidgetBuilder.label("Furniture Type"));
        dialog.add(new VisLabel(asset.getFurnitureTypeName()));
        dialog.row();

        dialog.add(WidgetBuilder.label("Layout"));
        dialog.add(WidgetBuilder.select(asset.getFurnitureLayoutName(), furnitureLayoutDictionary.getAll().stream().map(FurnitureLayout::getUniqueName).toList(), null, compose(asset::setFurnitureLayoutName, uniqueNameRebuilder)));
        dialog.row();

        dialog.add(WidgetBuilder.label("Valid Materials")).padTop(15);
        dialog.row();
        dialog.addSeparator().colspan(2);

        //Todo: nicer display name
        int checkboxColCount = 1;
        for (GameMaterialType materialType : GameMaterialType.values()) {
            VisCheckBox checkBox = WidgetBuilder.checkBox(materialType, asset.getValidMaterialTypes().contains(materialType),
                    it -> {
                        if (!asset.getValidMaterialTypes().contains(it)) {
                            asset.getValidMaterialTypes().add(it);
                            uniqueNameRebuilder.accept(null);
                        }

                    }, compose(asset.getValidMaterialTypes()::remove, uniqueNameRebuilder));
            checkBox.setChecked(furnitureType.getRequirements().containsKey(materialType));
            dialog.add(checkBox).fill(false, false).left();
            if (checkboxColCount % 2 == 0) {
                dialog.row();
            }
            checkboxColCount++;
        }

        dialog.row();

        dialog.addSeparator().colspan(2).padBottom(15);

        dialog.row();
        dialog.add(WidgetBuilder.label("Name"));
        dialog.add(nameTextField);
        return dialog;
    }

    @Override
    public VisTable getAssetPropertyControls(EntityAsset entityAsset) {
        FurnitureEntityAsset furnitureEntityAsset = (FurnitureEntityAsset) entityAsset;
        Collection<EntityAssetType> entityAssetTypes = entityAssetTypeDictionary.getByEntityType(getEntityType());

        VisTable controls = new VisTable();
        controls.columnDefaults(0).left().uniformX();
        controls.columnDefaults(1).left().fillX();
        controls.add(WidgetBuilder.label("Name"));
        controls.add(WidgetBuilder.textField(furnitureEntityAsset.getUniqueName(), furnitureEntityAsset::setUniqueName, new UniqueAssetNameValidator(completeAssetDictionary)));
        controls.row();

        controls.add(WidgetBuilder.label("Type"));
        controls.add(WidgetBuilder.select(furnitureEntityAsset.getType(), entityAssetTypes, null, furnitureEntityAsset::setType));
        controls.row();

        controls.add(WidgetBuilder.label("Furniture Type"));
        controls.add(WidgetBuilder.select(furnitureEntityAsset.getFurnitureTypeName(), furnitureTypeDictionary.getAll().stream().map(FurnitureType::getName).toList(), null, furnitureEntityAsset::setFurnitureTypeName));
        controls.row();

        controls.add(WidgetBuilder.label("Layout"));
        controls.add(WidgetBuilder.select(furnitureEntityAsset.getFurnitureLayoutName(), furnitureLayoutDictionary.getAll().stream().map(FurnitureLayout::getUniqueName).toList(), null, furnitureEntityAsset::setFurnitureLayoutName));
        controls.row();


        controls.add(WidgetBuilder.label("Valid Materials")).padTop(15);
        controls.row();
        controls.addSeparator().colspan(2);

        //Todo: nicer display name
        int checkboxColCount = 1;
        for (GameMaterialType materialType : GameMaterialType.values()) {
            VisCheckBox checkBox = WidgetBuilder.checkBox(materialType, furnitureEntityAsset.getValidMaterialTypes().contains(materialType),
                    it -> {
                        if (!furnitureEntityAsset.getValidMaterialTypes().contains(it)) {
                            furnitureEntityAsset.getValidMaterialTypes().add(it);
                        }
                    }, furnitureEntityAsset.getValidMaterialTypes()::remove);
            controls.add(checkBox).fill(false, false).left();
            if (checkboxColCount % 2 == 0) {
                controls.row();
            }
            checkboxColCount++;
        }

        controls.row();

        controls.addSeparator().colspan(2).padBottom(15);


        TagsWidget tagsWidget = new TagsWidget(furnitureEntityAsset.getTags());
        tagsWidget.setFillParent(true);

        CollapsibleWidget tagsCollapsible = new CollapsibleWidget(tagsWidget);
        tagsCollapsible.setCollapsed(furnitureEntityAsset.getTags().isEmpty());
        VisLabel tagsLabel = new VisLabel("Tags (click to show)");
        tagsLabel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                tagsCollapsible.setCollapsed(!tagsCollapsible.isCollapsed());
            }
        });
        controls.add(tagsLabel).row();
        controls.add();
        controls.add(tagsCollapsible).right().row();

        return controls;
    }
}
