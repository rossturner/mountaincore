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
import net.spookygames.gdx.nativefilechooser.NativeFileChooser;
import org.apache.commons.lang3.StringUtils;
import technology.rocketjump.saul.assets.editor.UniqueAssetNameValidator;
import technology.rocketjump.saul.assets.editor.model.EditorEntitySelection;
import technology.rocketjump.saul.assets.editor.model.PlantNameBuilders;
import technology.rocketjump.saul.assets.editor.model.ShowCreateAssetDialogMessage;
import technology.rocketjump.saul.assets.editor.widgets.OkCancelDialog;
import technology.rocketjump.saul.assets.editor.widgets.entitybrowser.EntityBrowserValue;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.ColorsWidget;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.TagsWidget;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.plant.PlantSeasonsWidget;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.plant.PlantStagesWidget;
import technology.rocketjump.saul.assets.editor.widgets.vieweditor.PlantAttributesPane;
import technology.rocketjump.saul.assets.entities.CompleteAssetDictionary;
import technology.rocketjump.saul.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.saul.assets.entities.model.ColoringLayer;
import technology.rocketjump.saul.assets.entities.model.EntityAsset;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.assets.entities.plant.model.PlantEntityAsset;
import technology.rocketjump.saul.entities.factories.PlantEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.entities.model.physical.plant.*;
import technology.rocketjump.saul.environment.model.Season;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.materials.model.GameMaterial;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.persistence.FileUtils;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static technology.rocketjump.saul.assets.entities.model.ColoringLayer.*;

@Singleton
public class PlantUIFactory implements UIFactory {
    private final PlantSpeciesDictionary plantSpeciesDictionary;
    private final PlantEntityFactory plantEntityFactory;
    private final PlantAttributesPane viewEditorControls;
    private final GameMaterialDictionary materialDictionary;
    private final NativeFileChooser fileChooser;
    private final MessageDispatcher messageDispatcher;
    private final ItemTypeDictionary itemTypeDictionary;
    private final CompleteAssetDictionary completeAssetDictionary;
    private final EntityAssetTypeDictionary entityAssetTypeDictionary;

    @Inject
    public PlantUIFactory(PlantSpeciesDictionary plantSpeciesDictionary, PlantEntityFactory plantEntityFactory,
                          PlantAttributesPane viewEditorControls, GameMaterialDictionary materialDictionary,
                          NativeFileChooser fileChooser, MessageDispatcher messageDispatcher,
                          ItemTypeDictionary itemTypeDictionary, CompleteAssetDictionary completeAssetDictionary,
                          EntityAssetTypeDictionary entityAssetTypeDictionary) {
        this.plantSpeciesDictionary = plantSpeciesDictionary;
        this.plantEntityFactory = plantEntityFactory;
        this.viewEditorControls = viewEditorControls;
        this.materialDictionary = materialDictionary;
        this.fileChooser = fileChooser;
        this.messageDispatcher = messageDispatcher;
        this.itemTypeDictionary = itemTypeDictionary;
        this.completeAssetDictionary = completeAssetDictionary;
        this.entityAssetTypeDictionary = entityAssetTypeDictionary;
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
        PlantSpecies plantSpecies = new PlantSpecies();
        OkCancelDialog dialog = new OkCancelDialog("Create new " + getEntityType()) {
            @Override
            public void onOk() {
                String name = plantSpecies.getSpeciesName();
                //required defaults
                PlantSpeciesGrowthStage firstGrowthStage = new PlantSpeciesGrowthStage();

                plantSpecies.setMaterial(GameMaterial.NULL_MATERIAL);
                plantSpecies.setMaterialName(GameMaterial.NULL_MATERIAL.getMaterialName());
                plantSpecies.setPlantType(PlantSpeciesType.CROP);
                plantSpecies.getGrowthStages().add(firstGrowthStage);
                String folderName = name.toLowerCase(Locale.ROOT);
                Path basePath = FileUtils.createDirectory(path, folderName);

                plantSpeciesDictionary.add(plantSpecies);
                completeAssetDictionary.rebuild();

                EditorEntitySelection editorEntitySelection = new EditorEntitySelection();
                editorEntitySelection.setEntityType(getEntityType());
                editorEntitySelection.setTypeName(name);
                editorEntitySelection.setBasePath(basePath.toString());
                messageDispatcher.dispatchMessage(MessageType.EDITOR_ENTITY_SELECTION, editorEntitySelection);
                messageDispatcher.dispatchMessage(MessageType.EDITOR_BROWSER_TREE_SELECTION, EntityBrowserValue.forTypeDescriptor(getEntityType(), basePath, plantSpecies));
            }
        };
        dialog.add(WidgetBuilder.label("Name"));
        InputValidator nonBlank = StringUtils::isNotBlank;
        InputValidator uniqueName = input -> plantSpeciesDictionary.getByName(input) == null;
        dialog.add(WidgetBuilder.textField(null, plantSpecies::setSpeciesName, nonBlank, uniqueName));

        return dialog;
    }

