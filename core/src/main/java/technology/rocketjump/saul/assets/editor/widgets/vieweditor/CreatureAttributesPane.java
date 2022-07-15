package technology.rocketjump.saul.assets.editor.widgets.vieweditor;

import com.kotcrab.vis.ui.widget.VisTable;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShape;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShapeDescriptor;
import technology.rocketjump.saul.entities.EntityAssetUpdater;
import technology.rocketjump.saul.entities.components.humanoid.ProfessionsComponent;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.Consciousness;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.Gender;
import technology.rocketjump.saul.jobs.ProfessionDictionary;
import technology.rocketjump.saul.jobs.model.Profession;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Consumer;

public class CreatureAttributesPane extends VisTable {

    private final EditorStateProvider editorStateProvider;
    private final EntityAssetUpdater entityAssetUpdater;
    private final ProfessionDictionary professionDictionary;

    public CreatureAttributesPane(CreatureEntityAttributes creatureAttributes, EditorStateProvider editorStateProvider, EntityAssetUpdater entityAssetUpdater, ProfessionDictionary professionDictionary) {
        super();
        this.editorStateProvider = editorStateProvider;
        this.entityAssetUpdater = entityAssetUpdater;
        this.professionDictionary = professionDictionary;

        //TODO: Move to reload pattern
        Entity currentEntity = editorStateProvider.getState().getCurrentEntity();
        Collection<Gender> genders = creatureAttributes.getRace().getGenders().keySet();
        Collection<CreatureBodyShape> bodyShapes = creatureAttributes.getRace().getBodyShapes().stream().map(CreatureBodyShapeDescriptor::getValue).toList();
        Collection<Consciousness> consciousnesses = Arrays.asList(Consciousness.values());
        Collection<Profession> professions = this.professionDictionary.getAll();

        ProfessionsComponent professionsComponent = currentEntity.getOrCreateComponent(ProfessionsComponent.class);


        add(WidgetBuilder.selectField("Gender:", creatureAttributes.getGender(), genders, null, update(creatureAttributes::setGender)));
        add(WidgetBuilder.selectField("Body Shape:", creatureAttributes.getBodyShape(), bodyShapes, null, update(creatureAttributes::setBodyShape)));
        add(WidgetBuilder.selectField("Consciousness:", creatureAttributes.getConsciousness(), consciousnesses, null, update(creatureAttributes::setConsciousness)));
        add(WidgetBuilder.selectField("Profession:", ProfessionDictionary.NULL_PROFESSION, professions, null, update(profession -> {
            professionsComponent.clear();
            professionsComponent.setSkillLevel(profession, 50);
        })));
        //		HorizontalFlowGroup assetTypeFlowGroup = new HorizontalFlowGroup(5); //TODO: not 100% is right separate flow groups
        //		assetTypeFlowGroup.addActor(new VisLabel("Hair:"));
        //		assetTypeFlowGroup.addActor(new VisSelectBox<>());
        //		assetTypeFlowGroup.addActor(new VisLabel("Eyebrows:"));
        //		assetTypeFlowGroup.addActor(new VisSelectBox<>());
        //		assetTypeFlowGroup.addActor(new VisLabel("Beard:"));
        //		assetTypeFlowGroup.addActor(new VisSelectBox<>());
        //		assetTypeFlowGroup.addActor(new VisLabel("Clothing:"));
        //		assetTypeFlowGroup.addActor(new VisSelectBox<>());
        //		viewEditor.add(assetTypeFlowGroup).expandX().fillX();
    }

    private <T> Consumer<T> update(Consumer<T> input) {
        Consumer<T> consumer = x -> {
            entityAssetUpdater.updateEntityAssets(editorStateProvider.getState().getCurrentEntity());
            editorStateProvider.stateChanged();
        };

        return input.andThen(consumer);
    }
}
