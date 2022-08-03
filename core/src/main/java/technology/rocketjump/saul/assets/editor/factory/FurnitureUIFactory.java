package technology.rocketjump.saul.assets.editor.factory;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.util.InputValidator;
import com.kotcrab.vis.ui.util.adapter.ArrayListAdapter;
import com.kotcrab.vis.ui.widget.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import technology.rocketjump.saul.assets.editor.UniqueAssetNameValidator;
import technology.rocketjump.saul.assets.editor.message.ShowCreateAssetDialogMessage;
import technology.rocketjump.saul.assets.editor.model.EditorEntitySelection;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.assets.editor.widgets.OkCancelDialog;
import technology.rocketjump.saul.assets.editor.widgets.ToStringDecorator;
import technology.rocketjump.saul.assets.editor.widgets.entitybrowser.EntityBrowserValue;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.TagsWidget;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.furniture.RequiredMaterialsWidget;
import technology.rocketjump.saul.assets.editor.widgets.vieweditor.FurnitureAttributesPane;
import technology.rocketjump.saul.assets.entities.CompleteAssetDictionary;
import technology.rocketjump.saul.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.saul.assets.entities.furniture.model.FurnitureEntityAsset;
import technology.rocketjump.saul.assets.entities.model.EntityAsset;
import technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.entities.behaviour.furniture.FurnitureBehaviour;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureLayoutDictionary;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.saul.entities.factories.FurnitureEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureLayout;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.materials.model.GameMaterialType;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.persistence.FileUtils;
import technology.rocketjump.saul.rendering.utils.HexColors;

import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;

import static technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation.*;

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
    }

    @Override
    public List<EntityAssetOrientation> getApplicableOrientations(EntityAsset entityAsset) {
        return List.of(DOWN, LEFT, RIGHT, UP);
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
                furnitureType.setRequirements(new HashMap<>());
                furnitureType.getRequirements().put(GameMaterialType.EARTH, new ArrayList<>());
                furnitureType.setDefaultLayoutName("1x1");
                furnitureType.setColorCode(HexColors.toHexString(Color.WHITE));
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

        controls.add(WidgetBuilder.label("Color"));
        controls.add(WidgetBuilder.colorPickerTextField(messageDispatcher, furnitureType.getColor(), (color, hex) -> {
            furnitureType.setColor(color);
            furnitureType.setColorCode(hex);
        }));
        controls.row();

        List<Path> icons = FileUtils.findFilesByFilename(editorStateProvider.getState().getModDirPath().resolve("icons"),
                                                            Pattern.compile(".*\\.png"));

        class IconAdapter extends ArrayListAdapter<Path, VisTable> {
            private final Drawable bg = VisUI.getSkin().getDrawable("window-bg");
            private final Drawable selection = VisUI.getSkin().getDrawable("list-selection");

            public IconAdapter(List<Path> array) {
                super(new ArrayList<>(array));
                setSelectionMode(SelectionMode.SINGLE);
            }

            @Override
            protected void selectView(VisTable view) {
                view.setBackground(selection);
            }

            @Override
            protected void deselectView(VisTable view) {
                view.setBackground(bg);
            }

            @Override
            protected VisTable createView(Path item) {
                Texture texture = new Texture(new FileHandle(item.toFile()));
                Image image = new Image(texture);
                VisTable row = new VisTable();
                row.left();
                row.add(image).maxHeight(24).maxWidth(24);
                row.add(new VisLabel(item.getFileName().toString())).uniformX().fillX();
                return row;
            }
        }
        IconAdapter iconAdapter = new IconAdapter(icons);
        ListView<Path> iconList = new ListView<>(iconAdapter);
        iconList.setItemClickListener(new ListView.ItemClickListener<Path>() {
            @Override
            public void clicked(Path item) {
                String iconName = FilenameUtils.removeExtension(item.getFileName().toString());
                furnitureType.setIconName(iconName);
            }
        });
        if (furnitureType.getIconName() != null) {
            icons.stream().filter(iconPath -> iconPath.toString().contains(furnitureType.getIconName())).findAny().ifPresent(matched -> {
                iconAdapter.getSelectionManager().select(matched);
            });
        }

        controls.add(WidgetBuilder.label("Icon"));
        controls.add(iconList.getMainTable()).maxHeight(64);
        controls.row();

        controls.add(WidgetBuilder.label("Place Anywhere"));
        controls.add(WidgetBuilder.toggle(furnitureType.isPlaceAnywhere(), furnitureType::setPlaceAnywhere));
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
        return null;
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
