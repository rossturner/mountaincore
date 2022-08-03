package technology.rocketjump.saul.assets.editor.factory;

import com.badlogic.gdx.math.GridPoint2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.widget.VisTable;
import technology.rocketjump.saul.assets.editor.message.ShowCreateAssetDialogMessage;
import technology.rocketjump.saul.assets.editor.widgets.OkCancelDialog;
import technology.rocketjump.saul.assets.editor.widgets.vieweditor.PlantAttributesPane;
import technology.rocketjump.saul.assets.entities.model.ColoringLayer;
import technology.rocketjump.saul.assets.entities.model.EntityAsset;
import technology.rocketjump.saul.entities.factories.PlantEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.plant.PlantEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpecies;
import technology.rocketjump.saul.entities.model.physical.plant.PlantSpeciesDictionary;
import technology.rocketjump.saul.gamecontext.GameContext;

import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static technology.rocketjump.saul.assets.entities.model.ColoringLayer.*;

@Singleton
public class PlantUIFactory implements UIFactory {
    private final PlantSpeciesDictionary plantSpeciesDictionary;
    private final PlantEntityFactory plantEntityFactory;
    private final PlantAttributesPane viewEditorControls;

    @Inject
    public PlantUIFactory(PlantSpeciesDictionary plantSpeciesDictionary, PlantEntityFactory plantEntityFactory, PlantAttributesPane viewEditorControls) {
        this.plantSpeciesDictionary = plantSpeciesDictionary;
        this.plantEntityFactory = plantEntityFactory;
        this.viewEditorControls = viewEditorControls;
    }

    @Override
    public List<ColoringLayer> getApplicableColoringLayers() {
        return List.of(BRANCHES_COLOR, LEAF_COLOR, FRUIT_COLOR, FLOWER_COLOR, WOOD_COLOR, OTHER_COLOR, VEGETABLE_COLOR);
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.PLANT;
    }

    @Override
    public Entity createEntityForRendering(String name) {
        PlantSpecies species = plantSpeciesDictionary.getByName(name);
        PlantEntityAttributes attributes = new PlantEntityAttributes(new Random().nextLong(), species);
        return plantEntityFactory.create(attributes, new GridPoint2(), new GameContext());
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
