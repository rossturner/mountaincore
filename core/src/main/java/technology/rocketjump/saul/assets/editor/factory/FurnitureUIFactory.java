package technology.rocketjump.saul.assets.editor.factory;

import com.badlogic.gdx.math.GridPoint2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.widget.VisTable;
import technology.rocketjump.saul.assets.editor.message.ShowCreateAssetDialogMessage;
import technology.rocketjump.saul.assets.editor.widgets.OkCancelDialog;
import technology.rocketjump.saul.assets.editor.widgets.vieweditor.FurnitureAttributesPane;
import technology.rocketjump.saul.assets.entities.model.EntityAsset;
import technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.saul.entities.behaviour.furniture.FurnitureBehaviour;
import technology.rocketjump.saul.entities.dictionaries.furniture.FurnitureTypeDictionary;
import technology.rocketjump.saul.entities.factories.FurnitureEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.furniture.FurnitureType;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.materials.model.GameMaterialType;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation.*;

@Singleton
public class FurnitureUIFactory implements UIFactory {
    private final FurnitureEntityFactory furnitureEntityFactory;
    private final FurnitureTypeDictionary furnitureTypeDictionary;
    private final FurnitureAttributesPane viewEditorControls;

    @Inject
    public FurnitureUIFactory(FurnitureEntityFactory furnitureEntityFactory, FurnitureTypeDictionary furnitureTypeDictionary, FurnitureAttributesPane viewEditorControls) {
        this.furnitureEntityFactory = furnitureEntityFactory;
        this.furnitureTypeDictionary = furnitureTypeDictionary;
        this.viewEditorControls = viewEditorControls;
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
        return null;
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
