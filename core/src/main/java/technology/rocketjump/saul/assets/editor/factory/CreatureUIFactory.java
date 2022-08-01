package technology.rocketjump.saul.assets.editor.factory;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.widget.*;
import net.spookygames.gdx.nativefilechooser.NativeFileChooser;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.pmw.tinylog.Logger;
import technology.rocketjump.saul.assets.editor.UniqueAssetNameValidator;
import technology.rocketjump.saul.assets.editor.message.ShowCreateAssetDialogMessage;
import technology.rocketjump.saul.assets.editor.model.CreatureNameBuilders;
import technology.rocketjump.saul.assets.editor.model.EditorEntitySelection;
import technology.rocketjump.saul.assets.editor.widgets.OkCancelDialog;
import technology.rocketjump.saul.assets.editor.widgets.TextBoxConventionListener;
import technology.rocketjump.saul.assets.editor.widgets.entitybrowser.EntityBrowserValue;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.ColorsWidget;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.TagsWidget;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.creature.BodyShapesWidget;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.creature.GenderWidget;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.creature.RaceFeaturesWidget;
import technology.rocketjump.saul.assets.editor.widgets.vieweditor.CreatureAttributesPane;
import technology.rocketjump.saul.assets.entities.CompleteAssetDictionary;
import technology.rocketjump.saul.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShape;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShapeDescriptor;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.saul.assets.entities.model.ColoringLayer;
import technology.rocketjump.saul.assets.entities.model.EntityAsset;
import technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.entities.ai.goap.EntityNeed;
import technology.rocketjump.saul.entities.ai.goap.ScheduleDictionary;
import technology.rocketjump.saul.entities.behaviour.creature.CreatureBehaviourDictionary;
import technology.rocketjump.saul.entities.factories.CreatureEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.creature.*;
import technology.rocketjump.saul.entities.model.physical.creature.body.BodyStructureDictionary;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.jobs.ProfessionDictionary;
import technology.rocketjump.saul.jobs.model.Profession;
import technology.rocketjump.saul.materials.GameMaterialDictionary;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.misc.ReflectionUtils;
import technology.rocketjump.saul.persistence.FileUtils;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder.*;
import static technology.rocketjump.saul.assets.entities.model.ColoringLayer.*;
import static technology.rocketjump.saul.assets.entities.model.EntityAssetOrientation.*;
import static technology.rocketjump.saul.jobs.ProfessionDictionary.NULL_PROFESSION;

@Singleton
public class CreatureUIFactory implements UIFactory {
    private final MessageDispatcher messageDispatcher;
    private final EntityAssetTypeDictionary entityAssetTypeDictionary;
    private final CompleteAssetDictionary completeAssetDictionary;
    private final RaceDictionary raceDictionary;
    private final CreatureEntityFactory creatureEntityFactory;
    private final ProfessionDictionary professionDictionary;
    private final CreatureAttributesPane viewEditorControls;
    private final BodyStructureDictionary bodyStructureDictionary;
    private final CreatureBehaviourDictionary creatureBehaviourDictionary;
    private final ScheduleDictionary scheduleDictionary;
    private final GameMaterialDictionary gameMaterialDictionary;
    private final ItemTypeDictionary itemTypeDictionary;
    private final NativeFileChooser fileChooser;

    @Inject
    public CreatureUIFactory(MessageDispatcher messageDispatcher, EntityAssetTypeDictionary entityAssetTypeDictionary,
                             CompleteAssetDictionary completeAssetDictionary, RaceDictionary raceDictionary,
                             CreatureEntityFactory creatureEntityFactory, ProfessionDictionary professionDictionary,
                             CreatureAttributesPane viewEditorControls, BodyStructureDictionary bodyStructureDictionary,
                             CreatureBehaviourDictionary creatureBehaviourDictionary, ScheduleDictionary scheduleDictionary,
                             GameMaterialDictionary gameMaterialDictionary, ItemTypeDictionary itemTypeDictionary,
                             NativeFileChooser fileChooser) {
        this.messageDispatcher = messageDispatcher;
        this.entityAssetTypeDictionary = entityAssetTypeDictionary;
        this.completeAssetDictionary = completeAssetDictionary;
        this.raceDictionary = raceDictionary;
        this.creatureEntityFactory = creatureEntityFactory;
        this.professionDictionary = professionDictionary;
        this.viewEditorControls = viewEditorControls;
        this.bodyStructureDictionary = bodyStructureDictionary;
        this.creatureBehaviourDictionary = creatureBehaviourDictionary;
        this.scheduleDictionary = scheduleDictionary;
        this.gameMaterialDictionary = gameMaterialDictionary;
        this.itemTypeDictionary = itemTypeDictionary;
        this.fileChooser = fileChooser;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CREATURE;
    }

