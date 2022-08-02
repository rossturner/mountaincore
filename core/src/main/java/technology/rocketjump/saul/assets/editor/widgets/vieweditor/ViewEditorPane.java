package technology.rocketjump.saul.assets.editor.widgets.vieweditor;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisRadioButton;
import com.kotcrab.vis.ui.widget.VisTable;
import technology.rocketjump.saul.assets.editor.factory.UIFactory;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.rendering.RenderMode;

import javax.inject.Inject;
import java.util.Map;

public class ViewEditorPane extends VisTable {

    private final EditorStateProvider editorStateProvider;
    private final Map<EntityType, UIFactory> uiFactories;

    @Inject
    public ViewEditorPane(EditorStateProvider editorStateProvider, Map<EntityType, UIFactory> uiFactories) {
        super();
        this.editorStateProvider = editorStateProvider;
        this.uiFactories = uiFactories;
    }


    public void reload() {
        this.clearChildren();
        setBackground("window-bg");

        add(new VisLabel("View Editor")).expandX().colspan(2).row();
        add(buildRenderModeWidget());
        add(buildSpritePaddingWidget());
        row();

        Entity currentEntity = editorStateProvider.getState().getCurrentEntity();
        if (currentEntity != null) {
            VisTable entityAttributesPane = uiFactories.get(currentEntity.getType()).getViewEditorControls();

            if (entityAttributesPane != null) {
                add(entityAttributesPane).left().fill().colspan(2).row();
            }
        }

    }


    private VisTable buildRenderModeWidget() {
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

    private VisTable buildSpritePaddingWidget() {
        return WidgetBuilder.slider("Sprite padding", editorStateProvider.getState().getSpritePadding(), 1, 3, 1, value -> editorStateProvider.getState().setSpritePadding(value));
    }
}
