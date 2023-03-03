package technology.rocketjump.saul.assets.editor.factory;

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
import technology.rocketjump.saul.assets.editor.UniqueAssetNameValidator;
import technology.rocketjump.saul.assets.editor.model.EditorEntitySelection;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.assets.editor.model.ShowCreateAssetDialogMessage;
import technology.rocketjump.saul.assets.editor.model.VehicleNameBuilders;
import technology.rocketjump.saul.assets.editor.widgets.OkCancelDialog;
import technology.rocketjump.saul.assets.editor.widgets.entitybrowser.EntityBrowserValue;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.TagsWidget;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.assets.editor.widgets.vieweditor.VehicleAttributesPane;
import technology.rocketjump.saul.assets.entities.CompleteAssetDictionary;
import technology.rocketjump.saul.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.saul.assets.entities.model.EntityAsset;
import technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.assets.entities.vehicle.model.VehicleEntityAsset;
import technology.rocketjump.saul.entities.components.Faction;
import technology.rocketjump.saul.entities.dictionaries.vehicle.VehicleTypeDictionary;
import technology.rocketjump.saul.entities.factories.VehicleEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.vehicle.VehicleEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.vehicle.VehicleType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.materials.model.GameMaterialType;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.persistence.FileUtils;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

import static technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation.*;

@Singleton
public class VehicleUIFactory implements UIFactory {
    private final VehicleEntityFactory vehicleEntityFactory;
    private final VehicleTypeDictionary vehicleTypeDictionary;
    private final VehicleAttributesPane viewEditorControls;
    private final MessageDispatcher messageDispatcher;
    private final ItemTypeDictionary itemTypeDictionary;
    private final EditorStateProvider editorStateProvider;
    private final CompleteAssetDictionary completeAssetDictionary;
    private final EntityAssetTypeDictionary entityAssetTypeDictionary;
    private final EntityAssetType VEHICLE_BASE_ASSET_TYPE;

    @Inject
    public VehicleUIFactory(VehicleEntityFactory vehicleEntityFactory, VehicleTypeDictionary vehicleTypeDictionary,
                            VehicleAttributesPane viewEditorControls,
                            MessageDispatcher messageDispatcher, ItemTypeDictionary itemTypeDictionary, EditorStateProvider editorStateProvider,
                            CompleteAssetDictionary completeAssetDictionary, EntityAssetTypeDictionary entityAssetTypeDictionary) {
        this.vehicleEntityFactory = vehicleEntityFactory;
        this.vehicleTypeDictionary = vehicleTypeDictionary;
        this.viewEditorControls = viewEditorControls;
        this.messageDispatcher = messageDispatcher;
        this.itemTypeDictionary = itemTypeDictionary;
        this.editorStateProvider = editorStateProvider;
        this.completeAssetDictionary = completeAssetDictionary;
        this.entityAssetTypeDictionary = entityAssetTypeDictionary;

        this.VEHICLE_BASE_ASSET_TYPE = entityAssetTypeDictionary.getByName("VEHICLE_BASE_LAYER");
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.VEHICLE;
    }

    @Override
    public List<EntityAssetOrientation> getApplicableOrientations(EntityAsset entityAsset) {
        return List.of(DOWN, LEFT, RIGHT, UP);
    }

    @Override
    public Entity createEntityForRendering(String name) {
        VehicleType type = vehicleTypeDictionary.getByName(name);
        VehicleEntityAttributes attributes = new VehicleEntityAttributes();
        attributes.setVehicleType(type);
        return vehicleEntityFactory.create(attributes, new GridPoint2(), new GameContext(), Faction.SETTLEMENT);
    }

    @Override
    public VisTable getViewEditorControls() {
        viewEditorControls.reload();
        return viewEditorControls;
    }

    @Override
    public OkCancelDialog createEntityDialog(Path path) {
        VehicleType vehicleType = new VehicleType();
        OkCancelDialog dialog = new OkCancelDialog("Create new " + getEntityType()) {
            @Override
            public void onOk() {
                String name = vehicleType.getName();
                //required defaults
                vehicleType.setI18nKey("VEHICLE."+name.toUpperCase());
                String folderName = name.toLowerCase(Locale.ROOT);
                Path basePath = FileUtils.createDirectory(path, folderName);

                vehicleTypeDictionary.add(vehicleType);
                completeAssetDictionary.rebuild();

                EditorEntitySelection editorEntitySelection = new EditorEntitySelection();
                editorEntitySelection.setEntityType(getEntityType());
                editorEntitySelection.setTypeName(name);
                editorEntitySelection.setBasePath(basePath.toString());
                messageDispatcher.dispatchMessage(MessageType.EDITOR_ENTITY_SELECTION, editorEntitySelection);
                messageDispatcher.dispatchMessage(MessageType.EDITOR_BROWSER_TREE_SELECTION, EntityBrowserValue.forTypeDescriptor(getEntityType(), basePath, vehicleType));
            }
        };
        dialog.add(WidgetBuilder.label("Name"));
        InputValidator nonBlank = StringUtils::isNotBlank;
        InputValidator uniqueName = input -> vehicleTypeDictionary.getByName(input) == null;
        dialog.add(WidgetBuilder.textField(null, vehicleType::setName, nonBlank, uniqueName));

        return dialog;
    }

