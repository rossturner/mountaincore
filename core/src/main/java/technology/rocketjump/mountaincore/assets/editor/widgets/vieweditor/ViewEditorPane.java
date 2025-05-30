package technology.rocketjump.mountaincore.assets.editor.widgets.vieweditor;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.kotcrab.vis.ui.widget.*;
import technology.rocketjump.mountaincore.assets.editor.factory.UIFactory;
import technology.rocketjump.mountaincore.assets.editor.model.EditorStateProvider;
import technology.rocketjump.mountaincore.assets.editor.widgets.propertyeditor.WidgetBuilder;
import technology.rocketjump.mountaincore.entities.components.AnimationComponent;
import technology.rocketjump.mountaincore.entities.model.Entity;
import technology.rocketjump.mountaincore.entities.model.EntityType;
import technology.rocketjump.mountaincore.entities.model.physical.creature.EquippedItemComponent;
import technology.rocketjump.mountaincore.messaging.MessageType;
import technology.rocketjump.mountaincore.persistence.UserPreferences;
import technology.rocketjump.mountaincore.rendering.RenderMode;
import technology.rocketjump.mountaincore.rendering.entities.AnimationStudio;

import javax.inject.Inject;
import java.util.Map;

public class ViewEditorPane extends VisTable {

    public static final String CLOSED_LABEL = "[>] View Editor";
    public static final String OPEN_LABEL = "[v] View Editor";
    private final EditorStateProvider editorStateProvider;
    private final Map<EntityType, UIFactory> uiFactories;
    private final AnimationStudio animationStudio;
    private final MessageDispatcher messageDispatcher;
    private boolean isCollapsed = true;

    @Inject
    public ViewEditorPane(EditorStateProvider editorStateProvider, Map<EntityType, UIFactory> uiFactories,
                          AnimationStudio animationStudio, MessageDispatcher messageDispatcher) {
        super();
        this.editorStateProvider = editorStateProvider;
        this.uiFactories = uiFactories;
        this.animationStudio = animationStudio;
        this.messageDispatcher = messageDispatcher;
        messageDispatcher.dispatchMessage(MessageType.GUI_CHANGE_SOUND_EFFECT_VOLUME, 0.0);
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

            viewEditorControls.row();
            viewEditorControls.add(animationPlaybackWidget(currentEntity)).colspan(2).left();
        }
    }

    private VisTable animationPlaybackWidget(Entity currentEntity) {
        VisTable table = new VisTable();
        AnimationComponent animationComponent = currentEntity.getOrCreateComponent(AnimationComponent.class);

        table.add(WidgetBuilder.label("Animation Controls")).padRight(14);
        table.add(WidgetBuilder.checkBox("Mute Sound", true,
                checked -> messageDispatcher.dispatchMessage(MessageType.GUI_CHANGE_SOUND_EFFECT_VOLUME, 0.0f),
                unchecked -> messageDispatcher.dispatchMessage(MessageType.GUI_CHANGE_SOUND_EFFECT_VOLUME, Float.parseFloat(UserPreferences.PreferenceKey.SOUND_EFFECT_VOLUME.defaultValue)))).padRight(12);

        table.add(WidgetBuilder.select(animationComponent.getCurrentAnimation(), animationStudio.getAvailableAnimationNames(), null, a -> {
            animationComponent.setCurrentAnimation(a);
            EquippedItemComponent equippedItemComponent = currentEntity.getComponent(EquippedItemComponent.class);
            if (equippedItemComponent != null && equippedItemComponent.getMainHandItem() != null) {
                equippedItemComponent.getMainHandItem().getOrCreateComponent(AnimationComponent.class).setCurrentAnimation(a);
            }
        })).padRight(10);

        VisLabel durationLabel = new VisLabel("");
        Table playControls = new Table();

        VisTextButton playPause = WidgetBuilder.button("> ||", textButton -> {
            if (textButton.isChecked()) {
                animationStudio.resumeAnimations();
                durationLabel.setText("Playing");
            } else {
                animationStudio.pauseAnimations();
                durationLabel.setText("Paused");
            }
        });
        VisTextButton jumpToStart = WidgetBuilder.button("|<", textButton -> {
            float keyFrameTime = animationStudio.jumpToStartForAnimations();
            playPause.setChecked(true);
            durationLabel.setText("Duration " + keyFrameTime);
        });
        VisTextButton previousKeyFrame = WidgetBuilder.button("<", textButton -> {
            float keyFrameTime = animationStudio.previousKeyFrame();
            playPause.setChecked(true);
            durationLabel.setText("Duration " + keyFrameTime);
        });

        VisTextButton nextKeyFrame = WidgetBuilder.button(">", textButton -> {
            float keyFrameTime = animationStudio.nextKeyFrame();
            playPause.setChecked(true);
            durationLabel.setText("Duration " + keyFrameTime);
        });

        VisTextButton jumpToEnd = WidgetBuilder.button(">|", textButton -> {
            float keyFrameTime = animationStudio.jumpToEndForAnimations();
            playPause.setChecked(true);
            durationLabel.setText("Duration " + keyFrameTime);
        });

        playPause.setChecked(false);

        new Tooltip.Builder("Jump to start").target(jumpToStart).build();
        new Tooltip.Builder("Previous Key Frame").target(previousKeyFrame).build();
        new Tooltip.Builder("Play/Pause the animation").target(playPause).build();
        new Tooltip.Builder("Next Key Frame").target(nextKeyFrame).build();
        new Tooltip.Builder("Jump to end").target(jumpToEnd).build();

        playControls.add(jumpToStart).uniform().spaceRight(5);
        playControls.add(previousKeyFrame).uniform().spaceRight(5);
        playControls.add(playPause).spaceRight(5);
        playControls.add(nextKeyFrame).uniform().spaceRight(5);
        playControls.add(jumpToEnd).uniform().spaceRight(5);

        table.add(playControls);
        table.add(durationLabel).padLeft(10);
        return table;
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
