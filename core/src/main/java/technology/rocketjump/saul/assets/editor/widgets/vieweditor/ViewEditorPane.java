package technology.rocketjump.saul.assets.editor.widgets.vieweditor;

import com.badlogic.gdx.ai.msg.MessageDispatcher;
import com.badlogic.gdx.ai.msg.Telegram;
import com.badlogic.gdx.ai.msg.Telegraph;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisRadioButton;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.kotcrab.vis.ui.widget.VisTable;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.saul.assets.entities.creature.CreatureEntityAssetDictionary;
import technology.rocketjump.saul.entities.EntityAssetUpdater;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.physical.EntityAttributes;
import technology.rocketjump.saul.entities.model.physical.creature.CreatureEntityAttributes;
import technology.rocketjump.saul.jobs.ProfessionDictionary;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.RenderMode;

import javax.inject.Inject;

public class ViewEditorPane extends VisTable implements Telegraph {

    private final EditorStateProvider editorStateProvider;
    private final EntityAssetUpdater entityAssetUpdater;
    private final ProfessionDictionary professionDictionary;
    private final EntityAssetTypeDictionary entityAssetTypeDictionary;
    private final CreatureEntityAssetDictionary creatureEntityAssetDictionary;
    private final MessageDispatcher messageDispatcher;

    @Inject
    public ViewEditorPane(EditorStateProvider editorStateProvider, EntityAssetUpdater entityAssetUpdater, ProfessionDictionary professionDictionary, EntityAssetTypeDictionary entityAssetTypeDictionary, CreatureEntityAssetDictionary creatureEntityAssetDictionary, MessageDispatcher messageDispatcher) {
        super();
        this.editorStateProvider = editorStateProvider;
        this.entityAssetUpdater = entityAssetUpdater;
        this.professionDictionary = professionDictionary;
        this.entityAssetTypeDictionary = entityAssetTypeDictionary;
        this.creatureEntityAssetDictionary = creatureEntityAssetDictionary;
        this.messageDispatcher = messageDispatcher;
        messageDispatcher.addListener(this, MessageType.EDITOR_BROWSER_TREE_SELECTION);
    }


    public void reload() {
        this.clearChildren();
        setBackground("window-bg");

        add(new VisLabel("View Editor")).expandX().left().row();
        add(buildRenderModeRow()).left();
        add(buildSpritePaddingRow()).left();
        row();

        Entity currentEntity = editorStateProvider.getState().getCurrentEntity();
        if (currentEntity != null) {

            EntityAttributes entityAttributes = currentEntity.getPhysicalEntityComponent().getAttributes();
            VisTable entityAttributesPane = null;
            VisTable entityAssetPane = null;
            //TODO: Move to UI Factory
            if (entityAttributes instanceof CreatureEntityAttributes creatureAttributes) {
                entityAttributesPane = new CreatureAttributesPane(creatureAttributes, editorStateProvider, entityAssetUpdater, professionDictionary, messageDispatcher);
                entityAssetPane = new CreatureAssetPane(creatureAttributes, entityAssetTypeDictionary, creatureEntityAssetDictionary, editorStateProvider);
            }

            if (entityAttributesPane != null) {
                add(entityAttributesPane).left().row();
                add(entityAssetPane).left().row();
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

    private VisTable buildSpritePaddingRow() {
        VisTable spritePaddingRow = new VisTable();
        spritePaddingRow.add(new VisLabel("Sprite padding")).left();
        VisSlider slider = new VisSlider(1, 3, 1, false);
        slider.setValue(editorStateProvider.getState().getSpritePadding());
        slider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                editorStateProvider.getState().setSpritePadding((int) slider.getValue());
            }
        });
        spritePaddingRow.add(slider).left();
        return spritePaddingRow;
    }

    @Override
    public boolean handleMessage(Telegram msg) {

        //TODO: Subscribe to this to update the asset type drop downs
//            case MessageType.EDITOR_BROWSER_TREE_SELECTION: {
//                EntityBrowserValue value = (EntityBrowserValue) msg.extraInfo;
        //EntityAsset comes from EDITOR_BROWSER_TREE_SELECTION
        return true;
    }
}