    @Override
    public VisTable getEntityPropertyControls(Object typeDescriptor, Path basePath) {
        VehicleType vehicleType = (VehicleType) typeDescriptor;
        VisTable controls = new VisTable();
        controls.defaults().left();
        controls.columnDefaults(0).uniformX().left();
        controls.columnDefaults(1).fillX().left();

        VisValidatableTextField nameTextField = WidgetBuilder.textField(vehicleType.getName(), vehicleType::setName);
        nameTextField.setDisabled(true);
        nameTextField.setTouchable(Touchable.disabled);
        controls.add(WidgetBuilder.label("Name"));
        controls.add(nameTextField); //TODO: make editable and update child entity asset types
        controls.row();

        controls.add(WidgetBuilder.label("i18n Key"));
        controls.add(WidgetBuilder.textField(vehicleType.getI18nKey(), vehicleType::setI18nKey));
        controls.row();

        controls.add(WidgetBuilder.selectField("Material Type", vehicleType.getMaterialType(), Arrays.asList(GameMaterialType.values()),
                GameMaterialType.OTHER, vehicleType::setMaterialType));
        controls.row();

        TagsWidget tagsWidget = new TagsWidget(vehicleType.getTags());
        tagsWidget.setFillParent(true);

        CollapsibleWidget tagsCollapsible = new CollapsibleWidget(tagsWidget);
        tagsCollapsible.setCollapsed(vehicleType.getTags().isEmpty());
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

    @Override
    public OkCancelDialog createAssetDialog(ShowCreateAssetDialogMessage message) {
        //TODO: quite a bit of duplication between here and the editor
        VehicleType vehicleType = (VehicleType) message.typeDescriptor();
        final Path directory = FileUtils.getDirectory(message.path()); //duplicated from CreatureUI
        Path descriptorsFile = directory.resolve("descriptors.json");

        VehicleEntityAsset asset = new VehicleEntityAsset();
        asset.setType(VEHICLE_BASE_ASSET_TYPE);
        asset.setVehicleTypeName(vehicleType.getName());
        Collection<EntityAssetType> entityAssetTypes = entityAssetTypeDictionary.getByEntityType(getEntityType());

        VisTextField nameTextField = WidgetBuilder.textField(asset.getUniqueName(), asset::setUniqueName, new UniqueAssetNameValidator(completeAssetDictionary));
        Consumer<Object> uniqueNameRebuilder = o -> {
            String builtName = VehicleNameBuilders.buildUniqueNameForAsset(vehicleType, asset);
            nameTextField.setText(builtName);
        };

        OkCancelDialog dialog = new OkCancelDialog("Create asset under " + directory) {
            @Override
            public void onOk() {
                completeAssetDictionary.add(asset);
                EntityBrowserValue value = EntityBrowserValue.forAsset(getEntityType(), descriptorsFile, asset, vehicleType);
                messageDispatcher.dispatchMessage(MessageType.EDITOR_ASSET_CREATED, value);
                messageDispatcher.dispatchMessage(MessageType.EDITOR_BROWSER_TREE_SELECTION, value);
            }
        };
        dialog.add(WidgetBuilder.label("Type"));
        dialog.add(WidgetBuilder.select(asset.getType(), entityAssetTypes, entityAssetTypes.stream().filter(t -> t.name.equals("BASE_LAYER")).findAny().orElse(null),
                compose(asset::setType, uniqueNameRebuilder)));
        dialog.row();

        dialog.add(WidgetBuilder.label("Vehicle Type"));
        dialog.add(new VisLabel(asset.getVehicleTypeName()));
        dialog.row();

        dialog.addSeparator().colspan(2).padBottom(15);

        dialog.row();
        dialog.add(WidgetBuilder.label("Name"));
        dialog.add(nameTextField);
        return dialog;
    }

    @Override
    public VisTable getAssetPropertyControls(EntityAsset entityAsset) {
        VehicleEntityAsset vehicleEntityAsset = (VehicleEntityAsset) entityAsset;
        Collection<EntityAssetType> entityAssetTypes = entityAssetTypeDictionary.getByEntityType(getEntityType());

        VisTable controls = new VisTable();
        controls.columnDefaults(0).left().uniformX();
        controls.columnDefaults(1).left().fillX();
        controls.add(WidgetBuilder.label("Name"));
        controls.add(WidgetBuilder.textField(vehicleEntityAsset.getUniqueName(), vehicleEntityAsset::setUniqueName, new UniqueAssetNameValidator(completeAssetDictionary)));
        controls.row();

        controls.add(WidgetBuilder.label("Type"));
        controls.add(WidgetBuilder.select(vehicleEntityAsset.getType(), entityAssetTypes, null, vehicleEntityAsset::setType));
        controls.row();

        controls.add(WidgetBuilder.label("Vehicle Type"));
        controls.add(WidgetBuilder.select(vehicleEntityAsset.getVehicleTypeName(), vehicleTypeDictionary.getAll().stream().map(VehicleType::getName).toList(), null, vehicleEntityAsset::setVehicleTypeName));
        controls.row();

        controls.addSeparator().colspan(2).padBottom(15);

        TagsWidget tagsWidget = new TagsWidget(vehicleEntityAsset.getTags());
        tagsWidget.setFillParent(true);

        CollapsibleWidget tagsCollapsible = new CollapsibleWidget(tagsWidget);
        tagsCollapsible.setCollapsed(vehicleEntityAsset.getTags().isEmpty());
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
