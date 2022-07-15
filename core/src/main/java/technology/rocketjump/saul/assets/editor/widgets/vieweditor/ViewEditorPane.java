package technology.rocketjump.saul.assets.editor.widgets.vieweditor;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisRadioButton;
import com.kotcrab.vis.ui.widget.VisTable;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.entities.EntityAssetUpdater;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.EntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.rendering.RenderMode;

import javax.inject.Inject;

public class ViewEditorPane extends VisTable {

    private final EditorStateProvider editorStateProvider;
    private final EntityAssetUpdater entityAssetUpdater;

    @Inject
    public ViewEditorPane(EditorStateProvider editorStateProvider, EntityAssetUpdater entityAssetUpdater) {
        super();
        this.editorStateProvider = editorStateProvider;
        this.entityAssetUpdater = entityAssetUpdater;
    }

    public void reload() {
        this.clearChildren();
        debug();
        setBackground("window-bg");

        add(new VisLabel("View Editor")).expandX().left().row();
        add(buildRenderModeRow()).left().row();

        Entity currentEntity = editorStateProvider.getState().getCurrentEntity();
        if (currentEntity != null) {
            EntityAttributes entityAttributes = currentEntity.getPhysicalEntityComponent().getAttributes();
            VisTable entityAttributesPane = null;
            if (entityAttributes instanceof CreatureEntityAttributes creatureAttributes) {
                entityAttributesPane = new CreatureAttributesPane(creatureAttributes, editorStateProvider, entityAssetUpdater);
            }
            if (entityAttributesPane != null) {
                add(entityAttributesPane).left().row();
            }
        }
    }

    private VisTable buildRenderModeRow() {
        VisTable renderModeRow = new VisTable();
        //TODO: Consider moving to WidgetBuilder to generate a radio button group
        renderModeRow.add(new VisLabel("Render Mode"));
        ButtonGroup<VisRadioButton> renderModeButtonGroup = new ButtonGroup<>();
        for (RenderMode renderMode : RenderMode.values()) {
            VisRadioButton radioButton = new VisRadioButton(renderMode.name());
            renderModeButtonGroup.add(radioButton);
            radioButton.setChecked(renderMode == editorStateProvider.getState().getRenderMode());
            radioButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (radioButton.isChecked()) {
                        editorStateProvider.getState().setRenderMode(renderMode);
                        editorStateProvider.stateChanged();
                    }
                }
            });
            renderModeRow.add(radioButton);
        }
        return renderModeRow;
    }
}
