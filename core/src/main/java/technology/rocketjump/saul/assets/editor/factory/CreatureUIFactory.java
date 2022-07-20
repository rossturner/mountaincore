package technology.rocketjump.saul.assets.editor.factory;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.widget.VisCheckBox;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisValidatableTextField;
import org.apache.commons.lang3.StringUtils;
import technology.rocketjump.saul.assets.editor.model.EditorEntitySelection;
import technology.rocketjump.saul.assets.editor.widgets.OkCancelDialog;
import technology.rocketjump.saul.assets.editor.widgets.entitybrowser.EntityBrowserValue;
import technology.rocketjump.saul.assets.editor.widgets.entitybrowser.ShowCreateAssetDialogMessage;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.assets.entities.CompleteAssetDictionary;
import technology.rocketjump.saul.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShape;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShapeDescriptor;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.saul.assets.entities.model.EntityAssetType;
import technology.rocketjump.saul.entities.factories.CreatureEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.creature.*;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.persistence.FileUtils;

import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;

@Singleton
public class CreatureUIFactory implements UIFactory {
    private final MessageDispatcher messageDispatcher;
    private final EntityAssetTypeDictionary entityAssetTypeDictionary;
    private final CompleteAssetDictionary completeAssetDictionary;
    private final RaceDictionary raceDictionary;
    private final CreatureEntityFactory creatureEntityFactory;

    @Inject
    public CreatureUIFactory(MessageDispatcher messageDispatcher, EntityAssetTypeDictionary entityAssetTypeDictionary, CompleteAssetDictionary completeAssetDictionary, RaceDictionary raceDictionary, CreatureEntityFactory creatureEntityFactory) {
        this.messageDispatcher = messageDispatcher;
        this.entityAssetTypeDictionary = entityAssetTypeDictionary;
        this.completeAssetDictionary = completeAssetDictionary;
        this.raceDictionary = raceDictionary;
        this.creatureEntityFactory = creatureEntityFactory;
    }

    @Override
    public EntityType getEntityType() {
        return EntityType.CREATURE;
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
    public OkCancelDialog createEntityDialog(Path path) {
        Function<String, Boolean> validRace = new Function<>() {
            @Override
            public Boolean apply(String input) {
                //TODO: define rules
                if (StringUtils.length(input) < 2) {
                    return false;
                }
                Race existing = raceDictionary.getByName(input);
                return existing == null;
            }
        };

        VisValidatableTextField raceNameTextField = new VisValidatableTextField();
        raceNameTextField.addValidator(validRace::apply);

        OkCancelDialog dialog = new OkCancelDialog("Create new " + getEntityType()) {
            @Override
            public void onOk() {
                String folderName = raceNameTextField.getText().toLowerCase(Locale.ROOT);
                Path basePath = FileUtils.createDirectory(path, folderName);
                //TODO: maybe a nice function to setup required defaults
                String raceName = raceNameTextField.getText();
                CreatureBodyShapeDescriptor bodyShape = new CreatureBodyShapeDescriptor();
                bodyShape.setValue(CreatureBodyShape.AVERAGE);
                Race newRace = new Race();
                newRace.setName(raceName);
                newRace.setI18nKey("RACE." + raceName.toUpperCase());
                newRace.setBodyStructureName("pawed-quadruped"); //TODO: Present user with options?
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
    public OkCancelDialog createAssetDialog(ShowCreateAssetDialogMessage message) {
        final Path directory = FileUtils.getDirectory(message.path());
        Path descriptorsFile = directory.resolve("descriptors.json");
        //TODO: consider Widget builder for validating text fields
        VisValidatableTextField uniqueNameTextBox = new VisValidatableTextField();
        uniqueNameTextBox.addValidator(StringUtils::isNotBlank);
        uniqueNameTextBox.addValidator(input -> completeAssetDictionary.getByUniqueName(input) == null);

        Race race = (Race) message.typeDescriptor();
        Collection<Gender> genders = new HashSet<>(race.getGenders().keySet());
        genders.add(Gender.ANY);
        Collection<CreatureBodyShape> bodyShapes = race.getBodyShapes().stream().map(CreatureBodyShapeDescriptor::getValue).toList();

        CreatureEntityAsset asset = new CreatureEntityAsset();
        asset.setRace(race);
        asset.setConsciousness(null);
        asset.setConsciousnessList(new ArrayList<>());

        OkCancelDialog dialog = new OkCancelDialog("Create asset under " + directory) {
            @Override
            public void onOk() {
                asset.setUniqueName(uniqueNameTextBox.getText());
                completeAssetDictionary.add(asset);
                messageDispatcher.dispatchMessage(MessageType.EDITOR_BROWSER_TREE_SELECTION, EntityBrowserValue.forAsset(getEntityType(), descriptorsFile, asset, race));
            }
        };


        dialog.add(WidgetBuilder.selectField("Gender:", asset.getGender(), genders, null, asset::setGender)).left();
        dialog.row();
        dialog.add(WidgetBuilder.selectField("Body Shape:", asset.getBodyShape(), bodyShapes, null, asset::setBodyShape)).left();
        dialog.row();
        //todo:profession
        Collection<EntityAssetType> assetTypes = entityAssetTypeDictionary.getByEntityType(getEntityType()).stream().filter(assetType -> !assetType.getName().startsWith("ATTACH")).toList();

        dialog.add(WidgetBuilder.selectField("Type", asset.getType(), assetTypes,
                entityAssetTypeDictionary.getByName("CREATURE_BODY"), asset::setType)).left();
        dialog.row();

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
                }
            });
            consciousnessTable.add(valueCheckbox).left().padLeft(20).row();
        }
        consciousnessRow.add(consciousnessLabel).left().row();
        consciousnessRow.add(consciousnessTable).left();
        dialog.add(consciousnessRow);
        dialog.row();

        //TODO: Autogenerate name
        //Race-[Gender]-[BodyShape]-[Profession]-Type-Consciousness
        VisTable nameRow = new VisTable();
        nameRow.add(new VisLabel("Unique Name:")).left();
        nameRow.add(uniqueNameTextBox).left();
        dialog.add(nameRow);
        return dialog;
    }

}
