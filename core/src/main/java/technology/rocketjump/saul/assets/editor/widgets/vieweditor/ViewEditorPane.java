package technology.rocketjump.saul.assets.editor.widgets.vieweditor;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.CollapsibleWidget;
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

    public static final String CLOSED_LABEL = "[>] View Editor";
    public static final String OPEN_LABEL = "[v] View Editor";
    private final EditorStateProvider editorStateProvider;
    private final Map<EntityType, UIFactory> uiFactories;
    private boolean isCollapsed = true;

    @Inject
    public ViewEditorPane(EditorStateProvider editorStateProvider, Map<EntityType, UIFactory> uiFactories) {
        super();
        this.editorStateProvider = editorStateProvider;
        this.uiFactories = uiFactories;
    }


    public void reload() {
        this.clearChildren();
        setBackground("window-bg");

        VisTable viewEditorControls = new VisTable();
        CollapsibleWidget collapsibleWidget = new CollapsibleWidget(viewEditorControls);
        collapsibleWidget.setCollapsed(isCollapsed);

        VisLabel viewEditorLabel = new VisLabel(isCollapsed ? CLOSED_LABEL : OPEN_LABEL);
        viewEditorLabel.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                isCollapsed = !isCollapsed;
                viewEditorLabel.setText(isCollapsed ? CLOSED_LABEL : OPEN_LABEL);
                collapsibleWidget.setCollapsed(isCollapsed);
                return true;
            }
        });
        add(viewEditorLabel).row();
        add(collapsibleWidget).row();

        viewEditorControls.add(buildRenderModeWidget());
        viewEditorControls.add(buildSpritePaddingWidget());
        viewEditorControls.row();

        Entity currentEntity = editorStateProvider.getState().getCurrentEntity();
        if (currentEntity != null) {
            VisTable entityAttributesPane = uiFactories.get(currentEntity.getType()).getViewEditorControls();

            if (entityAttributesPane != null) {
                viewEditorControls.add(entityAttributesPane).colspan(2).row();
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
        return WidgetBuilder.slider("Sprite padding", editorStateProvider.getState().getSpritePadding(), 1, 4, 1, value -> editorStateProvider.getState().setSpritePadding(value));
    }
}