    @Override
    public List<EntityAssetOrientation> getApplicableOrientations(EntityAsset entityAsset) {
        return List.of(DOWN, DOWN_LEFT, DOWN_RIGHT, UP_LEFT, UP_RIGHT, UP);
    }

    @Override
    public List<ColoringLayer> getApplicableColoringLayers() {
        return List.of(HAIR_COLOR, SKIN_COLOR, EYE_COLOR, ACCESSORY_COLOR, MARKING_COLOR, BONE_COLOR, OTHER_COLOR);
    }

    @Override
    public Entity createEntityForRendering(String name) {
        Random random = new Random();
		GameContext gameContext = new GameContext();
		gameContext.setRandom(new RandomXS128());
        Race race = raceDictionary.getByName(name);
        CreatureEntityAttributes attributes = new CreatureEntityAttributes(race, random.nextLong());
        Vector2 origin = new Vector2(0, 0f);
        return creatureEntityFactory.create(attributes, origin, origin, gameContext);
    }

    @Override
    public VisTable getViewEditorControls() {
        viewEditorControls.reload();
        return viewEditorControls;
    }

    @Override
    public OkCancelDialog createEntityDialog(Path path) {
        Function<String, Boolean> validRace = new Function<>() {
            @Override
            public Boolean apply(String input) {
                if (StringUtils.length(input) < 2) {
                    return false;
                }
                Race existing = raceDictionary.getByName(input);
                return existing == null;
            }
        };

        VisValidatableTextField raceNameTextField = new VisValidatableTextField();
        raceNameTextField.addValidator(validRace::apply);
        raceNameTextField.addListener(new TextBoxConventionListener(WordUtils::capitalize));

        OkCancelDialog dialog = new OkCancelDialog("Create new " + getEntityType()) {
            @Override
            public void onOk() {
                String folderName = raceNameTextField.getText().toLowerCase(Locale.ROOT);
                Path basePath = FileUtils.createDirectory(path, folderName);
                String raceName = raceNameTextField.getText();
                CreatureBodyShapeDescriptor bodyShape = new CreatureBodyShapeDescriptor();
                bodyShape.setValue(CreatureBodyShape.AVERAGE);
                Race newRace = new Race();
                newRace.setName(raceName);
                newRace.setI18nKey("RACE." + raceName.toUpperCase());
                newRace.setBodyStructureName("pawed-quadruped");
                newRace.setBodyShapes(List.of(bodyShape));
                newRace.setGenders(new HashMap<>(Map.of(
                        Gender.MALE, new RaceGenderDescriptor(),
                        Gender.FEMALE, new RaceGenderDescriptor()
                )));
                newRace.getGenders().values().forEach(v -> v.setWeighting(0.5f));
                raceDictionary.add(newRace);
                completeAssetDictionary.rebuild();

                EditorEntitySelection editorEntitySelection = new EditorEntitySelection();
                editorEntitySelection.setEntityType(getEntityType());
                editorEntitySelection.setTypeName(raceName);
                editorEntitySelection.setBasePath(basePath.toString());
                messageDispatcher.dispatchMessage(MessageType.EDITOR_ENTITY_SELECTION, editorEntitySelection);
                messageDispatcher.dispatchMessage(MessageType.EDITOR_BROWSER_TREE_SELECTION, EntityBrowserValue.forTypeDescriptor(getEntityType(), basePath, newRace));
            }
        };
        dialog.add(new VisLabel(getEntityType().typeDescriptorClass.getSimpleName()));
        dialog.add(raceNameTextField);

        return dialog;
    }

