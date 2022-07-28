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
import technology.rocketjump.saul.assets.editor.factory.UIFactory;
import technology.rocketjump.saul.assets.editor.model.EditorStateProvider;
import technology.rocketjump.saul.assets.entities.EntityAssetTypeDictionary;
import technology.rocketjump.saul.assets.entities.creature.CreatureEntityAssetDictionary;
import technology.rocketjump.saul.entities.EntityAssetUpdater;
import technology.rocketjump.saul.entities.factories.ItemEntityFactory;
import technology.rocketjump.saul.entities.model.Entity;
import technology.rocketjump.saul.entities.model.EntityType;
import technology.rocketjump.saul.entities.model.physical.item.ItemTypeDictionary;
import technology.rocketjump.saul.jobs.ProfessionDictionary;
import technology.rocketjump.saul.messaging.MessageType;
import technology.rocketjump.saul.rendering.RenderMode;

import javax.inject.Inject;
import java.util.Map;

public class ViewEditorPane extends VisTable implements Telegraph {

    private final EditorStateProvider editorStateProvider;
    private final EntityAssetUpdater entityAssetUpdater;
    private final ProfessionDictionary professionDictionary;
    private final EntityAssetTypeDictionary entityAssetTypeDictionary;
    private final CreatureEntityAssetDictionary creatureEntityAssetDictionary;
    private final MessageDispatcher messageDispatcher;
    private final ItemTypeDictionary itemTypeDictionary;
    private final ItemEntityFactory itemEntityFactory;
    private final Map<EntityType, UIFactory> uiFactories;

    @Inject
    public ViewEditorPane(EditorStateProvider editorStateProvider, EntityAssetUpdater entityAssetUpdater,
                          ProfessionDictionary professionDictionary, EntityAssetTypeDictionary entityAssetTypeDictionary,
                          CreatureEntityAssetDictionary creatureEntityAssetDictionary, MessageDispatcher messageDispatcher,
                          ItemTypeDictionary itemTypeDictionary, ItemEntityFactory itemEntityFactory, Map<EntityType, UIFactory> uiFactories) {
        super();
        this.editorStateProvider = editorStateProvider;
        this.entityAssetUpdater = entityAssetUpdater;
        this.professionDictionary = professionDictionary;
        this.entityAssetTypeDictionary = entityAssetTypeDictionary;
        this.creatureEntityAssetDictionary = creatureEntityAssetDictionary;
        this.messageDispatcher = messageDispatcher;
        this.itemTypeDictionary = itemTypeDictionary;
        this.itemEntityFactory = itemEntityFactory;
        this.uiFactories = uiFactories;
        messageDispatcher.addListener(this, MessageType.EDITOR_BROWSER_TREE_SELECTION);
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