    @Override
    public VisTable getEntityPropertyControls(Object typeDescriptor, Path basePath) {
        //TODO: Review the width sizing when color and season widgets are added
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

        final PlantSpeciesSeed seed;
        if (plantSpecies.getSeed() == null) {
            seed = new PlantSpeciesSeed();
        } else {
             seed = plantSpecies.getSeed();
        }
        VisTable seedControls = new VisTable();
        seedControls.defaults().left();
        seedControls.columnDefaults(0).expandX().left();
        seedControls.columnDefaults(1).fillX().right();
        seedControls.add(WidgetBuilder.label("Seed Item Type"));
        seedControls.add(WidgetBuilder.select(seed.getSeedItemType(), itemTypeDictionary.getAll(), null, selected -> {
            seed.setSeedItemType(selected);
            seed.setItemTypeName(selected.getItemTypeName());
        }));
        seedControls.row();
        seedControls.add(WidgetBuilder.label("Seed Material"));
        seedControls.add(WidgetBuilder.select(seed.getSeedMaterial(), materialDictionary.getAll(), null, material -> {
            seed.setSeedMaterial(material);
            seed.setMaterialName(material.getMaterialName());
        }));
        seedControls.row();

        seedControls.add(WidgetBuilder.label("Planting Seasons"));
        seedControls.add(WidgetBuilder.checkboxes(seed.getPlantingSeasons(), Arrays.asList(Season.values()), seed.getPlantingSeasons()::add, seed.getPlantingSeasons()::remove));
        seedControls.row();

        CollapsibleWidget seedCollapsible = new CollapsibleWidget(seedControls);
        seedCollapsible.setCollapsed(plantSpecies.getSeed() == null);
        controls.add(WidgetBuilder.label("Has Seed"));
        controls.add(WidgetBuilder.toggle(plantSpecies.getSeed() != null, toggle -> {
            seedCollapsible.setCollapsed(!toggle);
            if (toggle) {
                plantSpecies.setSeed(seed);
            } else {
                plantSpecies.setSeed(null);
            }
        })).row();
        controls.add(seedCollapsible).colspan(2).right().row();

        PlantSeasonsWidget seasonsWidget = new PlantSeasonsWidget(plantSpecies, messageDispatcher, fileChooser, basePath, getApplicableColoringLayers()) {
            @Override
            public void reload() {
                super.reload();
                viewEditorControls.reload();
            }
        };
        PlantStagesWidget stagesWidget = new PlantStagesWidget(plantSpecies, messageDispatcher, fileChooser, basePath, getApplicableColoringLayers(), materialDictionary, itemTypeDictionary) {
            @Override
            public void reload() {
                super.reload();
                seasonsWidget.reload(); //reload to show stage selection in seasons
            }
        };

        controls.add(stagesWidget).colspan(2);
        controls.row();
        controls.row().padTop(10);

        controls.add(WidgetBuilder.button("Add Growth Stage", x -> {
            plantSpecies.getGrowthStages().add(new PlantSpeciesGrowthStage());
            stagesWidget.reload();
        })).colspan(2).right();
        controls.row();


        controls.add(seasonsWidget).colspan(2).right();
        controls.row();
        controls.row().padTop(10);

        VisSelectBox<Season> seasonSelection = WidgetBuilder.select(null, Season.values(), null, selected -> {});
        VisTable addSeasonRow = new VisTable();
        addSeasonRow.add(WidgetBuilder.label("Season"));
        addSeasonRow.add(seasonSelection);
        addSeasonRow.add(WidgetBuilder.button("Add", x -> {
            Season season = seasonSelection.getSelected();
            plantSpecies.getSeasons().computeIfAbsent(season, s -> new PlantSeasonSettings());
            seasonsWidget.reload();
        }));

        controls.add(addSeasonRow).colspan(2).right();
        controls.row();

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
        //TODO: quite a bit of duplication between here and the editor
        PlantSpecies plantSpecies = (PlantSpecies) message.typeDescriptor();
        List<Integer> availableGrowthStages = IntStream.range(0, plantSpecies.getGrowthStages().size()).boxed().toList();
        final Path directory = FileUtils.getDirectory(message.path()); //duplicated from CreatureUI
        Path descriptorsFile = directory.resolve("descriptors.json");

        PlantEntityAsset asset = new PlantEntityAsset();
        asset.setSpeciesName(plantSpecies.getSpeciesName());
        Collection<EntityAssetType> entityAssetTypes = entityAssetTypeDictionary.getByEntityType(getEntityType());

        VisTextField nameTextField = WidgetBuilder.textField(asset.getUniqueName(), asset::setUniqueName, new UniqueAssetNameValidator(completeAssetDictionary));
        Consumer<Object> uniqueNameRebuilder = o -> {
            String builtName = PlantNameBuilders.buildUniqueNameForAsset(plantSpecies, asset);
            nameTextField.setText(builtName);
        };


        OkCancelDialog dialog = new OkCancelDialog("Create asset under " + directory) {
            @Override
            public void onOk() {
                completeAssetDictionary.add(asset);
                EntityBrowserValue value = EntityBrowserValue.forAsset(getEntityType(), descriptorsFile, asset, plantSpecies);
                messageDispatcher.dispatchMessage(MessageType.EDITOR_ASSET_CREATED, value);
                messageDispatcher.dispatchMessage(MessageType.EDITOR_BROWSER_TREE_SELECTION, value);
            }
        };
        dialog.add(WidgetBuilder.label("Type"));
        dialog.add(WidgetBuilder.select(asset.getType(), entityAssetTypes, null, compose(asset::setType, uniqueNameRebuilder)));
        dialog.row();

        dialog.add(WidgetBuilder.label("Growth Stages"));
        dialog.add(WidgetBuilder.checkboxes(asset.getGrowthStages(), availableGrowthStages, compose(asset.getGrowthStages()::add, uniqueNameRebuilder), compose(asset.getGrowthStages()::remove, uniqueNameRebuilder)));
        dialog.row();

        dialog.row();
        dialog.add(WidgetBuilder.label("Name"));
        dialog.add(nameTextField);
        return dialog;
    }

