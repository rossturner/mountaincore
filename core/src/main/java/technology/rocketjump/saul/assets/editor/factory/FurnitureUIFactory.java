package technology.rocketjump.saul.assets.editor.factory;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.widget.CollapsibleWidget;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisValidatableTextField;
import technology.rocketjump.saul.assets.editor.message.ShowCreateAssetDialogMessage;
import technology.rocketjump.saul.assets.editor.widgets.OkCancelDialog;
import technology.rocketjump.saul.assets.editor.widgets.ToStringDecorator;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.TagsWidget;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
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
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.materials.model.GameMaterialType;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation.*;

@Singleton
public class FurnitureUIFactory implements UIFactory {
    private final FurnitureEntityFactory furnitureEntityFactory;
    private final FurnitureTypeDictionary furnitureTypeDictionary;
    private final FurnitureAttributesPane viewEditorControls;
    private final FurnitureLayoutDictionary furnitureLayoutDictionary;
    private final MessageDispatcher messageDispatcher;

    @Inject
    public FurnitureUIFactory(FurnitureEntityFactory furnitureEntityFactory, FurnitureTypeDictionary furnitureTypeDictionary,
                              FurnitureAttributesPane viewEditorControls, FurnitureLayoutDictionary furnitureLayoutDictionary,
                              MessageDispatcher messageDispatcher) {
        this.furnitureEntityFactory = furnitureEntityFactory;
        this.furnitureTypeDictionary = furnitureTypeDictionary;
        this.viewEditorControls = viewEditorControls;
        this.furnitureLayoutDictionary = furnitureLayoutDictionary;
        this.messageDispatcher = messageDispatcher;
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

        controls.add(WidgetBuilder.label("Icon"));
        controls.add(WidgetBuilder.textField(furnitureType.getIconName(), furnitureType::setIconName));
        controls.row();

        controls.add(WidgetBuilder.label("Place Anywhere"));
        controls.add(WidgetBuilder.toggle(furnitureType.isPlaceAnywhere(), furnitureType::setPlaceAnywhere));
        controls.row();


        controls.add(WidgetBuilder.label("Automatic Construction"));
        controls.add(WidgetBuilder.toggle(furnitureType.isAutoConstructed(), furnitureType::setAutoConstructed));
        controls.row();


        controls.add(WidgetBuilder.label("Materials")).padTop(15);
        controls.row();
        controls.addSeparator().colspan(2);

        /*
	// This is the list of items (with quantities) needed to build the type for each listed GameMaterialType
	private Map<GameMaterialType, List<QuantifiedItemType>> requirements;

         */
        ToStringDecorator<GameMaterialType> requiredFloorMaterialType = ToStringDecorator.materialType(furnitureType.getRequiredFloorMaterialType());
        Collection<ToStringDecorator<GameMaterialType>> materialTypeOptions = Arrays.stream(GameMaterialType.values()).map(ToStringDecorator::materialType).toList();
        controls.add(WidgetBuilder.label("Required Floor"));
        controls.add(WidgetBuilder.select(requiredFloorMaterialType, materialTypeOptions, ToStringDecorator.none(), decorated -> {
            if (decorated != null) {
                furnitureType.setRequiredFloorMaterialType(decorated.getObject());
            }
        }));
        controls.row();

        controls.addSeparator().colspan(2).padBottom(15);

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