    @Override
    public VisTable getEntityPropertyControls(Object typeDescriptor, Path basePath) {
        VisTable editorTable = new VisTable();
        Race race = (Race) typeDescriptor;
        //TODO: make cascade down to entity assets
        VisValidatableTextField nameTextField = textField(race.getName(), race::setName);
        nameTextField.setDisabled(true);
        nameTextField.setTouchable(Touchable.disabled);
        editorTable.add(WidgetBuilder.label("Name"));
        editorTable.add(nameTextField);
        editorTable.row();
//        addTextField("Name:", "name", race, editorTable);
        addTextField("I18N key:", "i18nKey", race, editorTable);

        addFloatField("Minimum strength:", "minStrength", race, editorTable);
        addFloatField("Maximum strength:", "maxStrength", race, editorTable);

        addSelectField("Body structure:", "bodyStructure", bodyStructureDictionary.getAll(), null, race, editorTable);

        VisLabel bodyShapesLabel = new VisLabel("Body shapes:");
        VisTable bodyShapesTable = new VisTable();
        bodyShapesTable.add(new VisTable()).width(20).left();
        bodyShapesTable.add(new BodyShapesWidget(race.getBodyShapes())).expandX().fillX().row();
        editorTable.add(bodyShapesLabel).left().colspan(2).row();
        editorTable.add(bodyShapesTable).colspan(2).left().row();

        editorTable.add(new VisLabel("Colors:")).left().colspan(2).row();
        editorTable.add(new ColorsWidget(race.getColors(), getApplicableColoringLayers(),
                EntityType.CREATURE, basePath, fileChooser, messageDispatcher)).left().colspan(2).row();

        addSelectField("Behaviour:", "behaviourName", creatureBehaviourDictionary.getAllNames(), "", race.getBehaviour(), editorTable);
        addSelectField("Schedule:", "scheduleName", scheduleDictionary.getAllNames(), "", race.getBehaviour(), editorTable);

        editorTable.add(new VisLabel("Needs:")).left();
        VisTable needTable = new VisTable();
        for (EntityNeed need : EntityNeed.values()) {
            VisCheckBox needCheckbox = new VisCheckBox(need.name());
            needCheckbox.setChecked(race.getBehaviour().getNeeds().contains(need));
            needCheckbox.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (needCheckbox.isChecked()) {
                        race.getBehaviour().getNeeds().add(need);
                    } else {
                        race.getBehaviour().getNeeds().remove(need);
                    }
                }
            });
            needTable.add(needCheckbox).left().padLeft(10);
        }
        editorTable.add(needTable).left().row();

        //TODO: widgetize the checkbox group idea
        VisTable groupControlsTable = new VisTable();
        VisCheckBox behaviourGroupCheckbox = new VisCheckBox("Behaviour Group:");
        if (race.getBehaviour().getGroup() != null) {
            behaviourGroupCheckbox.setChecked(true);
            addBehaviourGroupControls(race.getBehaviour().getGroup(), groupControlsTable);
        }
        behaviourGroupCheckbox.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (behaviourGroupCheckbox.isChecked()) {
                    race.getBehaviour().setGroup(new RaceBehaviourGroup());
                    addBehaviourGroupControls(race.getBehaviour().getGroup(), groupControlsTable);
                } else {
                    groupControlsTable.clearChildren();
                    race.getBehaviour().setGroup(null);
                }
            }
        });
        editorTable.add(behaviourGroupCheckbox).left().colspan(2).row();
        editorTable.add(groupControlsTable).left().expandX().colspan(2).row();

        addSelectField("Aggression response:", "aggressionResponse", List.of(AggressionResponse.values()),
                null, race.getBehaviour(), editorTable);

        editorTable.add(new VisLabel("Gender settings:")).left().colspan(2).row();
        editorTable.add(new GenderWidget(race.getGenders())).left().colspan(2).row();


        editorTable.add(new VisLabel("Features:")).left().colspan(2).row();
        editorTable.add(new RaceFeaturesWidget(race.getFeatures(), gameMaterialDictionary, itemTypeDictionary, messageDispatcher)).left().colspan(2).row();
        return editorTable;
    }

    private void addBehaviourGroupControls(RaceBehaviourGroup group, VisTable groupControlsTable) {
        try {
            addIntegerField("Behaviour group min size:", "minSize", group, groupControlsTable);
            addIntegerField("Behaviour group max size:", "maxSize", group, groupControlsTable);
        } catch (ReflectionUtils.PropertyReflectionException e) {
            Logger.error("Error creating widgets", e);
        }
    }

    @Override
    public OkCancelDialog createAssetDialog(ShowCreateAssetDialogMessage message) {
        final Path directory = FileUtils.getDirectory(message.path());
        Path descriptorsFile = directory.resolve("descriptors.json");

        Race race = (Race) message.typeDescriptor();
        Collection<Gender> genders = new HashSet<>(race.getGenders().keySet());
        genders.add(Gender.ANY);
        Collection<CreatureBodyShape> bodyShapes = race.getBodyShapes().stream().map(CreatureBodyShapeDescriptor::getValue).toList();
        Collection<EntityAssetType> assetTypes = entityAssetTypeDictionary.getByEntityType(getEntityType()).stream().filter(assetType -> !assetType.getName().startsWith("ATTACH")).toList();
        List<String> allProfessions = professionDictionary.getAll().stream().map(Profession::getName).collect(Collectors.toList());

        CreatureEntityAsset asset = new CreatureEntityAsset();
        asset.setRace(race);
        asset.setConsciousness(null);
        asset.setConsciousnessList(new ArrayList<>());
        asset.setProfession(NULL_PROFESSION.getName());


        VisValidatableTextField uniqueNameTextBox = new VisValidatableTextField();
        uniqueNameTextBox.addValidator(new UniqueAssetNameValidator(completeAssetDictionary));

        Consumer<Object> uniqueNameRebuilder = o -> {
            String builtName = CreatureNameBuilders.buildUniqueNameForAsset(race, asset);
            uniqueNameTextBox.setText(builtName);
            asset.setUniqueName(builtName);
        };

        OkCancelDialog dialog = new OkCancelDialog("Create asset under " + directory) {
            @Override
            public void onOk() {
                asset.setUniqueName(uniqueNameTextBox.getText());
                completeAssetDictionary.add(asset);
                EntityBrowserValue value = EntityBrowserValue.forAsset(getEntityType(), descriptorsFile, asset, race);
                messageDispatcher.dispatchMessage(MessageType.EDITOR_ASSET_CREATED, value);
                messageDispatcher.dispatchMessage(MessageType.EDITOR_BROWSER_TREE_SELECTION, value);
            }
        };


        dialog.add(WidgetBuilder.selectField("Gender:", asset.getGender(), genders, null, compose(asset::setGender, uniqueNameRebuilder))).left();
        dialog.row();
        dialog.add(WidgetBuilder.selectField("Body Shape:", asset.getBodyShape(), bodyShapes, null, compose(asset::setBodyShape, uniqueNameRebuilder))).left();
        dialog.row();
        dialog.add(WidgetBuilder.selectField("Profession:", asset.getProfession(), allProfessions, NULL_PROFESSION.getName(), compose(asset::setProfession, uniqueNameRebuilder))).left();
        dialog.row();
        dialog.add(WidgetBuilder.selectField("Type", asset.getType(), assetTypes, entityAssetTypeDictionary.getByName("CREATURE_BODY"), compose(asset::setType, uniqueNameRebuilder))).left();
        dialog.row();

        //TODO: this was copied from the editor panel, should remove duplication
        VisTable consciousnessRow = new VisTable();
        VisLabel consciousnessLabel = new VisLabel("Consciousness");
        VisTable consciousnessTable = new VisTable();
        for (Consciousness value : Consciousness.values()) {
            VisCheckBox valueCheckbox = new VisCheckBox(value.name());
            valueCheckbox.setChecked(asset.getConsciousnessList().contains(value));
            valueCheckbox.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (valueCheckbox.isChecked()) {
                        asset.getConsciousnessList().add(value);
                    } else {
                        asset.getConsciousnessList().remove(value);
                    }
                    uniqueNameRebuilder.accept(null);
                }
            });
            consciousnessTable.add(valueCheckbox).left().padLeft(20).row();
        }
        consciousnessRow.add(consciousnessLabel).left().row();
        consciousnessRow.add(consciousnessTable).left();
        dialog.add(consciousnessRow);
        dialog.row();

        VisTable nameRow = new VisTable();
        nameRow.add(new VisLabel("Unique Name:")).left();
        nameRow.add(uniqueNameTextBox).left().expandX().fillX();
        dialog.add(nameRow);
        return dialog;
    }

    public static final String NOT_APPLICABLE = "(any)";

    @Override
    public VisTable getAssetPropertyControls(EntityAsset entityAsset) {
        CreatureEntityAsset creatureAsset = (CreatureEntityAsset) entityAsset;
        VisTable editorTable = new VisTable();
        addTextField("Unique name:", "uniqueName", creatureAsset, editorTable);

        addSelectField("Asset type:", "type", entityAssetTypeDictionary.getByEntityType(EntityType.CREATURE), null, creatureAsset, editorTable);

        addSelectField("Body shape:", "bodyShape", List.of(CreatureBodyShape.values()), CreatureBodyShape.ANY, creatureAsset, editorTable);
        addSelectField("Gender:", "gender", List.of(Gender.values()), Gender.ANY, creatureAsset, editorTable);

        List<String> allProfessions = professionDictionary.getAll().stream().map(Profession::getName).collect(Collectors.toList());
        addSelectField("Profession:", "profession", allProfessions, NULL_PROFESSION.getName(), creatureAsset, editorTable);

        VisSelectBox<String> sanitySelect = new VisSelectBox<>();
        sanitySelect.setItems(orderedArray(
                Arrays.stream(Sanity.values()).map(Sanity::name).collect(Collectors.toList()),
                NOT_APPLICABLE));
        sanitySelect.setSelected(creatureAsset.getSanity() == null ? NOT_APPLICABLE : creatureAsset.getSanity().name());
        sanitySelect.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String selected = sanitySelect.getSelected();
                creatureAsset.setSanity(selected.equals(NOT_APPLICABLE) ? null : Sanity.valueOf(selected));
            }
        });
        editorTable.add(new VisLabel("Sanity:")).left();
        editorTable.add(sanitySelect).left().row();

        if (creatureAsset.getConsciousnessList() == null) {
            creatureAsset.setConsciousnessList(new ArrayList<>());
        }
        if (creatureAsset.getConsciousness() != null && !creatureAsset.getConsciousnessList().contains(creatureAsset.getConsciousness())) {
            creatureAsset.getConsciousnessList().add(creatureAsset.getConsciousness());
            creatureAsset.setConsciousness(null);
        }
        VisLabel consciousnessLabel = new VisLabel("Consciousness (click to show)");
        VisTable consciousnessTable = new VisTable();
        for (Consciousness value : Consciousness.values()) {
            VisCheckBox valueCheckbox = new VisCheckBox(value.name());
            valueCheckbox.setChecked(creatureAsset.getConsciousnessList().contains(value));
            valueCheckbox.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (valueCheckbox.isChecked()) {
                        creatureAsset.getConsciousnessList().add(value);
                    } else {
                        creatureAsset.getConsciousnessList().remove(value);
                    }
                }
            });
            consciousnessTable.add(valueCheckbox).left().padLeft(20).row();
        }
        CollapsibleWidget consciousnessCollapsible = new CollapsibleWidget(consciousnessTable);
        consciousnessCollapsible.setCollapsed(creatureAsset.getConsciousnessList().isEmpty());
        consciousnessLabel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                consciousnessCollapsible.setCollapsed(!consciousnessCollapsible.isCollapsed());
            }
        });
        editorTable.add(consciousnessLabel).left().colspan(2).row();
        editorTable.add(consciousnessCollapsible).left().colspan(2).row();


        VisLabel tagsLabel = new VisLabel("Tags (click to show)");
        TagsWidget tagsWidget = new TagsWidget(creatureAsset.getTags());
        CollapsibleWidget tagsCollapsible = new CollapsibleWidget(tagsWidget);
        tagsCollapsible.setCollapsed(creatureAsset.getTags().isEmpty());
        tagsLabel.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                tagsCollapsible.setCollapsed(!tagsCollapsible.isCollapsed());
            }
        });
        editorTable.add(tagsLabel).left().expandX().fillX().colspan(2).row();
        editorTable.add(tagsCollapsible).expandX().fillX().left().colspan(2).row();
        return editorTable;
    }

}
