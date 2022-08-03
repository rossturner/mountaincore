package technology.rocketjump.saul.assets.editor.factory;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.files.FileHandle;
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
import com.kotcrab.vis.ui.util.adapter.ArrayListAdapter;
import com.kotcrab.vis.ui.widget.*;
import org.apache.commons.io.FilenameUtils;
import technology.rocketjump.saul.assets.editor.message.ShowCreateAssetDialogMessage;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.assets.editor.widgets.OkCancelDialog;
import technology.rocketjump.saul.assets.editor.widgets.ToStringDecorator;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.TagsWidget;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.furniture.RequiredMaterialsWidget;
import technology.rocketjump.saul.assets.editor.widgets.vieweditor.FurnitureAttributesPane;
import technology.rocketjump.saul.assets.entities.model.EntityAsset;
import technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.saul.entities.behaviour.furniture.FurnitureBehaviour;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureLayoutDictionary;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.saul.entities.factories.FurnitureEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.materials.model.GameMaterialType;
import technology.rocketjump.saul.persistence.FileUtils;

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

    @Inject
    public FurnitureUIFactory(FurnitureEntityFactory furnitureEntityFactory, FurnitureTypeDictionary furnitureTypeDictionary,
                              FurnitureAttributesPane viewEditorControls, FurnitureLayoutDictionary furnitureLayoutDictionary,
                              MessageDispatcher messageDispatcher, ItemTypeDictionary itemTypeDictionary, EditorStateProvider editorStateProvider) {
        this.furnitureEntityFactory = furnitureEntityFactory;
        this.furnitureTypeDictionary = furnitureTypeDictionary;
        this.viewEditorControls = viewEditorControls;
        this.furnitureLayoutDictionary = furnitureLayoutDictionary;
        this.messageDispatcher = messageDispatcher;
        this.itemTypeDictionary = itemTypeDictionary;
        this.editorStateProvider = editorStateProvider;
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
        return null;
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
        icons.stream().filter(iconPath -> iconPath.toString().contains(furnitureType.getIconName())).findAny().ifPresent(matched -> {
            iconAdapter.getSelectionManager().select(matched);

        });

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

        RequiredMaterialsWidget requiredMaterialsWidget = new RequiredMaterialsWidget(furnitureType, itemTypeDictionary);
        controls.add(requiredMaterialsWidget).colspan(2).row();
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
        CollapsibleWidget tagsCollapsible = new CollapsibleWidget(tagsWidget);
        tagsCollapsible.setCollapsed(furnitureType.getTags().isEmpty());
        VisLabel tagsLabel = new VisLabel("Tags (click to show)");
        tagsLabel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                tagsCollapsible.setCollapsed(!tagsCollapsible.isCollapsed());
            }
        });
        controls.add(tagsLabel).left().expandX().fillX().colspan(2).row();
        controls.add(tagsCollapsible).expandX().fillX().left().colspan(2).row();


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
        return null;
    }
}
