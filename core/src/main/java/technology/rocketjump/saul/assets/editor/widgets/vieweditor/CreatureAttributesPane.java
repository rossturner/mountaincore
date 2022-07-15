package technology.rocketjump.saul.assets.editor.widgets.vieweditor;

import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTable;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShape;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShapeDescriptor;
import technology.rocketjump.saul.entities.EntityAssetUpdater;
import technology.rocketjump.saul.entities.model.physical.creature.Consciousness;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.Gender;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class CreatureAttributesPane extends VisTable {

    private final EditorStateProvider editorStateProvider;
    private final EntityAssetUpdater entityAssetUpdater;

    public CreatureAttributesPane(CreatureEntityAttributes creatureAttributes, EditorStateProvider editorStateProvider, EntityAssetUpdater entityAssetUpdater) {
        super();
        this.editorStateProvider = editorStateProvider;
        this.entityAssetUpdater = entityAssetUpdater;

        Set<Gender> genders = creatureAttributes.getRace().getGenders().keySet();
        List<CreatureBodyShape> bodyShapes = creatureAttributes.getRace().getBodyShapes().stream().map(CreatureBodyShapeDescriptor::getValue).toList();
        Set<Consciousness> consciousnesses = new HashSet<>(); //TODO: Enum to List function
        for (Consciousness value : Consciousness.values()) {
            consciousnesses.add(value);
        }

        add(WidgetBuilder.selectField("Gender:", creatureAttributes.getGender(), genders, null, update(creatureAttributes::setGender)));
        add(WidgetBuilder.selectField("Body Shape:", creatureAttributes.getBodyShape(), bodyShapes, null, update(creatureAttributes::setBodyShape)));
        add(WidgetBuilder.selectField("Consciousness:", creatureAttributes.getConsciousness(), consciousnesses, null, update(creatureAttributes::setConsciousness)));
        add(new VisLabel("Profession:"));
        add(new VisSelectBox<>());
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
