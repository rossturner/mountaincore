package technology.rocketjump.saul.assets.editor.factory;

import com.kotcrab.vis.ui.widget.VisTable;
import technology.rocketjump.saul.assets.editor.message.ShowCreateAssetDialogMessage;
import technology.rocketjump.saul.assets.editor.widgets.OkCancelDialog;
import technology.rocketjump.saul.assets.entities.model.ColoringLayer;
import technology.rocketjump.saul.assets.entities.model.EntityAsset;
import technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

import static technology.rocketjump.saul.assets.entities.model.ColoringLayer.*;
import static technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation.DOWN;

public interface UIFactory {

    EntityType getEntityType();

    default List<EntityAssetOrientation> getApplicableOrientations(EntityAsset entityAsset) {
        return List.of(DOWN);
    }

    default List<ColoringLayer> getApplicableColoringLayers() {
        return List.of(MISC_COLOR_1, MISC_COLOR_2, MISC_COLOR_3, MISC_COLOR_4, MISC_COLOR_5,
                BONE_COLOR, SEED_COLOR, VEGETABLE_COLOR, CLOTH_COLOR, ROPE_COLOR, EARTH_COLOR, STONE_COLOR,
                ORE_COLOR, GEM_COLOR, METAL_COLOR, WOOD_COLOR, VITRIOL_COLOR, FOODSTUFF_COLOR, LIQUID_COLOR, OTHER_COLOR);
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