    @Override
    public VisTable getAssetPropertyControls(EntityAsset entityAsset) {
        PlantEntityAsset plantEntityAsset = (PlantEntityAsset) entityAsset;
        PlantSpecies species = plantSpeciesDictionary.getByName(plantEntityAsset.getSpeciesName());
        List<Integer> availableGrowthStages = IntStream.range(0, species.getGrowthStages().size()).boxed().toList();
        Collection<EntityAssetType> entityAssetTypes = entityAssetTypeDictionary.getByEntityType(getEntityType());

        VisTable controls = new VisTable();
        controls.columnDefaults(0).left().uniformX();
        controls.columnDefaults(1).left().fillX();
        controls.add(WidgetBuilder.label("Name"));
        controls.add(WidgetBuilder.textField(plantEntityAsset.getUniqueName(), plantEntityAsset::setUniqueName, new UniqueAssetNameValidator(completeAssetDictionary)));
        controls.row();

        controls.add(WidgetBuilder.label("Type"));
        controls.add(WidgetBuilder.select(plantEntityAsset.getType(), entityAssetTypes, null, plantEntityAsset::setType));
        controls.row();

        controls.add(WidgetBuilder.label("Growth Stages"));
        controls.add(WidgetBuilder.checkboxes(plantEntityAsset.getGrowthStages(), availableGrowthStages, plantEntityAsset.getGrowthStages()::add, plantEntityAsset.getGrowthStages()::remove));
        controls.row();

        TagsWidget tagsWidget = new TagsWidget(plantEntityAsset.getTags());
        tagsWidget.setFillParent(true);
        CollapsibleWidget tagsCollapsible = new CollapsibleWidget(tagsWidget);
        tagsCollapsible.setCollapsed(plantEntityAsset.getTags().isEmpty());
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
