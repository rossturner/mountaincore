package technology.rocketjump.saul.assets.editor.factory;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.math.RandomXS128;
import com.badlogic.gdx.math.Vector2;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisValidatableTextField;
import org.apache.commons.lang3.StringUtils;
import technology.rocketjump.saul.assets.editor.model.EditorEntitySelection;
import technology.rocketjump.saul.assets.editor.widgets.OkCancelDialog;
import technology.rocketjump.saul.assets.editor.widgets.entitybrowser.EntityBrowserValue;
import technology.rocketjump.saul.assets.editor.widgets.entitybrowser.ShowCreateAssetDialogMessage;
import technology.rocketjump.saul.assets.entities.CompleteAssetDictionary;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShape;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShapeDescriptor;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureEntityAsset;
import technology.rocketjump.saul.entities.factories.CreatureEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.Race;
import technology.rocketjump.saul.entities.model.physical.creature.RaceDictionary;
import technology.rocketjump.saul.gamecontext.GameContext;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.persistence.FileUtils;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.function.Function;

@Singleton
public class CreatureUIFactory implements UIFactory {
    private final MessageDispatcher messageDispatcher;
    private final CompleteAssetDictionary completeAssetDictionary;
    private final RaceDictionary raceDictionary;
    private final CreatureEntityFactory creatureEntityFactory;

    @Inject
    public CreatureUIFactory(MessageDispatcher messageDispatcher, CompleteAssetDictionary completeAssetDictionary, RaceDictionary raceDictionary, CreatureEntityFactory creatureEntityFactory) {
        this.messageDispatcher = messageDispatcher;
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
        VisValidatableTextField assetNameTextBox = new VisValidatableTextField();
        assetNameTextBox.addValidator(StringUtils::isNotBlank);

        OkCancelDialog dialog = new OkCancelDialog("Create asset under " + message.path()) {
            @Override
            public void onOk() {
                //TODO: add to complete asset dictionary and rebuild or just to creatureEntityAssetDictionary and hit rebuildComplete
                CreatureEntityAsset asset = new CreatureEntityAsset();
//						asset.set

            }
        };
        dialog.add(new VisLabel("Asset Name"));
        dialog.add(assetNameTextBox);
        return dialog;

    }
}
