package technology.rocketjump.mountaincore.assets.editor.factory;

import com.kotcrab.vis.ui.widget.VisTable;
import technology.rocketjump.mountaincore.assets.editor.model.ShowCreateAssetDialogMessage;
import technology.rocketjump.mountaincore.assets.editor.widgets.OkCancelDialog;
import technology.rocketjump.mountaincore.assets.entities.model.ColoringLayer;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAsset;
import technology.rocketjump.mountaincore.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

public interface UIFactory {

    EntityType getEntityType();

    default List<EntityAssetOrientation> getApplicableOrientations(EntityAsset entityAsset) {
        return List.of(EntityAssetOrientation.DOWN);
    }

    default List<ColoringLayer> getApplicableColoringLayers() {
        return List.of(ColoringLayer.MISC_COLOR_1, ColoringLayer.MISC_COLOR_2, ColoringLayer.MISC_COLOR_3, ColoringLayer.MISC_COLOR_4, ColoringLayer.MISC_COLOR_5,
                ColoringLayer.BONE_COLOR, ColoringLayer.SKIN_COLOR, ColoringLayer.SEED_COLOR, ColoringLayer.VEGETABLE_COLOR, ColoringLayer.CLOTH_COLOR, ColoringLayer.ROPE_COLOR, ColoringLayer.EARTH_COLOR, ColoringLayer.STONE_COLOR,
                ColoringLayer.ORE_COLOR, ColoringLayer.GEM_COLOR, ColoringLayer.METAL_COLOR, ColoringLayer.WOOD_COLOR, ColoringLayer.VITRIOL_COLOR, ColoringLayer.FOODSTUFF_COLOR, ColoringLayer.LIQUID_COLOR, ColoringLayer.OTHER_COLOR);
    }

    Entity createEntityForRendering(String name);

    VisTable getViewEditorControls();

    OkCancelDialog createEntityDialog(Path path);

    VisTable getEntityPropertyControls(Object typeDescriptor, Path basePath);

    OkCancelDialog createAssetDialog(ShowCreateAssetDialogMessage message);

    VisTable getAssetPropertyControls(EntityAsset entityAsset);

    default <T> Consumer<T> compose(Consumer<T> input, Consumer<Object> nameBuilder) {
        return input.andThen(nameBuilder);
    }
}
