package technology.rocketjump.saul.assets.editor.widgets.vieweditor;

import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSelectBox;
import com.kotcrab.vis.ui.widget.VisTable;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShape;
import technology.rocketjump.saul.assets.entities.creature.model.CreatureBodyShapeDescriptor;
import technology.rocketjump.saul.entities.EntityAssetUpdater;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;

import java.util.List;

public class CreatureAttributesPane extends VisTable {

    public CreatureAttributesPane(CreatureEntityAttributes creatureAttributes, EditorStateProvider editorStateProvider, EntityAssetUpdater entityAssetUpdater) {
        super();
        Entity currentEntity = editorStateProvider.getState().getCurrentEntity();
        add(
                WidgetBuilder.selectField("Gender:", creatureAttributes.getGender(), creatureAttributes.getRace().getGenders().keySet(), null, gender -> {
                    creatureAttributes.setGender(gender);
                    entityAssetUpdater.updateEntityAssets(currentEntity);
                    editorStateProvider.stateChanged();
                })
        );
        List<CreatureBodyShape> bodyShapes = creatureAttributes.getRace().getBodyShapes().stream().map(CreatureBodyShapeDescriptor::getValue).toList();
        add(
                WidgetBuilder.selectField("Body Shape:", creatureAttributes.getBodyShape(), bodyShapes, null, bodyShape -> {
                    creatureAttributes.setBodyShape(bodyShape);
                    entityAssetUpdater.updateEntityAssets(currentEntity);
                    editorStateProvider.stateChanged();
                })
        );
        add(new VisLabel("Consciousness:"));
        add(new VisSelectBox<>());
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
}
