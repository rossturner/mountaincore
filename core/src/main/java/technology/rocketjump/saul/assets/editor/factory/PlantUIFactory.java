package technology.rocketjump.saul.assets.editor.factory;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.GridPoint2;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.widget.*;
import net.spookygames.gdx.nativefilechooser.NativeFileChooser;
import technology.rocketjump.saul.assets.editor.message.ShowCreateAssetDialogMessage;
import technology.rocketjump.saul.assets.editor.widgets.OkCancelDialog;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.ColorsWidget;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.TagsWidget;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.plant.PlantSeasonsWidget;
import technology.rocketjump.saul.assets.editor.widgets.vieweditor.PlantAttributesPane;
import technology.rocketjump.saul.assets.entities.model.ColoringLayer;
import technology.rocketjump.saul.assets.entities.model.EntityAsset;
import technology.rocketjump.saul.entities.factories.PlantEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.plant.*;
import technology.rocketjump.saul.environment.model.Season;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.materials.GameMaterialDictionary;

import java.nio.file.Path;
import java.util.List;
import java.util.Random;

import static technology.rocketjump.saul.assets.entities.model.ColoringLayer.*;

@Singleton
public class PlantUIFactory implements UIFactory {
    private final PlantSpeciesDictionary plantSpeciesDictionary;
    private final PlantEntityFactory plantEntityFactory;
    private final PlantAttributesPane viewEditorControls;
    private final GameMaterialDictionary materialDictionary;
    private final NativeFileChooser fileChooser;
    private final MessageDispatcher messageDispatcher;

    @Inject
    public PlantUIFactory(PlantSpeciesDictionary plantSpeciesDictionary, PlantEntityFactory plantEntityFactory,
                          PlantAttributesPane viewEditorControls, GameMaterialDictionary materialDictionary,
                          NativeFileChooser fileChooser, MessageDispatcher messageDispatcher) {
        this.plantSpeciesDictionary = plantSpeciesDictionary;
        this.plantEntityFactory = plantEntityFactory;
        this.viewEditorControls = viewEditorControls;
        this.materialDictionary = materialDictionary;
        this.fileChooser = fileChooser;
        this.messageDispatcher = messageDispatcher;
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
        PlantSpecies plantSpecies = (PlantSpecies) typeDescriptor;
        VisTable controls = new VisTable();
        controls.defaults().left();
        controls.columnDefaults(0).uniformX();
        controls.columnDefaults(1).fillX();

        VisValidatableTextField nameTextField = WidgetBuilder.textField(plantSpecies.getSpeciesName(), plantSpecies::setSpeciesName);
        nameTextField.setDisabled(true);
        nameTextField.setTouchable(Touchable.disabled);
        controls.add(WidgetBuilder.label("Name"));
        controls.add(nameTextField); //TODO: make editable and update child entity asset types
        controls.row();

        controls.add(WidgetBuilder.label("Type"));
        controls.add(WidgetBuilder.select(plantSpecies.getPlantType(), PlantSpeciesType.values(), null, plantSpecies::setPlantType));
        controls.row();


        controls.add(WidgetBuilder.label("Material"));
        controls.add(WidgetBuilder.select(plantSpecies.getMaterial(), materialDictionary.getAll(), null, material -> {
            plantSpecies.setMaterial(material);
            plantSpecies.setMaterialName(material.getMaterialName());
        }));
        controls.row();

        controls.add(WidgetBuilder.label("Colors")).padTop(15);
        controls.row();

        controls.add(new ColorsWidget(plantSpecies.getDefaultColors(), getApplicableColoringLayers(),
                EntityType.PLANT, basePath, fileChooser, messageDispatcher)).colspan(2).row();

        controls.row();
        controls.add(WidgetBuilder.label("Seasons")).padTop(15);
        controls.row();
        controls.addSeparator().colspan(2);
        controls.row();

        PlantSeasonsWidget seasonsWidget = new PlantSeasonsWidget(plantSpecies, messageDispatcher, fileChooser, basePath, getApplicableColoringLayers()) {
            @Override
            public void reload() {
                super.reload();
                viewEditorControls.reload();
            }
        };
        controls.add(seasonsWidget).fill(false, false).uniform(false).expand(false, false).colspan(2).left();
        controls.row();
        controls.row().padTop(10);

        /*
	// Base material type for catching fire, that kind of thing
	private PlantSpeciesSeed seed = null;
	private List<PlantSpeciesGrowthStage> growthStages = new ArrayList<>();
         */


        VisTable addSeasonRow = new VisTable();
        addSeasonRow.add(WidgetBuilder.label("Season"));
        VisSelectBox<Season> seasonSelection = WidgetBuilder.select(null, Season.values(), null, selected -> {});
        addSeasonRow.add(seasonSelection);
        addSeasonRow.add(WidgetBuilder.button("Add", x -> {
            Season season = seasonSelection.getSelected();
            plantSpecies.getSeasons().computeIfAbsent(season, s -> new PlantSeasonSettings());
            seasonsWidget.reload();
        }));

        controls.add(addSeasonRow).colspan(2).right();
        controls.row();

        controls.addSeparator().colspan(2).padBottom(15);



        controls.add(WidgetBuilder.label("Max Growth Speed Variance"));
        controls.add(WidgetBuilder.floatSpinner(plantSpecies.getMaxGrowthSpeedVariance(), 0.0f, Float.MAX_VALUE, plantSpecies::setMaxGrowthSpeedVariance));
        controls.row();

        controls.add(WidgetBuilder.label("Occurrence Weight"));
        controls.add(WidgetBuilder.floatSpinner(plantSpecies.getOccurenceWeight(), 0.0f, Float.MAX_VALUE, plantSpecies::setOccurenceWeight));
        controls.row();

        controls.add(WidgetBuilder.label("Ignores Moisture"));
        controls.add(WidgetBuilder.toggle(plantSpecies.isIgnoresMoisture(), plantSpecies::setIgnoresMoisture));
        controls.row();

        controls.add(WidgetBuilder.label("Replaces Other Plants"));
        controls.add(WidgetBuilder.intSpinner(plantSpecies.getReplacesOtherPlantsInRegion(), 0, Integer.MAX_VALUE, plantSpecies::setReplacesOtherPlantsInRegion));
        controls.row();

        TagsWidget tagsWidget = new TagsWidget(plantSpecies.getTags());
        tagsWidget.setFillParent(true);

        CollapsibleWidget tagsCollapsible = new CollapsibleWidget(tagsWidget);
        tagsCollapsible.setCollapsed(plantSpecies.getTags().isEmpty());
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
        return null;
    }

    @Override
    public VisTable getAssetPropertyControls(EntityAsset entityAsset) {
        return null;
    }
}
